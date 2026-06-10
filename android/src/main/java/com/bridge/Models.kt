package com.bridge

import org.json.JSONArray
import org.json.JSONObject

/** A single captured native API call (request + response). */
data class ApiCall(
    val id: String,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val status: Int,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val timestamp: Long,
    val error: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("method", method)
        put("url", url)
        put("requestHeaders", JSONObject(requestHeaders as Map<*, *>))
        put("requestBody", requestBody ?: JSONObject.NULL)
        put("status", status)
        put("responseHeaders", JSONObject(responseHeaders as Map<*, *>))
        put("responseBody", responseBody ?: JSONObject.NULL)
        put("durationMs", durationMs)
        put("timestamp", timestamp)
        put("error", error ?: JSONObject.NULL)
    }
}

/** A captured push or local notification. */
data class NotificationRecord(
    val id: String,
    val title: String?,
    val body: String?,
    val data: Map<String, Any?>,
    val timestamp: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title ?: JSONObject.NULL)
        put("body", body ?: JSONObject.NULL)
        put("data", JSONObject(data as Map<*, *>))
        put("timestamp", timestamp)
    }
}

/** Thread-safe bounded ring buffer for captured API calls. */
class ApiCallStore(private val max: Int = 200) {
    private val items = ArrayDeque<ApiCall>()

    @Synchronized
    fun add(c: ApiCall) {
        items.addLast(c)
        while (items.size > max) items.removeFirst()
    }

    @Synchronized
    fun all(): List<ApiCall> = items.toList()

    @Synchronized
    fun query(params: JSONObject): JSONArray {
        val urlContains = params.optString("urlContains", "")
        val method = params.optString("method", "")
        val limit = params.optInt("limit", max)
        val filtered = items.filter {
            (urlContains.isEmpty() || it.url.contains(urlContains, ignoreCase = true)) &&
                (method.isEmpty() || it.method.equals(method, ignoreCase = true))
        }.takeLast(limit)
        return JSONArray(filtered.map { it.toJson() })
    }
}

/** Thread-safe bounded ring buffer for captured notifications. */
class NotificationStore(private val max: Int = 100) {
    private val items = ArrayDeque<NotificationRecord>()

    @Synchronized
    fun add(n: NotificationRecord) {
        items.addLast(n)
        while (items.size > max) items.removeFirst()
    }

    @Synchronized
    fun all(): List<NotificationRecord> = items.toList()
}
