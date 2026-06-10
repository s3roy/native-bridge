package com.bridge

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Receives custom events from the web layer (`NativeBridge.send`). */
typealias WebEventListener = (event: String, payload: Any?, webViewId: String, webView: WebView) -> Unit

/**
 * Central, app-wide bridge. Singleton so capture (network/notifications) can feed
 * data to every attached WebView without the host app writing any glue code.
 */
object NativeBridge {

    private val attachedViews = mutableListOf<WebView>()
    private val webViewIds = java.util.WeakHashMap<WebView, String>()
    private val dataStore = ConcurrentHashMap<String, Any?>()
    private var deviceInfoProvider: () -> Map<String, Any?> = { emptyMap() }
    private var webViewSeq = 0

    val apiCalls = ApiCallStore()
    val notifications = NotificationStore()

    private var globalWebEventListener: WebEventListener? = null
    private val webEventListeners = CopyOnWriteArrayList<WebEventListener>()
    private val perWebViewEventListeners = java.util.WeakHashMap<WebView, (String, Any?) -> Unit>()
    private val emitListeners = CopyOnWriteArrayList<(String, Any?) -> Unit>()

    // ---- Host app helpers (optional) ----------------------------------------

    /** Publish any value the web side can read via NativeBridge.getData(key). */
    fun putData(key: String, value: Any?) = setData(key, value, "native")

    /**
     * Store a value in the shared app-wide store. All attached WebViews receive [onData].
     * [sourceWebViewId] identifies the writer ("native", "wv_1", …).
     */
    fun setData(key: String, value: Any?, sourceWebViewId: String?) {
        if (key.isEmpty()) return
        dataStore[key] = value
        val payload = JSONObject()
            .put("key", key)
            .put("value", wrapValue(value))
        if (sourceWebViewId != null) payload.put("source", sourceWebViewId)
        emit("data", payload)
    }

    fun removeData(key: String, sourceWebViewId: String?) {
        if (key.isEmpty()) return
        dataStore.remove(key)
        val payload = JSONObject()
            .put("key", key)
            .put("value", JSONObject.NULL)
            .put("removed", true)
        if (sourceWebViewId != null) payload.put("source", sourceWebViewId)
        emit("data", payload)
    }

    internal fun webViewId(webView: WebView): String =
        webViewIds.getOrPut(webView) { "wv_${++webViewSeq}" }

    fun setDeviceInfoProvider(provider: () -> Map<String, Any?>) {
        deviceInfoProvider = provider
    }

    internal fun getData(key: String): Any? = dataStore[key]

    internal fun getAllData(): Map<String, Any?> = dataStore.toMap()

    internal fun getDeviceInfo(): Map<String, Any?> = deviceInfoProvider()

    // ---- WebView wiring ------------------------------------------------------

    /**
     * Attach to any existing WebView. The drop-in [BridgeWebView] calls this for
     * you. Safe to call multiple times.
     */
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    fun attach(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }
        webView.addJavascriptInterface(JsInterface(webView), "AndroidBridge")
        if (!attachedViews.contains(webView)) attachedViews.add(webView)
        webViewId(webView)
        inject(webView)
        // Push current app state immediately so web has snapshot on load.
        emit("app.state", AppStateMonitor.currentJson())
    }

    fun detach(webView: WebView) {
        attachedViews.remove(webView)
    }

    /** Inject the web SDK. Call from WebViewClient.onPageStarted for SPA reloads. */
    fun inject(webView: WebView) {
        webView.post {
            webView.evaluateJavascript(BridgeScript.JS, null)
            injectSafeAreaCss(AppStateMonitor.getSafeArea())
        }
    }

    internal fun injectSafeAreaCss(insets: SafeAreaInsets) {
        attachedViews.forEach { wv -> BridgeSafeArea.injectCss(wv, insets) }
    }

    // ---- Outbound events to web ---------------------------------------------

    /** Observe every [emit] / [publishEvent] (e.g. React Native event relay). */
    fun addEmitListener(listener: (event: String, payload: Any?) -> Unit) {
        emitListeners.add(listener)
    }

    fun removeEmitListener(listener: (event: String, payload: Any?) -> Unit) {
        emitListeners.remove(listener)
    }

    fun emit(event: String, payload: Any?) {
        val js = "window.NativeBridge && window.NativeBridge.__emit(" +
            "${JSONObject.quote(event)}, ${wrap(payload)})"
        attachedViews.forEach { wv -> wv.post { wv.evaluateJavascript(js, null) } }
        emitListeners.forEach { listener ->
            try {
                listener(event, payload)
            } catch (_: Exception) {
            }
        }
    }

    internal fun recordApiCall(call: ApiCall) {
        apiCalls.add(call)
        emit("api.call", call.toJson())
    }

    fun recordNotification(record: NotificationRecord) {
        notifications.add(record)
        emit("notification", record.toJson())
    }

    /** Push any custom realtime event to all attached WebViews. */
    fun publishEvent(event: String, payload: Any?) {
        emit(event, payload)
    }

    /** Handle every `NativeBridge.send(event, payload)` from web (app-wide). */
    fun setWebEventListener(listener: WebEventListener?) {
        globalWebEventListener = listener
    }

    fun addWebEventListener(listener: WebEventListener) {
        webEventListeners.add(listener)
    }

    fun removeWebEventListener(listener: WebEventListener) {
        webEventListeners.remove(listener)
    }

    internal fun setWebViewEventListener(
        webView: WebView,
        listener: ((event: String, payload: Any?) -> Unit)?,
    ) {
        if (listener == null) perWebViewEventListeners.remove(webView)
        else perWebViewEventListeners[webView] = listener
    }

    private fun deliverWebEvent(
        event: String,
        payload: Any?,
        webViewId: String,
        webView: WebView,
    ) {
        globalWebEventListener?.invoke(event, payload, webViewId, webView)
        webEventListeners.forEach { listener ->
            try {
                listener(event, payload, webViewId, webView)
            } catch (_: Exception) {
            }
        }
        perWebViewEventListeners[webView]?.invoke(event, payload)
    }

    // ---- Inbound requests from web ------------------------------------------

    private class JsInterface(private val webView: WebView) {
        @JavascriptInterface
        fun postMessage(message: String) {
            val msg = try { JSONObject(message) } catch (e: Exception) { return }
            when (msg.optString("type")) {
                "event" -> {
                    val event = msg.optString("event")
                    if (event.isEmpty()) return
                    val payload = msg.opt("payload")
                    deliverWebEvent(event, payload, webViewId(webView), webView)
                    return
                }
                "request" -> Unit
                else -> return
            }
            val id = msg.optString("id")
            val method = msg.optString("method")
            val params = msg.optJSONObject("params") ?: JSONObject()

            when (method) {
                "bridge.requestPermission" -> {
                    val perm = params.optString("permission")
                    BridgePermissions.request(webView.context.applicationContext, perm) { result ->
                        webView.post { resolve(id, result.toJson()) }
                    }
                    return
                }
                "bridge.canGoBackInWebView" -> {
                    resolve(id, BridgeBackPress.canGoBack(webView))
                    return
                }
                "bridge.goBackInWebView" -> {
                    resolve(id, BridgeBackPress.goBack(webView))
                    return
                }
                "bridge.getSafeAreaInsets" -> {
                    resolve(id, AppStateMonitor.getSafeArea().toJson())
                    return
                }
                "bridge.setApplySafeAreaPadding" -> {
                    val enabled = params.optBoolean("enabled", true)
                    if (webView is BridgeWebView) {
                        webView.setApplySafeAreaPadding(enabled)
                    }
                    resolve(id, JSONObject().put("enabled", enabled))
                    return
                }
                "bridge.setSystemUi" -> {
                    resolve(id, BridgeSystemUi.set(webView.context, params))
                    return
                }
                "bridge.getSystemUi" -> {
                    resolve(id, BridgeSystemUi.get())
                    return
                }
                "bridge.getNotificationSettings" -> {
                    resolve(id, BridgeNotifications.getSettings(webView.context))
                    return
                }
                "bridge.cancelNotification" -> {
                    resolve(id, BridgeNotifications.cancel(webView.context, params))
                    return
                }
                "bridge.cancelAllNotifications" -> {
                    resolve(id, BridgeNotifications.cancelAll(webView.context))
                    return
                }
                "bridge.openNotificationSettings" -> {
                    resolve(id, BridgeNotifications.openSettings(webView.context))
                    return
                }
                "bridge.setWebViewCachePolicy" -> {
                    resolve(id, BridgeWebViewCache.setPolicy(webView, params))
                    return
                }
                "bridge.getWebViewCacheInfo" -> {
                    resolve(id, BridgeWebViewCache.getInfo(webView))
                    return
                }
                "bridge.clearWebViewCache" -> {
                    resolve(id, BridgeWebViewCache.clear(webView, params))
                    return
                }
                "bridge.reloadWebView" -> {
                    resolve(
                        id,
                        BridgeWebViewCache.reload(
                            webView,
                            params.optBoolean("bypassCache", true),
                        ),
                    )
                    return
                }
                "bridge.setCookie" -> {
                    resolve(id, BridgeCookies.setCookie(webView, params))
                    return
                }
                "bridge.setCookies" -> {
                    resolve(id, BridgeCookies.setCookies(webView, params))
                    return
                }
                "bridge.getCookies" -> {
                    resolve(id, BridgeCookies.getCookies(webView, params))
                    return
                }
                "bridge.removeCookie" -> {
                    resolve(id, BridgeCookies.removeCookie(webView, params))
                    return
                }
                "bridge.clearCookies" -> {
                    resolve(id, BridgeCookies.clearCookies(webView, params))
                    return
                }
            }

            if (BridgeDispatcher.dispatchAsync(
                    webView.context.applicationContext,
                    method,
                    params,
                ) { result, error ->
                    webView.post {
                        if (error != null) reject(id, error)
                        else resolve(id, result)
                    }
                }
            ) {
                return
            }

            try {
                resolve(
                    id,
                    BridgeDispatcher.dispatch(
                        webView.context.applicationContext,
                        method,
                        params,
                        webViewId(webView),
                    ),
                )
            } catch (e: Exception) {
                reject(id, e.message ?: "error")
            }
        }

        private fun resolve(id: String, result: Any?) =
            eval("window.NativeBridge.__resolve('$id', ${wrap(result)})")

        private fun reject(id: String, error: String) =
            eval("window.NativeBridge.__reject('$id', ${JSONObject.quote(error)})")

        private fun eval(js: String) = webView.post { webView.evaluateJavascript(js, null) }
    }

    // ---- JSON helpers --------------------------------------------------------

    private fun wrap(v: Any?): String = when (v) {
        null -> "null"
        is JSONObject, is JSONArray -> v.toString()
        is Number, is Boolean -> v.toString()
        is String -> JSONObject.quote(v)
        else -> JSONObject.wrap(v)?.toString() ?: "null"
    }

    private fun wrapValue(v: Any?): Any? = v ?: JSONObject.NULL
}
