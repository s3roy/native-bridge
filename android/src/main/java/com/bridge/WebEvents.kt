package com.bridge

import android.webkit.WebView
import org.json.JSONObject

/** Built-in web → native event names (`NativeBridge.send`). */
object WebEvents {
    const val WEBVIEW_LOADED = "WEBVIEW_LOADED"
}

enum class WebViewLoadedPhase(val raw: String) {
    DOM("dom"),
    COMPLETE("complete"),
    MANUAL("manual"),
    ;

    companion object {
        fun from(raw: String?): WebViewLoadedPhase =
            entries.find { it.raw == raw } ?: MANUAL
    }
}

/** Parsed payload for [WebEvents.WEBVIEW_LOADED]. */
data class WebViewLoadedPayload(
    val url: String,
    val title: String,
    val timestamp: Long,
    val readyState: String,
    val phase: WebViewLoadedPhase,
    val referrer: String?,
    val webViewId: String?,
    val route: String?,
    val extras: Map<String, Any?>,
) {
    companion object {
        private val KNOWN_KEYS = setOf(
            "event", "url", "title", "timestamp", "readyState",
            "phase", "referrer", "webViewId", "route",
        )

        fun parse(payload: Any?, fallbackWebViewId: String): WebViewLoadedPayload {
            val json = when (payload) {
                is JSONObject -> payload
                is Map<*, *> -> JSONObject(payload)
                else -> JSONObject()
            }
            val extras = mutableMapOf<String, Any?>()
            json.keys().forEach { key ->
                if (key !in KNOWN_KEYS) extras[key] = json.opt(key)
            }
            return WebViewLoadedPayload(
                url = json.optString("url", ""),
                title = json.optString("title", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                readyState = json.optString("readyState", "unknown"),
                phase = WebViewLoadedPhase.from(json.optString("phase")),
                referrer = json.optString("referrer").takeIf { it.isNotEmpty() },
                webViewId = json.optString("webViewId").takeIf { it.isNotEmpty() }
                    ?: fallbackWebViewId,
                route = json.optString("route").takeIf { it.isNotEmpty() },
                extras = extras,
            )
        }
    }
}

typealias WebViewLoadedListener = (
    payload: WebViewLoadedPayload,
    webViewId: String,
    webView: WebView,
) -> Unit
