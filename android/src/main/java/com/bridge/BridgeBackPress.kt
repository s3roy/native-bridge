package com.bridge

import android.webkit.WebView
import org.json.JSONObject

/** Hardware back button → web listeners + optional WebView history back. */
object BridgeBackPress {

    /**
     * Ask the web page to handle back. [onComplete] receives true if consumed
     * (web returned true OR WebView navigated back in history).
     */
    fun handle(webView: WebView, onComplete: (Boolean) -> Unit) {
        val canGoBack = webView.canGoBack()
        val url = webView.url ?: ""
        val js = """
            (function () {
              var payload = {
                canGoBack: $canGoBack,
                url: ${JSONObject.quote(url)},
                source: "hardware"
              };
              var consumed = false;
              var listeners = (window.NativeBridge && window.NativeBridge._backListeners) || [];
              for (var i = 0; i < listeners.length; i++) {
                try {
                  if (listeners[i](payload) === true) { consumed = true; break; }
                } catch (e) {}
              }
              if (window.NativeBridge && window.NativeBridge.__emit) {
                window.NativeBridge.__emit("back.press", payload);
              }
              return consumed;
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { result ->
            val consumedByWeb = result == "true"
            if (consumedByWeb) {
                onComplete(true)
            } else if (canGoBack) {
                webView.goBack()
                NativeBridge.emit(
                    "back.navigation",
                    JSONObject()
                        .put("direction", "back")
                        .put("url", webView.url ?: "")
                        .put("source", "hardware"),
                )
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun canGoBack(webView: WebView): JSONObject =
        JSONObject().put("canGoBack", webView.canGoBack()).put("url", webView.url ?: "")

    fun goBack(webView: WebView): JSONObject {
        val could = webView.canGoBack()
        if (could) {
            webView.goBack()
            NativeBridge.emit(
                "back.navigation",
                JSONObject()
                    .put("direction", "back")
                    .put("url", webView.url ?: "")
                    .put("source", "api"),
            )
        }
        return JSONObject().put("wentBack", could).put("canGoBack", webView.canGoBack())
    }
}
