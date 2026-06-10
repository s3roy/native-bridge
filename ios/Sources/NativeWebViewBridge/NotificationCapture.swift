import Foundation
import UserNotifications

/// Auto-captures notifications by swizzling the UNUserNotificationCenterDelegate
/// callbacks. No host-app code required beyond NativeBridge.start().
enum NotificationCapture {

    static func enable() {
        DispatchQueue.main.async {
            swizzleDelegateSetter()
            // If a delegate is already set, swizzle it now.
            if let delegate = UNUserNotificationCenter.current().delegate {
                swizzle(delegate: type(of: delegate))
            }
        }
    }

    private static func swizzleDelegateSetter() {
        let cls: AnyClass = UNUserNotificationCenter.self
        guard
            let original = class_getInstanceMethod(cls, #selector(setter: UNUserNotificationCenter.delegate)),
            let swizzled = class_getInstanceMethod(cls, #selector(UNUserNotificationCenter.bridge_setDelegate(_:)))
        else { return }
        method_exchangeImplementations(original, swizzled)
    }

    static func swizzle(delegate cls: AnyClass) {
        let willPresent = #selector(
            UNUserNotificationCenterDelegate.userNotificationCenter(_:willPresent:withCompletionHandler:)
        )
        let didReceive = #selector(
            UNUserNotificationCenterDelegate.userNotificationCenter(_:didReceive:withCompletionHandler:)
        )
        inject(cls, selector: willPresent, swizzled: #selector(NSObject.bridge_willPresent(_:_:_:)))
        inject(cls, selector: didReceive, swizzled: #selector(NSObject.bridge_didReceive(_:_:_:)))
    }

    private static func inject(_ cls: AnyClass, selector: Selector, swizzled: Selector) {
        guard let swizzledMethod = class_getInstanceMethod(NSObject.self, swizzled) else { return }
        if let original = class_getInstanceMethod(cls, selector) {
            method_exchangeImplementations(original, swizzledMethod)
        } else {
            class_addMethod(cls, selector,
                            method_getImplementation(swizzledMethod),
                            method_getTypeEncoding(swizzledMethod))
        }
    }

    static func capture(_ notification: UNNotification) {
        let content = notification.request.content
        var data: [String: String] = [:]
        for (k, v) in content.userInfo {
            if let key = k as? String { data[key] = "\(v)" }
        }
        NativeBridge.shared.recordNotification(
            NotificationRecord(
                id: notification.request.identifier,
                title: content.title.isEmpty ? nil : content.title,
                body: content.body.isEmpty ? nil : content.body,
                data: data,
                timestamp: Int(Date().timeIntervalSince1970 * 1000)
            )
        )
    }
}

extension UNUserNotificationCenter {
    @objc func bridge_setDelegate(_ delegate: UNUserNotificationCenterDelegate?) {
        if let delegate = delegate {
            NotificationCapture.swizzle(delegate: type(of: delegate))
        }
        bridge_setDelegate(delegate) // calls original after swizzle
    }
}

extension NSObject {
    @objc func bridge_willPresent(
        _ center: UNUserNotificationCenter,
        _ notification: UNNotification,
        _ completion: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        NotificationCapture.capture(notification)
        // call original if it existed (exchanged into this selector)
        if responds(to: #selector(bridge_willPresent(_:_:_:))) {
            bridge_willPresent(center, notification, completion)
        } else {
            completion([])
        }
    }

    @objc func bridge_didReceive(
        _ center: UNUserNotificationCenter,
        _ response: UNNotificationResponse,
        _ completion: @escaping () -> Void
    ) {
        NotificationCapture.capture(response.notification)
        if responds(to: #selector(bridge_didReceive(_:_:_:))) {
            bridge_didReceive(center, response, completion)
        } else {
            completion()
        }
    }
}
