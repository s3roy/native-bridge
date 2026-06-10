package com.bridge

import android.webkit.CookieManager
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

/** WebView HTTP cookies — set, read, remove from native or web. */
object BridgeCookies {

    fun setCookie(webView: WebView, params: JSONObject): JSONObject {
        val url = resolveUrl(webView, params)
        val name = params.optString("name")
        if (name.isEmpty()) throw IllegalArgumentException("name required")
        val value = params.optString("value", "")
        val cookieString = buildCookieString(name, value, params)
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        manager.setAcceptThirdPartyCookies(webView, true)
        manager.setCookie(url, cookieString)
        manager.flush()
        return JSONObject().put("set", true).put("name", name).put("url", url)
    }

    fun setCookies(webView: WebView, params: JSONObject): JSONObject {
        val cookies = params.optJSONArray("cookies") ?: JSONArray()
        var count = 0
        for (i in 0 until cookies.length()) {
            val c = cookies.optJSONObject(i) ?: continue
            setCookie(webView, c)
            count++
        }
        return JSONObject().put("set", count)
    }

    fun getCookies(webView: WebView, params: JSONObject): JSONObject {
        val url = resolveUrl(webView, params)
        val raw = CookieManager.getInstance().getCookie(url) ?: ""
        val list = JSONArray()
        if (raw.isNotEmpty()) {
            raw.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
                val eq = part.indexOf('=')
                if (eq > 0) {
                    list.put(
                        JSONObject()
                            .put("name", part.substring(0, eq).trim())
                            .put("value", part.substring(eq + 1).trim())
                            .put("url", url),
                    )
                }
            }
        }
        return JSONObject().put("url", url).put("cookies", list)
    }

    fun removeCookie(webView: WebView, params: JSONObject): JSONObject {
        val url = resolveUrl(webView, params)
        val name = params.optString("name")
        if (name.isEmpty()) throw IllegalArgumentException("name required")
        val expired = buildCookieString(name, "", params) + "; Max-Age=0"
        val manager = CookieManager.getInstance()
        manager.setCookie(url, expired)
        manager.flush()
        return JSONObject().put("removed", true).put("name", name)
    }

    /** Clear all cookies, or only cookies for [url] when provided. */
    fun clearCookies(webView: WebView, params: JSONObject): JSONObject {
        val url = params.optString("url", "").takeIf { it.isNotEmpty() }
        val manager = CookieManager.getInstance()
        if (url == null) {
            manager.removeAllCookies(null)
            manager.flush()
            return JSONObject().put("clearedAll", true)
        }
        val raw = manager.getCookie(url) ?: ""
        var removed = 0
        raw.split(";").map { it.trim() }.forEach { part ->
            val name = part.substringBefore('=').trim()
            if (name.isNotEmpty()) {
                manager.setCookie(url, "$name=; Max-Age=0; Path=/")
                removed++
            }
        }
        manager.flush()
        return JSONObject().put("cleared", removed).put("url", url)
    }

    private fun resolveUrl(webView: WebView, params: JSONObject): String {
        params.optString("url", "").takeIf { it.isNotEmpty() }?.let { return it }
        webView.url?.takeIf { it.isNotEmpty() }?.let { return it }
        throw IllegalStateException("url required (or load a page in the WebView first)")
    }

    private fun buildCookieString(name: String, value: String, params: JSONObject): String {
        val sb = StringBuilder().append(name).append('=').append(value)
        params.optString("domain", "").takeIf { it.isNotEmpty() }?.let { sb.append("; Domain=").append(it) }
        params.optString("path", "").takeIf { it.isNotEmpty() }?.let { sb.append("; Path=").append(it) }
            ?: sb.append("; Path=/")
        if (params.has("maxAge")) {
            sb.append("; Max-Age=").append(params.optLong("maxAge"))
        } else if (params.has("expires")) {
            sb.append("; Expires=").append(params.optString("expires"))
        }
        if (params.optBoolean("secure", false)) sb.append("; Secure")
        if (params.optBoolean("httpOnly", false)) sb.append("; HttpOnly")
        params.optString("sameSite", "").takeIf { it.isNotEmpty() }?.let {
            sb.append("; SameSite=").append(it)
        }
        return sb.toString()
    }
}
