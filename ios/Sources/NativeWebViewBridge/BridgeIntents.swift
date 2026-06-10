import Foundation
import UIKit

enum BridgeIntents {

    static func canOpenUrl(_ url: String) -> [String: Any] {
        guard let u = URL(string: url) else {
            return ["url": url, "canOpen": false]
        }
        return ["url": url, "canOpen": UIApplication.shared.canOpenURL(u)]
    }

    static func openUrl(_ url: String) -> [String: Any] {
        guard let u = URL(string: url), UIApplication.shared.canOpenURL(u) else {
            return ["opened": false, "url": url, "error": "Cannot open URL"]
        }
        DispatchQueue.main.async {
            UIApplication.shared.open(u, options: [:], completionHandler: nil)
        }
        return ["opened": true, "url": url]
    }

    /**
     params: url | data, options (UIApplication.OpenExternalURLOptionsKey map)
     iOS has no generic Intent API — URLs and share sheets are the equivalent.
     */
    static func launchIntent(_ params: [String: Any]) -> [String: Any] {
        let urlString = params["url"] as? String
            ?? params["data"] as? String
            ?? ""
        guard !urlString.isEmpty else {
            return ["opened": false, "error": "url or data is required"]
        }
        guard let url = URL(string: urlString) else {
            return ["opened": false, "error": "Invalid URL", "url": urlString]
        }
        guard UIApplication.shared.canOpenURL(url) else {
            return ["opened": false, "url": urlString, "error": "No app can handle this URL"]
        }

        var options: [UIApplication.OpenExternalURLOptionsKey: Any] = [:]
        if let raw = params["options"] as? [String: Any] {
            if let universal = raw["universalLinksOnly"] as? Bool {
                options[.universalLinksOnly] = universal
            }
        }

        DispatchQueue.main.async {
            UIApplication.shared.open(url, options: options, completionHandler: nil)
        }
        return ["opened": true, "url": urlString]
    }

    /** On iOS, returns whether the URL can be opened (and known scheme probes). */
    static func queryIntents(_ params: [String: Any]) -> [[String: Any]] {
        if let urlString = params["data"] as? String ?? params["url"] as? String,
           let url = URL(string: urlString) {
            let can = UIApplication.shared.canOpenURL(url)
            return [[
                "url": urlString,
                "scheme": url.scheme ?? "",
                "canOpen": can,
                "name": url.scheme ?? "handler",
            ]]
        }

        if let schemes = params["schemes"] as? [String] {
            return schemes.compactMap { scheme in
                guard let url = URL(string: "\(scheme)://") else { return nil }
                return [
                    "scheme": scheme,
                    "canOpen": UIApplication.shared.canOpenURL(url),
                    "name": scheme,
                ] as [String: Any]
            }
        }

        return BridgePaymentApps.getUpiApps().map { app in
            [
                "scheme": app["scheme"] as? String ?? "",
                "name": app["name"] as? String ?? "",
                "canOpen": app["installed"] as? Bool ?? false,
                "category": app["category"] as? String ?? "",
            ] as [String: Any]
        }
    }
}
