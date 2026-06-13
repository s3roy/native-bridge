import Foundation
import WebKit

/// Built-in web → native event names (`NativeBridge.send`).
public enum WebEvents {
    public static let webViewLoaded = "WEBVIEW_LOADED"
}

public enum WebViewLoadedPhase: String {
    case dom
    case complete
    case manual

    public static func from(_ raw: String?) -> WebViewLoadedPhase {
        guard let raw = raw, let phase = WebViewLoadedPhase(rawValue: raw) else { return .manual }
        return phase
    }
}

/// Parsed payload for [WebEvents.webViewLoaded].
public struct WebViewLoadedPayload {
    public let url: String
    public let title: String
    public let timestamp: TimeInterval
    public let readyState: String
    public let phase: WebViewLoadedPhase
    public let referrer: String?
    public let webViewId: String?
    public let route: String?
    public let extras: [String: Any]

    public static func parse(_ payload: Any?, fallbackWebViewId: String) -> WebViewLoadedPayload {
        let dict: [String: Any]
        if let map = payload as? [String: Any] {
            dict = map
        } else {
            dict = [:]
        }

        let known: Set<String> = [
            "event", "url", "title", "timestamp", "readyState",
            "phase", "referrer", "webViewId", "route",
        ]
        var extras: [String: Any] = [:]
        for (key, value) in dict where !known.contains(key) {
            extras[key] = value
        }

        let webViewId = (dict["webViewId"] as? String).flatMap { $0.isEmpty ? nil : $0 }
            ?? fallbackWebViewId

        return WebViewLoadedPayload(
            url: dict["url"] as? String ?? "",
            title: dict["title"] as? String ?? "",
            timestamp: (dict["timestamp"] as? NSNumber)?.doubleValue ?? Date().timeIntervalSince1970 * 1000,
            readyState: dict["readyState"] as? String ?? "unknown",
            phase: WebViewLoadedPhase.from(dict["phase"] as? String),
            referrer: (dict["referrer"] as? String).flatMap { $0.isEmpty ? nil : $0 },
            webViewId: webViewId,
            route: (dict["route"] as? String).flatMap { $0.isEmpty ? nil : $0 },
            extras: extras
        )
    }
}

public typealias WebViewLoadedHandler = (
    _ payload: WebViewLoadedPayload,
    _ webViewId: String,
    _ webView: WKWebView
) -> Void
