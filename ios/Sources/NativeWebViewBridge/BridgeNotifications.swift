import Foundation
import UIKit
import UserNotifications

enum BridgeNotifications {

    static func getSettings(completion: @escaping ([String: Any]) -> Void) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let enabled = settings.authorizationStatus == .authorized
                || settings.authorizationStatus == .provisional
            UNUserNotificationCenter.current().getDeliveredNotifications { delivered in
                let active = delivered.map { n -> [String: Any] in
                    [
                        "id": n.request.identifier,
                        "title": n.request.content.title,
                        "body": n.request.content.body,
                    ]
                }
                DispatchQueue.main.async {
                    completion([
                        "enabled": enabled,
                        "authorizationStatus": authString(settings.authorizationStatus),
                        "activeCount": delivered.count,
                        "active": active,
                        "badge": UIApplication.shared.applicationIconBadgeNumber,
                    ])
                }
            }
        }
    }

    static func cancel(identifier: String) -> [String: Any] {
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [identifier])
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
        return ["cancelled": true, "id": identifier]
    }

    static func cancelAll() -> [String: Any] {
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        if #available(iOS 16.0, *) {
            UNUserNotificationCenter.current().setBadgeCount(0)
        } else {
            UIApplication.shared.applicationIconBadgeNumber = 0
        }
        return ["cancelledAll": true]
    }

    static func openSettings() -> [String: Any] {
        guard let url = URL(string: UIApplication.openSettingsURLString),
              UIApplication.shared.canOpenURL(url) else {
            return ["opened": false]
        }
        UIApplication.shared.open(url)
        return ["opened": true]
    }

    private static func authString(_ status: UNAuthorizationStatus) -> String {
        switch status {
        case .authorized: return "authorized"
        case .denied: return "denied"
        case .notDetermined: return "notDetermined"
        case .provisional: return "provisional"
        case .ephemeral: return "ephemeral"
        @unknown default: return "unknown"
        }
    }
}
