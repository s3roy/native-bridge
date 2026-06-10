package com.bridge

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference

/**
 * Auto-tracks full device/app context and pushes realtime updates to every
 * attached WebView. Started automatically by [BridgeInitProvider].
 */
object AppStateMonitor {

    @Volatile private var started = false
    @Volatile private var lifecycle = "active"
    @Volatile private var isForeground = true
    @Volatile private var isInPiP = false
    @Volatile private var network = NetworkState(true, "unknown")
    @Volatile private var orientation = "unknown"
    @Volatile private var webViewVisible = true
    @Volatile private var webViewFocused = false
    @Volatile private var webViewUrl: String? = null
    @Volatile private var keyboard = KeyboardState(false, 0)
    @Volatile private var safeArea = SafeAreaInsets(0, 0, 0, 0)
    @Volatile private var battery = BatteryState(1f, false, false)
    @Volatile private var audio = AudioRouteState("unknown", false, null)
    @Volatile private var display = DisplayState(0, 0, 1f, 1f)
    @Volatile private var theme = ThemeState(false)
    @Volatile private var locale = LocaleState("en", "", TimeZone.getDefault().id)
    @Volatile private var call = CallState(false, "idle")

    private var app: Application? = null
    private var connectivityManager: ConnectivityManager? = null
    private var audioManager: AudioManager? = null
    private var telephonyManager: TelephonyManager? = null
    private val resumedActivity = AtomicReference<Activity?>(null)
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshNetwork()
        override fun onLost(network: Network) = refreshNetwork()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refreshNetwork()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) = refreshBattery(ctx)
    }

    private val audioDeviceCallback = object : AudioManager.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshAudio()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshAudio()
    }

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            val next = when (state) {
                TelephonyManager.CALL_STATE_RINGING -> CallState(true, "ringing")
                TelephonyManager.CALL_STATE_OFFHOOK -> CallState(true, "active")
                else -> CallState(false, "idle")
            }
            if (next == call) return
            call = next
            publish("call")
        }
    }

    fun start(application: Application) {
        if (started) return
        started = true
        app = application
        connectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telephonyManager =
            application.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        orientation = readOrientation(application.resources.configuration)
        display = readDisplay(application)
        theme = readTheme(application.resources.configuration)
        locale = readLocale()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isForeground = true
                lifecycle = "active"
                publish("lifecycle")
            }

            override fun onStop(owner: LifecycleOwner) {
                isForeground = false
                lifecycle = "background"
                publish("lifecycle")
            }
        })

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, s: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}

            override fun onActivityResumed(a: Activity) {
                resumedActivity.set(a)
                lifecycle = "active"
                attachKeyboardListener(a)
                refreshInsetsFromActivity(a)
                publish("lifecycle")
            }

            override fun onActivityPaused(a: Activity) {
                lifecycle = "inactive"
                detachKeyboardListener(a)
                publish("lifecycle")
            }

            override fun onActivityStopped(a: Activity) {
                if (resumedActivity.get() === a) resumedActivity.set(null)
            }

            override fun onActivitySaveInstanceState(a: Activity, out: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {
                if (resumedActivity.get() === a) resumedActivity.set(null)
            }

            override fun onActivityPictureInPictureModeChanged(
                activity: Activity,
                isInPictureInPictureMode: Boolean,
            ) {
                isInPiP = isInPictureInPictureMode
                publish("pip")
            }
        })

        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                val nextO = readOrientation(newConfig)
                if (nextO != orientation) {
                    orientation = nextO
                    publish("orientation")
                }
                val nextT = readTheme(newConfig)
                if (nextT != theme) {
                    theme = nextT
                    publish("theme")
                }
                val nextD = readDisplay(application)
                if (nextD != display) {
                    display = nextD
                    publish("display")
                }
            }

            override fun onLowMemory() {
                NativeBridge.publishEvent("app.memory", org.json.JSONObject().put("lowMemory", true))
            }
        })

        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            networkCallback,
        )

        application.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )

        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
        refreshBattery(application)
        refreshAudio()
        refreshNetwork()
        listenCallState()

        publish("init")
    }

    internal fun setWebViewVisible(visible: Boolean) {
        if (webViewVisible == visible) return
        webViewVisible = visible
        publish("webview")
    }

    internal fun setWebViewFocused(focused: Boolean) {
        if (webViewFocused == focused) return
        webViewFocused = focused
        publish("webview")
    }

    internal fun setWebViewUrl(url: String?) {
        if (webViewUrl == url) return
        webViewUrl = url
        publish("webview")
    }

    internal fun updateInsets(top: Int, bottom: Int, left: Int, right: Int) {
        val next = SafeAreaInsets(top, bottom, left, right)
        if (next == safeArea) return
        safeArea = next
        NativeBridge.injectSafeAreaCss(next)
        publish("safeArea")
    }

    fun getSafeArea(): SafeAreaInsets = safeArea

    internal fun updateKeyboard(visible: Boolean, height: Int) {
        val next = KeyboardState(visible, height)
        if (next == keyboard) return
        keyboard = next
        publish("keyboard")
    }

    internal fun snapshot(changed: String? = null) = AppStateSnapshot(
        lifecycle = lifecycle,
        isForeground = isForeground,
        isInPiP = isInPiP,
        network = network,
        orientation = orientation,
        webView = WebViewState(webViewVisible, webViewFocused, webViewUrl),
        keyboard = keyboard,
        safeArea = safeArea,
        battery = battery,
        audio = audio,
        display = display,
        theme = theme,
        locale = locale,
        call = call,
        changed = changed,
    )

    internal fun currentJson() = snapshot().toJson()

    fun getResumedActivity(): Activity? = resumedActivity.get()

    fun getApplicationContext(): Context? = app

    private fun publish(changed: String) {
        val snap = snapshot(changed)
        NativeBridge.emit("app.state", snap.toJson())
        when (changed) {
            "lifecycle", "init" -> NativeBridge.emit(
                "app.lifecycle",
                org.json.JSONObject()
                    .put("lifecycle", snap.lifecycle)
                    .put("isForeground", snap.isForeground)
                    .put("timestamp", snap.timestamp),
            )
            "pip" -> NativeBridge.emit(
                "app.pip",
                org.json.JSONObject()
                    .put("isInPiP", snap.isInPiP)
                    .put("timestamp", snap.timestamp),
            )
            "network", "init" -> NativeBridge.emit("app.network", snap.network.toJson())
            "orientation", "init" -> NativeBridge.emit(
                "app.orientation",
                org.json.JSONObject()
                    .put("orientation", snap.orientation)
                    .put("timestamp", snap.timestamp),
            )
            "webview", "init" -> NativeBridge.emit("app.webview", snap.webView?.toJson())
            "keyboard", "init" -> NativeBridge.emit("app.keyboard", snap.keyboard.toJson())
            "safeArea", "init" -> NativeBridge.emit("app.safeArea", snap.safeArea.toJson())
            "battery", "init" -> NativeBridge.emit("app.battery", snap.battery.toJson())
            "audio", "init" -> NativeBridge.emit("app.audio", snap.audio.toJson())
            "display", "init" -> NativeBridge.emit("app.display", snap.display.toJson())
            "theme", "init" -> NativeBridge.emit("app.theme", snap.theme.toJson())
            "locale", "init" -> NativeBridge.emit("app.locale", snap.locale.toJson())
            "call", "init" -> NativeBridge.emit("app.call", snap.call.toJson())
        }
    }

    private fun attachKeyboardListener(activity: Activity) {
        detachKeyboardListener(activity)
        val root = activity.window.decorView.rootView
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val screenH = root.rootView.height
            val keypadH = (screenH - rect.bottom).coerceAtLeast(0)
            val visible = keypadH > screenH * 0.15
            updateKeyboard(visible, if (visible) keypadH else 0)
        }
        globalLayoutListener = listener
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun detachKeyboardListener(activity: Activity) {
        val listener = globalLayoutListener ?: return
        val observer = activity.window.decorView.rootView.viewTreeObserver
        if (observer.isAlive) observer.removeOnGlobalLayoutListener(listener)
        globalLayoutListener = null
    }

    private fun refreshInsetsFromActivity(activity: Activity) {
        val root = activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(root) ?: return
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        updateInsets(
            maxOf(bars.top, cutout.top),
            maxOf(bars.bottom, cutout.bottom),
            maxOf(bars.left, cutout.left),
            maxOf(bars.right, cutout.right),
        )
    }

    private fun refreshNetwork() {
        val cm = connectivityManager ?: return
        val active = cm.activeNetwork
        if (active == null) {
            network = NetworkState(false, "none")
            publish("network")
            return
        }
        val caps = cm.getNetworkCapabilities(active)
        val type = when {
            caps == null -> "unknown"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "unknown"
        }
        network = NetworkState(true, type)
        publish("network")
    }

    private fun refreshBattery(ctx: Context?) {
        val context = ctx ?: app ?: return
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f
        val charging = bm.isCharging
        val lowPower = pm.isPowerSaveMode
        val next = BatteryState(level, charging, lowPower)
        if (next == battery) return
        battery = next
        publish("battery")
    }

    private fun refreshAudio() {
        val am = audioManager ?: return
        var route = "speaker"
        var bt = false
        var name: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val active = outs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ||
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            when (active?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    route = "bluetooth"; bt = true
                    name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) active.productName?.toString() else null
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> route = "wired"
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> route = "earpiece"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> route = "speaker"
            }
        } else {
            @Suppress("DEPRECATION")
            when {
                am.isBluetoothScoOn || am.isBluetoothA2dpOn -> { route = "bluetooth"; bt = true }
                am.isWiredHeadsetOn -> route = "wired"
                am.isSpeakerphoneOn -> route = "speaker"
                else -> route = "earpiece"
            }
        }

        val next = AudioRouteState(route, bt, name)
        if (next == audio) return
        audio = next
        publish("audio")
    }

    @Suppress("DEPRECATION")
    private fun listenCallState() {
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (_: SecurityException) {
            // READ_PHONE_STATE not granted — call state stays idle.
        }
    }

    private fun readOrientation(config: Configuration) = when (config.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> "unknown"
    }

    private fun readDisplay(ctx: Context): DisplayState {
        val dm = ctx.resources.displayMetrics
        return DisplayState(dm.widthPixels, dm.heightPixels, dm.density, dm.scaledDensity / dm.density)
    }

    private fun readTheme(config: Configuration): ThemeState {
        val night = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return ThemeState(night == Configuration.UI_MODE_NIGHT_YES)
    }

    private fun readLocale(): LocaleState {
        val l = Locale.getDefault()
        return LocaleState(l.language, l.country, TimeZone.getDefault().id)
    }
}
