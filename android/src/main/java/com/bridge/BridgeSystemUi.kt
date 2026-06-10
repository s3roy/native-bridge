package com.bridge

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject

/** Status bar, navigation bar, immersive mode — controlled from web. */
object BridgeSystemUi {

    private var config = JSONObject()

    fun get(): JSONObject = JSONObject(config.toString()).apply {
        put("safeArea", AppStateMonitor.getSafeArea().toJson())
    }

    fun set(context: Context, params: JSONObject): JSONObject {
        val activity = findActivity(context) ?: throw IllegalStateException("No Activity for system UI")
        val window = activity.window

        if (params.has("statusBarColor")) {
            val color = params.optString("statusBarColor")
            if (color == "transparent") {
                window.statusBarColor = Color.TRANSPARENT
                mergeConfig("statusBarColor", "transparent")
            } else {
                window.statusBarColor = parseColor(color)
                mergeConfig("statusBarColor", color)
            }
        }

        if (params.has("navigationBarColor")) {
            val color = params.optString("navigationBarColor")
            if (color == "transparent") {
                window.navigationBarColor = Color.TRANSPARENT
                mergeConfig("navigationBarColor", "transparent")
            } else {
                window.navigationBarColor = parseColor(color)
                mergeConfig("navigationBarColor", color)
            }
        }

        when (params.optString("statusBarStyle", "")) {
            "light", "dark-content" -> {
                setLightStatusBar(window, true)
                mergeConfig("statusBarStyle", "dark-content")
            }
            "dark", "light-content" -> {
                setLightStatusBar(window, false)
                mergeConfig("statusBarStyle", "light-content")
            }
        }

        when (params.optString("navigationBarStyle", "")) {
            "light", "dark-content" -> {
                setLightNavigationBar(window, true)
                mergeConfig("navigationBarStyle", "dark-content")
            }
            "dark", "light-content" -> {
                setLightNavigationBar(window, false)
                mergeConfig("navigationBarStyle", "light-content")
            }
        }

        if (params.has("layoutFullscreen")) {
            val edge = params.optBoolean("layoutFullscreen", false)
            WindowCompat.setDecorFitsSystemWindows(window, !edge)
            mergeConfig("layoutFullscreen", edge)
        }

        if (params.has("fitsSystemWindows")) {
            val fits = params.optBoolean("fitsSystemWindows", true)
            activity.findViewById<View>(android.R.id.content)?.let { root ->
                ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                    if (fits) {
                        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                    } else {
                        v.setPadding(0, 0, 0, 0)
                    }
                    insets
                }
                root.requestApplyInsets()
            }
            mergeConfig("fitsSystemWindows", fits)
        }

        if (params.has("statusBarHidden")) {
            val hidden = params.optBoolean("statusBarHidden", false)
            controller(window).apply {
                if (hidden) hide(WindowInsetsCompat.Type.statusBars())
                else show(WindowInsetsCompat.Type.statusBars())
            }
            mergeConfig("statusBarHidden", hidden)
        }

        if (params.has("navigationBarHidden")) {
            val hidden = params.optBoolean("navigationBarHidden", false)
            controller(window).apply {
                if (hidden) hide(WindowInsetsCompat.Type.navigationBars())
                else show(WindowInsetsCompat.Type.navigationBars())
            }
            mergeConfig("navigationBarHidden", hidden)
        }

        if (params.optBoolean("immersive", false)) {
            controller(window).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            mergeConfig("immersive", true)
        } else if (params.has("immersive") && !params.optBoolean("immersive", false)) {
            controller(window).show(WindowInsetsCompat.Type.systemBars())
            mergeConfig("immersive", false)
        }

        if (params.optBoolean("keepScreenOn", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            mergeConfig("keepScreenOn", true)
        } else if (params.has("keepScreenOn") && !params.optBoolean("keepScreenOn", false)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            mergeConfig("keepScreenOn", false)
        }

        return get()
    }

    private fun controller(window: android.view.Window) =
        WindowCompat.getInsetsController(window, window.decorView)

    private fun setLightStatusBar(window: android.view.Window, light: Boolean) {
        controller(window).isAppearanceLightStatusBars = light
    }

    private fun setLightNavigationBar(window: android.view.Window, light: Boolean) {
        if (Build.VERSION.SDK_INT >= 26) {
            controller(window).isAppearanceLightNavigationBars = light
        }
    }

    private fun parseColor(hex: String): Int {
        val normalized = if (hex.startsWith("#")) hex else "#$hex"
        return Color.parseColor(normalized)
    }

    private fun mergeConfig(key: String, value: Any) {
        config.put(key, value)
    }

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return AppStateMonitor.getResumedActivity()
    }
}
