import Foundation
import WebKit

enum BridgeBackPress {

    static func handle(webView: WKWebView, completion: @escaping (Bool) -> Void) {
        let canGoBack = webView.canGoBack
        let url = webView.url?.absoluteString ?? ""
        let js = """
        (function () {
          var payload = { canGoBack: \(canGoBack), url: \(jsonString(url)), source: "hardware" };
          var consumed = false;
          var listeners = (window.NativeBridge && window.NativeBridge._backListeners) || [];
          for (var i = 0; i < listeners.length; i++) {
            try { if (listeners[i](payload) === true) { consumed = true; break; } } catch (e) {}
          }
          if (window.NativeBridge && window.NativeBridge.__emit) {
            window.NativeBridge.__emit("back.press", payload);
          }
          return consumed;
        })();
        """
        webView.evaluateJavaScript(js) { result, _ in
            let consumedByWeb = (result as? Bool) == true || (result as? NSNumber)?.boolValue == true
            if consumedByWeb {
                completion(true)
            } else if canGoBack {
                webView.goBack()
                NativeBridge.shared.emit("back.navigation", [
                    "direction": "back",
                    "url": webView.url?.absoluteString ?? "",
                    "source": "hardware",
                ])
                completion(true)
            } else {
                completion(false)
            }
        }
    }

    static func canGoBack(_ webView: WKWebView) -> [String: Any] {
        ["canGoBack": webView.canGoBack, "url": webView.url?.absoluteString ?? ""]
    }

    static func goBack(_ webView: WKWebView) -> [String: Any] {
        let could = webView.canGoBack
        if could {
            webView.goBack()
            NativeBridge.shared.emit("back.navigation", [
                "direction": "back",
                "url": webView.url?.absoluteString ?? "",
                "source": "api",
            ])
        }
        return ["wentBack": could, "canGoBack": webView.canGoBack]
    }

    static func emitGestureBack(webView: WKWebView) {
        let payload: [String: Any] = [
            "canGoBack": webView.canGoBack,
            "url": webView.url?.absoluteString ?? "",
            "source": "gesture",
        ]
        let js = """
        (function () {
          var payload = \(jsonDict(payload));
          var listeners = (window.NativeBridge && window.NativeBridge._backListeners) || [];
          listeners.forEach(function (cb) { try { cb(payload); } catch (e) {} });
          if (window.NativeBridge && window.NativeBridge.__emit) {
            window.NativeBridge.__emit("back.press", payload);
          }
        })();
        """
        webView.evaluateJavaScript(js, completionHandler: nil)
    }

    private static func jsonString(_ s: String) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: [s]),
              let wrapped = String(data: data, encoding: .utf8) else { return "\"\"" }
        return String(wrapped.dropFirst().dropLast())
    }

    private static func jsonDict(_ dict: [String: Any]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: dict),
              let s = String(data: data, encoding: .utf8) else { return "{}" }
        return s
    }
}
