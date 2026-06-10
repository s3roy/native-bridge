package com.bridge

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONObject
import java.util.WeakHashMap

/**
 * WebView HTTP cache control.
 *
 * **smart** (default): HTML/document always revalidated; hashed JS/CSS chunks stay cached
 * (pairs with Vercel `immutable` headers — lower bandwidth bills).
 */
object BridgeWebViewCache {

    const val MODE_DEFAULT = "default"
    const val MODE_NO_CACHE = "noCache"
    const val MODE_CACHE_ONLY = "cacheOnly"
    const val MODE_SMART = "smart"

    private const val BYPASS_HEADER = "X-Bridge-Cache-Bypass"

    private data class Policy(
        val mode: String = MODE_SMART,
        val clearOnLaunch: Boolean = false,
    )

    private val policies = WeakHashMap<WebView, Policy>()
    private var globalPolicy = Policy()

    fun setPolicy(webView: WebView?, params: JSONObject): JSONObject {
        val mode = params.optString("mode", globalPolicy.mode)
        val clearOnLaunch = params.optBoolean("clearOnLaunch", globalPolicy.clearOnLaunch)
        val policy = Policy(mode, clearOnLaunch)
        if (webView != null) {
            policies[webView] = policy
            apply(webView, policy)
            if (clearOnLaunch) clear(webView, params)
        } else {
            globalPolicy = policy
        }
        return getInfo(webView)
    }

    fun getInfo(webView: WebView?): JSONObject {
        val p = policyFor(webView)
        return JSONObject()
            .put("mode", p.mode)
            .put("clearOnLaunch", p.clearOnLaunch)
            .put("documentHeaders", JSONObject(documentHeaders()))
    }

    fun apply(webView: WebView, policy: Policy = policyFor(webView)) {
        webView.settings.cacheMode = when (policy.mode) {
            MODE_NO_CACHE -> WebSettings.LOAD_NO_CACHE
            MODE_CACHE_ONLY -> WebSettings.LOAD_CACHE_ONLY
            MODE_DEFAULT, MODE_SMART -> WebSettings.LOAD_DEFAULT
            else -> WebSettings.LOAD_DEFAULT
        }
        if (policy.clearOnLaunch) clear(webView, JSONObject())
    }

    fun applyOnAttach(webView: WebView) {
        apply(webView, policyFor(webView))
    }

    fun clear(webView: WebView, params: JSONObject): JSONObject {
        val includeDisk = params.optBoolean("disk", true)
        val includeCookies = params.optBoolean("cookies", false)
        webView.clearCache(includeDisk)
        webView.clearFormData()
        if (includeCookies) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        return JSONObject().put("cleared", true)
    }

    fun reload(webView: WebView, bypassCache: Boolean): JSONObject {
        if (bypassCache) {
            val url = webView.url
            if (!url.isNullOrBlank()) {
                webView.loadUrl(url, documentHeaders())
            } else {
                webView.reload()
            }
        } else {
            webView.reload()
        }
        return JSONObject().put("reloaded", true).put("bypassCache", bypassCache)
    }

    fun loadUrl(webView: WebView, url: String, extra: Map<String, String>? = null) {
        val headers = if (shouldBypassDocument(url, policyFor(webView))) {
            mergeDocumentHeaders(extra)
        } else {
            extra ?: emptyMap()
        }
        if (headers.isEmpty()) webView.loadUrl(url) else webView.loadUrl(url, headers)
    }

    fun isSmartMode(webView: WebView): Boolean =
        policyFor(webView).mode == MODE_SMART

    fun shouldInterceptMainDocument(webView: WebView, url: String, isMainFrame: Boolean, method: String): Boolean {
        if (!isMainFrame || method != "GET") return false
        val policy = policyFor(webView)
        return when (policy.mode) {
            MODE_NO_CACHE -> true
            MODE_SMART -> shouldBypassDocument(url, policy)
            else -> false
        }
    }

    fun documentHeaders(): Map<String, String> = mapOf(
        "Cache-Control" to "no-cache, no-store, must-revalidate",
        "Pragma" to "no-cache",
        BYPASS_HEADER to "1",
    )

    fun mergeDocumentHeaders(extra: Map<String, String>?): Map<String, String> {
        val merged = documentHeaders().toMutableMap()
        extra?.forEach { (k, v) -> merged[k] = v }
        return merged
    }

    fun hasBypassHeader(headers: Map<String, String>): Boolean =
        headers.containsKey(BYPASS_HEADER)

    /** Static hashed assets — let CDN / WebView cache serve them. */
    fun isStaticAsset(url: String): Boolean {
        val path = url.substringBefore('?').lowercase()
        if (path.contains("/_next/static/")) return true
        if (path.contains("/static/")) return true
        if (path.contains("/assets/")) return true
        val ext = path.substringAfterLast('.', "")
        return ext in STATIC_EXTENSIONS
    }

    fun shouldBypassDocument(url: String, policy: Policy = globalPolicy): Boolean {
        if (policy.mode == MODE_NO_CACHE) return true
        if (policy.mode != MODE_SMART) return false
        if (isStaticAsset(url)) return false
        val path = url.substringBefore('?')
        if (path.endsWith(".html", ignoreCase = true)) return true
        // SPA / document routes — no static extension
        return !path.substringAfterLast('/').contains('.')
    }

    private fun policyFor(webView: WebView?): Policy =
        if (webView != null) policies[webView] ?: globalPolicy else globalPolicy

    private val STATIC_EXTENSIONS = setOf(
        "js", "mjs", "css", "woff", "woff2", "ttf", "otf", "eot",
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif",
        "map", "json", "wasm", "br", "gz",
    )
}
