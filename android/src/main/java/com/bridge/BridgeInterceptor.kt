package com.bridge

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.util.UUID

/**
 * OkHttp interceptor that auto-captures every API call (request + response) and
 * feeds it to the bridge. Add ONE line to your existing OkHttpClient:
 *
 *   OkHttpClient.Builder().addInterceptor(BridgeInterceptor()).build()
 *
 * Most apps (Retrofit/OkHttp) need only this single line for full network capture.
 */
class BridgeInterceptor(
    /** Max bytes of a response body to capture (avoids huge payloads). */
    private val maxBodyBytes: Long = 1_024 * 1024,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val start = System.currentTimeMillis()
        val reqBody = request.body?.let { safeBody(it) }

        try {
            val response = chain.proceed(request)
            val bodyText = runCatching {
                response.peekBody(maxBodyBytes).string()
            }.getOrNull()

            NativeBridge.recordApiCall(
                ApiCall(
                    id = UUID.randomUUID().toString(),
                    method = request.method,
                    url = request.url.toString(),
                    requestHeaders = request.headers.toMap(),
                    requestBody = reqBody,
                    status = response.code,
                    responseHeaders = response.headers.toMap(),
                    responseBody = bodyText,
                    durationMs = System.currentTimeMillis() - start,
                    timestamp = start,
                )
            )
            return response
        } catch (e: Exception) {
            NativeBridge.recordApiCall(
                ApiCall(
                    id = UUID.randomUUID().toString(),
                    method = request.method,
                    url = request.url.toString(),
                    requestHeaders = request.headers.toMap(),
                    requestBody = reqBody,
                    status = -1,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    durationMs = System.currentTimeMillis() - start,
                    timestamp = start,
                    error = e.message,
                )
            )
            throw e
        }
    }

    private fun safeBody(body: RequestBody): String? = runCatching {
        val buffer = Buffer()
        body.writeTo(buffer)
        buffer.readUtf8()
    }.getOrNull()

    private fun Headers.toMap(): Map<String, String> =
        (0 until size).associate { name(it) to value(it) }
}
