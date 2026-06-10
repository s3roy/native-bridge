package com.bridge

import android.webkit.WebView

/** Injects CSS env-style variables so web pages can pad headers/footers. */
object BridgeSafeArea {

    fun injectCss(webView: WebView, insets: SafeAreaInsets) {
        val js = """
            (function () {
              var s = document.documentElement.style;
              s.setProperty('--native-safe-area-top', '${insets.top}px');
              s.setProperty('--native-safe-area-bottom', '${insets.bottom}px');
              s.setProperty('--native-safe-area-left', '${insets.left}px');
              s.setProperty('--native-safe-area-right', '${insets.right}px');
              s.setProperty('--sat', '${insets.top}px');
              s.setProperty('--sab', '${insets.bottom}px');
              s.setProperty('--sal', '${insets.left}px');
              s.setProperty('--sar', '${insets.right}px');
              var meta = document.querySelector('meta[name=viewport]');
              if (meta && meta.content.indexOf('viewport-fit=cover') < 0) {
                meta.content = meta.content + (meta.content ? ', ' : '') + 'viewport-fit=cover';
              }
            })();
        """.trimIndent()
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
