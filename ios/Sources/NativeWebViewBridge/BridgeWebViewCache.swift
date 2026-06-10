import Foundation
import WebKit

/// WebView HTTP cache — **smart** mode revalidates HTML, keeps hashed static chunks cached.
enum BridgeWebViewCache {

    static let modeDefault = "default"
    static let modeNoCache = "noCache"
    static let modeCacheOnly = "cacheOnly"
    static let modeSmart = "smart"

    private static let bypassHeader = "X-Bridge-Cache-Bypass"

    private struct Policy {
        var mode: String = modeSmart
        var clearOnLaunch: Bool = false
    }

    private static var globalPolicy = Policy()
    private static var policies = NSMapTable<WKWebView, PolicyBox>.weakToStrongObjects()

    private final class PolicyBox {
        let value: Policy
        init(_ value: Policy) { self.value = value }
    }

    static func setPolicy(webView: WKWebView?, params: [String: Any]) -> [String: Any] {
        var policy = Policy()
        policy.mode = params["mode"] as? String ?? globalPolicy.mode
        policy.clearOnLaunch = params["clearOnLaunch"] as? Bool ?? globalPolicy.clearOnLaunch
        if let wv = webView {
            policies.setObject(PolicyBox(policy), forKey: wv)
            apply(webView: wv, policy: policy)
            if policy.clearOnLaunch {
                clear(webView: wv, params: params) { _ in }
            }
        } else {
            globalPolicy = policy
        }
        return getInfo(webView: webView)
    }

    static func getInfo(webView: WKWebView?) -> [String: Any] {
        let p = policy(for: webView)
        return [
            "mode": p.mode,
            "clearOnLaunch": p.clearOnLaunch,
            "documentHeaders": documentHeaders(),
        ]
    }

    static func applyOnAttach(webView: WKWebView) {
        apply(webView: webView, policy: policy(for: webView))
    }

    private static func apply(webView: WKWebView, policy: Policy) {
        if policy.clearOnLaunch {
            clear(webView: webView, params: [:]) { _ in }
        }
    }

    static func clear(
        webView: WKWebView,
        params: [String: Any],
        completion: @escaping ([String: Any]) -> Void
    ) {
        let includeCookies = params["cookies"] as? Bool ?? false
        let types = WKWebsiteDataStore.allWebsiteDataTypes()
        webView.configuration.websiteDataStore.removeData(
            ofTypes: types,
            modifiedSince: Date(timeIntervalSince1970: 0)
        ) {
            if includeCookies {
                HTTPCookieStorage.shared.removeCookies(since: Date.distantPast)
            }
            completion(["cleared": true])
        }
    }

    static func reload(webView: WKWebView, bypassCache: Bool) -> [String: Any] {
        if bypassCache {
            if let url = webView.url {
                var req = URLRequest(url: url)
                applyDocumentCachePolicy(&req)
                webView.load(req)
            } else {
                webView.reloadFromOrigin()
            }
        } else {
            webView.reload()
        }
        return ["reloaded": true, "bypassCache": bypassCache]
    }

    static func load(_ webView: WKWebView, url: URL, extra: [String: String]? = nil) {
        var req = URLRequest(url: url)
        extra?.forEach { req.setValue($1, forHTTPHeaderField: $0) }
        if shouldBypassDocument(url.absoluteString, policy: policy(for: webView)) {
            applyDocumentCachePolicy(&req)
        }
        webView.load(req)
    }

    static func shouldBypassDocumentRequest(_ request: URLRequest, webView: WKWebView) -> Bool {
        if request.value(forHTTPHeaderField: bypassHeader) != nil { return false }
        let policy = policy(for: webView)
        guard let url = request.url?.absoluteString else { return false }
        switch policy.mode {
        case modeNoCache: return true
        case modeSmart: return shouldBypassDocument(url, policy: policy)
        default: return false
        }
    }

    static func documentRequest(from request: URLRequest) -> URLRequest {
        var req = request
        applyDocumentCachePolicy(&req)
        return req
    }

    static func isStaticAsset(_ url: String) -> Bool {
        let path = url.split(separator: "?").first.map(String.init)?.lowercased() ?? url.lowercased()
        if path.contains("/_next/static/") { return true }
        if path.contains("/static/") { return true }
        if path.contains("/assets/") { return true }
        let ext = (path as NSString).pathExtension.lowercased()
        return !ext.isEmpty && staticExtensions.contains(ext)
    }

    static func shouldBypassDocument(_ url: String, policy: Policy) -> Bool {
        if policy.mode == modeNoCache { return true }
        if policy.mode != modeSmart { return false }
        if isStaticAsset(url) { return false }
        let path = url.split(separator: "?").first.map(String.init) ?? url
        if path.lowercased().hasSuffix(".html") { return true }
        return !(path as NSString).lastPathComponent.contains(".")
    }

    private static func applyDocumentCachePolicy(_ request: inout URLRequest) {
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.setValue("no-cache, no-store, must-revalidate", forHTTPHeaderField: "Cache-Control")
        request.setValue("no-cache", forHTTPHeaderField: "Pragma")
        request.setValue("1", forHTTPHeaderField: bypassHeader)
    }

    private static func documentHeaders() -> [String: String] {
        [
            "Cache-Control": "no-cache, no-store, must-revalidate",
            "Pragma": "no-cache",
            bypassHeader: "1",
        ]
    }

    private static func policy(for webView: WKWebView?) -> Policy {
        guard let wv = webView, let box = policies.object(forKey: wv) else { return globalPolicy }
        return box.value
    }

    private static let staticExtensions: Set<String> = [
        "js", "mjs", "css", "woff", "woff2", "ttf", "otf", "eot",
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "avif",
        "map", "json", "wasm", "br", "gz",
    ]
}
