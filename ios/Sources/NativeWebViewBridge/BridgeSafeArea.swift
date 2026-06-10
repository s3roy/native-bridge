import Foundation
import WebKit

enum BridgeSafeArea {

    static func injectCss(into webView: WKWebView, insets: SafeAreaInsets) {
        let js = """
        (function () {
          var s = document.documentElement.style;
          s.setProperty('--native-safe-area-top', '\(Int(insets.top))px');
          s.setProperty('--native-safe-area-bottom', '\(Int(insets.bottom))px');
          s.setProperty('--native-safe-area-left', '\(Int(insets.left))px');
          s.setProperty('--native-safe-area-right', '\(Int(insets.right))px');
          s.setProperty('--sat', '\(Int(insets.top))px');
          s.setProperty('--sab', '\(Int(insets.bottom))px');
          s.setProperty('--sal', '\(Int(insets.left))px');
          s.setProperty('--sar', '\(Int(insets.right))px');
          var meta = document.querySelector('meta[name=viewport]');
          if (meta && meta.content.indexOf('viewport-fit=cover') < 0) {
            meta.content = meta.content + (meta.content ? ', ' : '') + 'viewport-fit=cover';
          }
        })();
        """
        webView.evaluateJavaScript(js, completionHandler: nil)
    }
}
