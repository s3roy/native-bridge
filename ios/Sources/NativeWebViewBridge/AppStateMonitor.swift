import Foundation
import Network
#if canImport(UIKit)
import UIKit
import AVKit
import AVFoundation
#endif
#if canImport(CallKit)
import CallKit
#endif

#if canImport(UIKit)

struct NetworkState {
    let connected: Bool
    let type: String
    func toDictionary() -> [String: Any] { ["connected": connected, "type": type] }
}

struct WebViewState {
    let visible: Bool
    let focused: Bool
    let url: String?
    func toDictionary() -> [String: Any] {
        ["visible": visible, "focused": focused, "url": url as Any]
    }
}

struct KeyboardState {
    let visible: Bool
    let height: Int
    func toDictionary() -> [String: Any] { ["visible": visible, "height": height] }
}

struct SafeAreaInsets {
    let top: CGFloat
    let bottom: CGFloat
    let left: CGFloat
    let right: CGFloat
    func toDictionary() -> [String: Any] {
        ["top": Int(top), "bottom": Int(bottom), "left": Int(left), "right": Int(right)]
    }
}

struct BatteryState {
    let level: Float
    let charging: Bool
    let lowPowerMode: Bool
    func toDictionary() -> [String: Any] {
        ["level": Double(level), "charging": charging, "lowPowerMode": lowPowerMode]
    }
}

struct AudioRouteState {
    let route: String
    let bluetoothConnected: Bool
    let deviceName: String?
    func toDictionary() -> [String: Any] {
        ["route": route, "bluetoothConnected": bluetoothConnected, "deviceName": deviceName as Any]
    }
}

struct DisplayState {
    let width: Int
    let height: Int
    let scale: CGFloat
    func toDictionary() -> [String: Any] {
        ["width": width, "height": height, "scale": Double(scale)]
    }
}

struct ThemeState {
    let darkMode: Bool
    func toDictionary() -> [String: Any] { ["darkMode": darkMode] }
}

struct LocaleState {
    let language: String
    let region: String
    let timezone: String
    func toDictionary() -> [String: Any] {
        ["language": language, "region": region, "timezone": timezone]
    }
}

struct CallState {
    let inCall: Bool
    let state: String
    func toDictionary() -> [String: Any] { ["inCall": inCall, "state": state] }
}

struct AppStateSnapshot {
    let lifecycle: String
    let isForeground: Bool
    let isInPiP: Bool
    let network: NetworkState
    let orientation: String
    let webView: WebViewState?
    let keyboard: KeyboardState
    let safeArea: SafeAreaInsets
    let battery: BatteryState
    let audio: AudioRouteState
    let display: DisplayState
    let theme: ThemeState
    let locale: LocaleState
    let call: CallState
    let timestamp: Int
    let changed: String?

    func toDictionary() -> [String: Any] {
        var d: [String: Any] = [
            "lifecycle": lifecycle,
            "isForeground": isForeground,
            "isInPiP": isInPiP,
            "network": network.toDictionary(),
            "orientation": orientation,
            "keyboard": keyboard.toDictionary(),
            "safeArea": safeArea.toDictionary(),
            "battery": battery.toDictionary(),
            "audio": audio.toDictionary(),
            "display": display.toDictionary(),
            "theme": theme.toDictionary(),
            "locale": locale.toDictionary(),
            "call": call.toDictionary(),
            "timestamp": timestamp,
        ]
        d["webView"] = webView?.toDictionary()
        d["changed"] = changed
        return d
    }
}

/// Tracks full device/app context and pushes realtime updates to web.
enum AppStateMonitor {

    private static var started = false
    private static var lifecycle = "active"
    private static var isForeground = true
    private static var isInPiP = false
    private static var network = NetworkState(connected: true, type: "unknown")
    private static var orientation = "unknown"
    private static var webViewVisible = true
    private static var webViewFocused = false
    private static var webViewUrl: String?
    private static var keyboard = KeyboardState(visible: false, height: 0)
    private static var safeArea = SafeAreaInsets(top: 0, bottom: 0, left: 0, right: 0)
    private static var battery = BatteryState(level: 1, charging: false, lowPowerMode: false)
    private static var audio = AudioRouteState(route: "unknown", bluetoothConnected: false, deviceName: nil)
    private static var display = DisplayState(width: 0, height: 0, scale: 1)
    private static var theme = ThemeState(darkMode: false)
    private static var locale = LocaleState(language: "en", region: "", timezone: TimeZone.current.identifier)
    private static var call = CallState(inCall: false, state: "idle")
    private static var pathMonitor: NWPathMonitor?
    #if canImport(CallKit)
    private static let callObserver = CXCallObserver()
    #endif

    static func enable() {
        guard !started else { return }
        started = true

        orientation = readOrientation()
        display = readDisplay()
        theme = readTheme()
        locale = readLocale()
        refreshSafeArea()
        refreshBattery()
        refreshAudio()

        let center = NotificationCenter.default

        center.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { _ in
            lifecycle = "active"; isForeground = true; publish("lifecycle")
        }
        center.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { _ in
            lifecycle = "inactive"; publish("lifecycle")
        }
        center.addObserver(forName: UIApplication.didEnterBackgroundNotification, object: nil, queue: .main) { _ in
            lifecycle = "background"; isForeground = false; publish("lifecycle")
        }
        center.addObserver(forName: UIApplication.willEnterForegroundNotification, object: nil, queue: .main) { _ in
            isForeground = true; publish("lifecycle")
        }
        center.addObserver(forName: UIDevice.orientationDidChangeNotification, object: nil, queue: .main) { _ in
            let next = readOrientation()
            guard next != orientation else { return }
            orientation = next; display = readDisplay(); publish("orientation"); publish("display")
        }
        center.addObserver(forName: UIApplication.didChangeStatusBarOrientationNotification, object: nil, queue: .main) { _ in
            refreshSafeArea(); display = readDisplay(); publish("display")
        }

        center.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { n in
            let h = keyboardHeight(from: n)
            keyboard = KeyboardState(visible: true, height: h); publish("keyboard")
        }
        center.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { _ in
            keyboard = KeyboardState(visible: false, height: 0); publish("keyboard")
        }
        center.addObserver(forName: UIResponder.keyboardWillChangeFrameNotification, object: nil, queue: .main) { n in
            let h = keyboardHeight(from: n)
            let visible = h > 0
            keyboard = KeyboardState(visible: visible, height: visible ? h : 0); publish("keyboard")
        }

        center.addObserver(forName: AVPictureInPictureController.willStartPictureInPictureNotification, object: nil, queue: .main) { _ in
            isInPiP = true; publish("pip")
        }
        center.addObserver(forName: AVPictureInPictureController.didStopPictureInPictureNotification, object: nil, queue: .main) { _ in
            isInPiP = false; publish("pip")
        }

        center.addObserver(forName: UIDevice.batteryLevelDidChangeNotification, object: nil, queue: .main) { _ in
            refreshBattery()
        }
        center.addObserver(forName: UIDevice.batteryStateDidChangeNotification, object: nil, queue: .main) { _ in
            refreshBattery()
        }
        center.addObserver(forName: .NSProcessInfoPowerStateDidChange, object: nil, queue: .main) { _ in
            refreshBattery()
        }

        center.addObserver(forName: AVAudioSession.routeChangeNotification, object: nil, queue: .main) { _ in
            refreshAudio()
        }

        center.addObserver(forName: UIApplication.didReceiveMemoryWarningNotification, object: nil, queue: .main) { _ in
            NativeBridge.shared.publishEvent("app.memory", ["lowMemory": true])
        }

        UIDevice.current.isBatteryMonitoringEnabled = true

        let monitor = NWPathMonitor()
        pathMonitor = monitor
        monitor.pathUpdateHandler = { path in
            DispatchQueue.main.async {
                let connected = path.status == .satisfied
                let type: String
                if !connected { type = "none" }
                else if path.usesInterfaceType(.wifi) { type = "wifi" }
                else if path.usesInterfaceType(.cellular) { type = "cellular" }
                else if path.usesInterfaceType(.wiredEthernet) { type = "ethernet" }
                else { type = "unknown" }
                network = NetworkState(connected: connected, type: type)
                publish("network")
            }
        }
        monitor.start(queue: DispatchQueue(label: "com.bridge.network"))

        #if canImport(CallKit)
        callObserver.setDelegate(CallObserverDelegate.shared, queue: .main)
        #endif

        publish("init")
    }

    static func setWebViewVisible(_ visible: Bool) {
        guard webViewVisible != visible else { return }
        webViewVisible = visible; publish("webview")
    }

    static func setWebViewFocused(_ focused: Bool) {
        guard webViewFocused != focused else { return }
        webViewFocused = focused; publish("webview")
    }

    static func setWebViewUrl(_ url: String?) {
        guard webViewUrl != url else { return }
        webViewUrl = url; publish("webview")
    }

    static func refreshSafeArea(from view: UIView? = nil) {
        let window = view?.window ?? UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        let insets = window?.safeAreaInsets ?? .zero
        let next = SafeAreaInsets(top: insets.top, bottom: insets.bottom, left: insets.left, right: insets.right)
        guard next.top != safeArea.top || next.bottom != safeArea.bottom ||
              next.left != safeArea.left || next.right != safeArea.right else { return }
        safeArea = next
        NativeBridge.shared.injectSafeAreaCss(next)
        publish("safeArea")
    }

    static func safeAreaDictionary() -> [String: Any] { safeArea.toDictionary() }

    static func safeAreaInsetsValue() -> SafeAreaInsets { safeArea }

    static func snapshot(changed: String? = nil) -> AppStateSnapshot {
        AppStateSnapshot(
            lifecycle: lifecycle,
            isForeground: isForeground,
            isInPiP: isInPiP,
            network: network,
            orientation: orientation,
            webView: WebViewState(visible: webViewVisible, focused: webViewFocused, url: webViewUrl),
            keyboard: keyboard,
            safeArea: safeArea,
            battery: battery,
            audio: audio,
            display: display,
            theme: theme,
            locale: locale,
            call: call,
            timestamp: Int(Date().timeIntervalSince1970 * 1000),
            changed: changed
        )
    }

    static func currentDictionary() -> [String: Any] { snapshot().toDictionary() }

    static func updateCall(inCall: Bool, state: String) {
        let next = CallState(inCall: inCall, state: state)
        guard next.inCall != call.inCall || next.state != call.state else { return }
        call = next; publish("call")
    }

    private static func publish(_ changed: String) {
        let snap = snapshot(changed: changed)
        NativeBridge.shared.emit("app.state", snap.toDictionary())
        switch changed {
        case "lifecycle", "init":
            NativeBridge.shared.emit("app.lifecycle", [
                "lifecycle": snap.lifecycle, "isForeground": snap.isForeground, "timestamp": snap.timestamp,
            ])
        case "pip":
            NativeBridge.shared.emit("app.pip", ["isInPiP": snap.isInPiP, "timestamp": snap.timestamp])
        case "network", "init": NativeBridge.shared.emit("app.network", snap.network.toDictionary())
        case "orientation", "init":
            NativeBridge.shared.emit("app.orientation", ["orientation": snap.orientation, "timestamp": snap.timestamp])
        case "webview", "init":
            if let wv = snap.webView?.toDictionary() { NativeBridge.shared.emit("app.webview", wv) }
        case "keyboard", "init": NativeBridge.shared.emit("app.keyboard", snap.keyboard.toDictionary())
        case "safeArea", "init": NativeBridge.shared.emit("app.safeArea", snap.safeArea.toDictionary())
        case "battery", "init": NativeBridge.shared.emit("app.battery", snap.battery.toDictionary())
        case "audio", "init": NativeBridge.shared.emit("app.audio", snap.audio.toDictionary())
        case "display", "init": NativeBridge.shared.emit("app.display", snap.display.toDictionary())
        case "theme", "init": NativeBridge.shared.emit("app.theme", snap.theme.toDictionary())
        case "locale", "init": NativeBridge.shared.emit("app.locale", snap.locale.toDictionary())
        case "call", "init": NativeBridge.shared.emit("app.call", snap.call.toDictionary())
        default: break
        }
    }

    private static func refreshBattery() {
        let level = UIDevice.current.batteryLevel < 0 ? 1 : UIDevice.current.batteryLevel
        let charging: Bool
        switch UIDevice.current.batteryState {
        case .charging, .full: charging = true
        default: charging = false
        }
        let low = ProcessInfo.processInfo.isLowPowerModeEnabled
        let next = BatteryState(level: level, charging: charging, lowPowerMode: low)
        guard next.level != battery.level || next.charging != battery.charging || next.lowPowerMode != battery.lowPowerMode else { return }
        battery = next; publish("battery")
    }

    private static func refreshAudio() {
        let session = AVAudioSession.sharedInstance()
        var route = "speaker"
        var bt = false
        var name: String?
        for output in session.currentRoute.outputs {
            switch output.portType {
            case .bluetoothA2DP, .bluetoothHFP, .bluetoothLE:
                route = "bluetooth"; bt = true; name = output.portName
            case .headphones, .headsetMic:
                route = "wired"; name = output.portName
            case .builtInReceiver:
                route = "earpiece"
            case .builtInSpeaker:
                route = "speaker"
            default:
                break
            }
        }
        let next = AudioRouteState(route: route, bluetoothConnected: bt, deviceName: name)
        guard next.route != audio.route || next.bluetoothConnected != audio.bluetoothConnected else { return }
        audio = next; publish("audio")
    }

    private static func keyboardHeight(from notification: Notification) -> Int {
        guard let frame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return 0 }
        return Int(frame.height)
    }

    private static func readOrientation() -> String {
        switch UIDevice.current.orientation {
        case .portrait, .portraitUpsideDown: return "portrait"
        case .landscapeLeft, .landscapeRight: return "landscape"
        default:
            switch UIApplication.shared.statusBarOrientation {
            case .portrait, .portraitUpsideDown: return "portrait"
            case .landscapeLeft, .landscapeRight: return "landscape"
            default: return "unknown"
            }
        }
    }

    private static func readDisplay() -> DisplayState {
        let bounds = UIScreen.main.bounds
        return DisplayState(width: Int(bounds.width), height: Int(bounds.height), scale: UIScreen.main.scale)
    }

    private static func readTheme() -> ThemeState {
        let style = UITraitCollection.current.userInterfaceStyle
        return ThemeState(darkMode: style == .dark)
    }

    private static func readLocale() -> LocaleState {
        let l = Locale.current
        return LocaleState(
            language: l.languageCode ?? l.identifier,
            region: l.regionCode ?? "",
            timezone: TimeZone.current.identifier
        )
    }
}

#if canImport(CallKit)
private final class CallObserverDelegate: NSObject, CXCallObserverDelegate {
    static let shared = CallObserverDelegate()
    func callObserver(_ callObserver: CXCallObserver, callChanged call: CXCall) {
        let state: String
        if call.hasEnded { state = "idle" }
        else if call.isOutgoing && !call.hasConnected { state = "ringing" }
        else if call.hasConnected { state = "active" }
        else { state = "ringing" }
        AppStateMonitor.updateCall(inCall: !call.hasEnded, state: state)
    }
}
#endif

#endif
