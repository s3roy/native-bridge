package com.bridge.demo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bridge.BridgeWebView
import com.bridge.NativeBridge
import com.bridge.WebEvents
import com.bridge.WebViewLoadedPhase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: BridgeWebView
    private lateinit var urlInput: EditText
    private lateinit var eventLog: TextView
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.web)
        urlInput = findViewById(R.id.urlInput)
        eventLog = findViewById(R.id.eventLog)

        // Emulator → host machine localhost. Physical device → use your LAN IP.
        val defaultUrl = "http://10.0.2.2:3001/playground"
        urlInput.setText(
            getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_URL, defaultUrl),
        )

        findViewById<Button>(R.id.loadBtn).setOnClickListener { loadUrl() }

        NativeBridge.putData("demoApp", "android")
        NativeBridge.putData("authToken", "demo-token-android")

        NativeBridge.setOnWebViewLoaded { payload, webViewId, _ ->
            appendLog(
                "WEBVIEW_LOADED [${payload.phase}] id=$webViewId\n" +
                    "  url=${payload.url}\n" +
                    "  title=${payload.title}",
            )
            when (payload.phase) {
                WebViewLoadedPhase.DOM -> Unit
                WebViewLoadedPhase.COMPLETE -> Unit
                WebViewLoadedPhase.MANUAL -> Unit
            }
        }

        NativeBridge.setWebEventListener { event, payload, webViewId, _ ->
            if (event == WebEvents.WEBVIEW_LOADED) return@setWebEventListener
            appendLog("send: $event (wv=$webViewId) → $payload")
        }

        webView.setOnWebViewLoaded { payload ->
            appendLog("per-WebView loaded: ${payload.phase} ${payload.url}")
        }

        loadUrl()
    }

    private fun loadUrl() {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) return
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_URL, url).apply()
        appendLog("Loading $url")
        webView.loadUrl(url)
    }

    private fun appendLog(line: String) {
        val entry = "${timeFmt.format(Date())}  $line\n"
        runOnUiThread {
            eventLog.append(entry)
        }
    }

    companion object {
        private const val PREFS = "native_bridge_demo"
        private const val KEY_URL = "playground_url"
    }
}
