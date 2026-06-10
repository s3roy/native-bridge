import Foundation
import WebKit

enum BridgeCookies {

    static func setCookie(webView: WKWebView, params: [String: Any]) throws -> [String: Any] {
        let url = try resolveUrl(webView: webView, params: params)
        guard let name = params["name"] as? String, !name.isEmpty else {
            throw NSError(domain: "BridgeCookies", code: 1, userInfo: [NSLocalizedDescriptionKey: "name required"])
        }
        let value = params["value"] as? String ?? ""
        var props: [HTTPCookiePropertyKey: Any] = [
            .name: name,
            .value: value,
            .domain: (params["domain"] as? String) ?? (url.host ?? ""),
            .path: (params["path"] as? String) ?? "/",
        ]
        if let maxAge = params["maxAge"] as? Int {
            props[.maximumAge] = maxAge
        }
        if params["secure"] as? Bool == true {
            props[.secure] = "TRUE"
        }
        guard let cookie = HTTPCookie(properties: props) else {
            throw NSError(domain: "BridgeCookies", code: 2, userInfo: [NSLocalizedDescriptionKey: "invalid cookie"])
        }
        let sema = DispatchSemaphore(value: 0)
        webView.configuration.websiteDataStore.httpCookieStore.setCookie(cookie) {
            sema.signal()
        }
        sema.wait()
        return ["set": true, "name": name, "url": url.absoluteString]
    }

    static func setCookies(webView: WKWebView, params: [String: Any]) throws -> [String: Any] {
        let list = params["cookies"] as? [[String: Any]] ?? []
        var count = 0
        for c in list {
            _ = try setCookie(webView: webView, params: c)
            count += 1
        }
        return ["set": count]
    }

    static func getCookies(webView: WKWebView, params: [String: Any], completion: @escaping ([String: Any]) -> Void) {
        let urlString = (params["url"] as? String).flatMap { $0.isEmpty ? nil : $0 }
            ?? webView.url?.absoluteString
            ?? ""
        webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { all in
            let host = URL(string: urlString)?.host
            let filtered = all.filter { cookie in
                guard !urlString.isEmpty else { return true }
                if let h = host {
                    return cookie.domain.contains(h) || h.contains(cookie.domain.trimmingCharacters(in: CharacterSet(charactersIn: ".")))
                }
                return true
            }
            let cookies = filtered.map { c -> [String: Any] in
                [
                    "name": c.name,
                    "value": c.value,
                    "domain": c.domain,
                    "path": c.path,
                    "secure": c.isSecure,
                    "httpOnly": c.isHTTPOnly,
                ]
            }
            completion(["url": urlString, "cookies": cookies])
        }
    }

    static func removeCookie(webView: WKWebView, params: [String: Any], completion: @escaping ([String: Any]) -> Void) {
        guard let name = params["name"] as? String, !name.isEmpty else {
            completion(["removed": false, "error": "name required"])
            return
        }
        let store = webView.configuration.websiteDataStore.httpCookieStore
        store.getAllCookies { all in
            let matches = all.filter { $0.name == name }
            let group = DispatchGroup()
            for cookie in matches {
                group.enter()
                store.delete(cookie) { group.leave() }
            }
            group.notify(queue: .main) {
                completion(["removed": true, "name": name, "count": matches.count])
            }
        }
    }

    static func clearCookies(webView: WKWebView, params: [String: Any], completion: @escaping ([String: Any]) -> Void) {
        let urlFilter = (params["url"] as? String).flatMap { $0.isEmpty ? nil : $0 }
        let store = webView.configuration.websiteDataStore.httpCookieStore
        store.getAllCookies { all in
            let host = urlFilter.flatMap { URL(string: $0)?.host }
            let targets: [HTTPCookie]
            if let h = host {
                targets = all.filter {
                    $0.domain.contains(h) || h.contains($0.domain.trimmingCharacters(in: CharacterSet(charactersIn: ".")))
                }
            } else {
                targets = all
            }
            let group = DispatchGroup()
            for cookie in targets {
                group.enter()
                store.delete(cookie) { group.leave() }
            }
            group.notify(queue: .main) {
                if urlFilter == nil {
                    completion(["clearedAll": true, "count": targets.count])
                } else {
                    completion(["cleared": targets.count, "url": urlFilter!])
                }
            }
        }
    }

    private static func resolveUrl(webView: WKWebView, params: [String: Any]) throws -> URL {
        if let s = params["url"] as? String, !s.isEmpty, let u = URL(string: s) { return u }
        if let u = webView.url { return u }
        throw NSError(domain: "BridgeCookies", code: 3, userInfo: [NSLocalizedDescriptionKey: "url required"])
    }
}
