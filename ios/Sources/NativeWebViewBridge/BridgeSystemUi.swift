import Foundation
import UIKit

enum BridgeSystemUi {

    private static var config: [String: Any] = [:]

    static func get() -> [String: Any] {
        var out = config
        out["safeArea"] = AppStateMonitor.safeAreaDictionary()
        return out
    }

    @discardableResult
    static func set(_ params: [String: Any]) -> [String: Any] {
        if let hidden = params["statusBarHidden"] as? Bool {
            config["statusBarHidden"] = hidden
            applyStatusBarHidden(hidden)
        }

        if let style = params["statusBarStyle"] as? String {
            config["statusBarStyle"] = style
            applyStatusBarStyle(style)
        }

        if let hidden = params["homeIndicatorHidden"] as? Bool {
            config["homeIndicatorHidden"] = hidden
        }

        if let on = params["keepScreenOn"] as? Bool {
            UIApplication.shared.isIdleTimerDisabled = on
            config["keepScreenOn"] = on
        }

        return get()
    }

    private static func applyStatusBarHidden(_ hidden: Bool) {
        guard let window = keyWindow() else { return }
        if #available(iOS 13.0, *) {
            window.windowScene?.statusBarManager // read-only; use VC update
        }
        topViewController()?.setNeedsStatusBarAppearanceUpdate()
        if !hidden {
            // no-op — host VC should read config via getSystemUi
        }
    }

    private static func applyStatusBarStyle(_ style: String) {
        topViewController()?.setNeedsStatusBarAppearanceUpdate()
        config["statusBarStyle"] = style
    }

    static func preferredStatusBarStyle() -> UIStatusBarStyle {
        switch config["statusBarStyle"] as? String {
        case "light", "light-content": return .lightContent
        case "dark", "dark-content": return .darkContent
        default: return .default
        }
    }

    static func prefersStatusBarHidden() -> Bool {
        config["statusBarHidden"] as? Bool ?? false
    }

    private static func keyWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
    }

    private static func topViewController() -> UIViewController? {
        guard let root = keyWindow()?.rootViewController else { return nil }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}
