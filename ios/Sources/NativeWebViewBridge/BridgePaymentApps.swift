import Foundation
import UIKit

enum BridgePaymentApps {

    struct AppDef {
        let id: String
        let name: String
        let scheme: String
        let category: String
    }

    /// Known UPI / wallet apps on iOS (checked via canOpenURL).
    private static let knownApps: [AppDef] = [
        AppDef(id: "phonepe", name: "PhonePe", scheme: "phonepe", category: "upi"),
        AppDef(id: "gpay", name: "Google Pay", scheme: "tez", category: "upi"),
        AppDef(id: "paytm", name: "Paytm", scheme: "paytmmp", category: "upi"),
        AppDef(id: "bhim", name: "BHIM UPI", scheme: "bhim", category: "upi"),
        AppDef(id: "amazonpay", name: "Amazon Pay", scheme: "amazonpay", category: "wallet"),
        AppDef(id: "mobikwik", name: "MobiKwik", scheme: "mobikwik", category: "wallet"),
        AppDef(id: "freecharge", name: "Freecharge", scheme: "freecharge", category: "wallet"),
        AppDef(id: "cred", name: "CRED", scheme: "cred", category: "wallet"),
        AppDef(id: "whatsapp", name: "WhatsApp", scheme: "whatsapp", category: "messaging"),
    ]

    static func getUpiApps() -> [[String: Any]] {
        knownApps
            .filter { $0.category == "upi" && canOpen(scheme: $0.scheme) }
            .map { appToDict($0, installed: true) }
    }

    static func getPaymentApps() -> [String: Any] {
        let upi = getUpiApps()
        var wallets: [[String: Any]] = []
        var messaging: [[String: Any]] = []
        var all: [[String: Any]] = []

        for def in knownApps {
            let installed = canOpen(scheme: def.scheme)
            let dict = appToDict(def, installed: installed)
            all.append(dict)
            if installed {
                switch def.category {
                case "wallet": wallets.append(dict)
                case "messaging": messaging.append(dict)
                default: break
                }
            }
        }

        // Add UPI apps not in known list (already in all if installed)
        for u in upi {
            if !all.contains(where: { ($0["id"] as? String) == (u["id"] as? String) }) {
                all.append(u)
            }
        }

        return [
            "upi": upi,
            "wallets": wallets,
            "messaging": messaging,
            "all": all,
            "platform": "ios",
        ]
    }

    static func canOpenUrl(_ url: String) -> [String: Any] {
        BridgeIntents.canOpenUrl(url)
    }

    static func openUrl(_ url: String) -> [String: Any] {
        BridgeIntents.openUrl(url)
    }

    /// params: vpa, amount, name, note, txnId, currency (default INR)
    static func openUpiPayment(_ params: [String: Any]) -> [String: Any] {
        let vpa = params["vpa"] as? String ?? params["pa"] as? String ?? ""
        guard !vpa.isEmpty else {
            return ["opened": false, "error": "vpa (payee address) is required"]
        }
        let uri = buildUpiUri(params)
        guard let url = URL(string: uri), UIApplication.shared.canOpenURL(url) else {
            return ["opened": false, "uri": uri, "error": "No UPI app available", "availableApps": getUpiApps()]
        }
        DispatchQueue.main.async {
            UIApplication.shared.open(url)
        }
        return ["opened": true, "uri": uri, "availableApps": getUpiApps()]
    }

    static func buildUpiUri(_ params: [String: Any]) -> String {
        var c = URLComponents()
        c.scheme = "upi"
        c.host = "pay"
        var items: [URLQueryItem] = [
            URLQueryItem(name: "pa", value: params["vpa"] as? String ?? params["pa"] as? String),
        ]
        if let name = params["name"] as? String ?? params["pn"] as? String, !name.isEmpty {
            items.append(URLQueryItem(name: "pn", value: name))
        }
        if let amount = params["amount"] as? String ?? params["am"] as? String, !amount.isEmpty {
            items.append(URLQueryItem(name: "am", value: amount))
        }
        items.append(URLQueryItem(
            name: "cu",
            value: params["currency"] as? String ?? params["cu"] as? String ?? "INR"
        ))
        if let note = params["note"] as? String ?? params["tn"] as? String, !note.isEmpty {
            items.append(URLQueryItem(name: "tn", value: note))
        }
        if let txn = params["txnId"] as? String ?? params["tr"] as? String, !txn.isEmpty {
            items.append(URLQueryItem(name: "tr", value: txn))
        }
        c.queryItems = items
        return c.url?.absoluteString ?? "upi://pay"
    }

    private static func canOpen(scheme: String) -> Bool {
        guard let url = URL(string: "\(scheme)://") else { return false }
        return UIApplication.shared.canOpenURL(url)
    }

    private static func appToDict(_ def: AppDef, installed: Bool) -> [String: Any] {
        [
            "id": def.id,
            "name": def.name,
            "scheme": def.scheme,
            "category": def.category,
            "installed": installed,
            "packageName": def.scheme, // iOS has no package; use scheme as id
        ]
    }
}
