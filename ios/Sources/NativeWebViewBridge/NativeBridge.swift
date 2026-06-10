import Foundation
import WebKit

/// Central, app-wide bridge singleton. Owns capture stores and fans data out to
/// every attached WKWebView, so the host app writes essentially no glue code.
public final class NativeBridge: NSObject {

    public static let shared = NativeBridge()

    private let apiCalls = RingBuffer<ApiCall>(max: 200)
    private let notifications = RingBuffer<NotificationRecord>(max: 100)
    private var dataStore: [String: Any] = [:]
    private let dataLock = NSLock()
    private var attached = NSHashTable<WKWebView>.weakObjects()
    private var webViewIds = NSMapTable<WKWebView, NSString>.weakToStrongObjects()
    private var webViewSeq = 0
    private var globalWebEventHandler: ((String, Any?, String, WKWebView) -> Void)?
    private var perWebViewEventHandlers = NSMapTable<WKWebView, AnyObject>.weakToStrongObjects()
    private var emitListeners: [(String, Any?) -> Void] = []
    private let emitLock = NSLock()

    public typealias WebEventHandler = (String, Any?, String, WKWebView) -> Void
    public typealias EmitListener = (String, Any?) -> Void

    private override init() { super.init() }

    /// Call once in application(_:didFinishLaunchingWithOptions:).
    /// Enables automatic network + notification capture.
    public static func start() {
        BridgeURLProtocol.enable()
        NotificationCapture.enable()
        AppStateMonitor.enable()
    }

    // MARK: - Host app helpers

    /// Publish a value the web side can read via NativeBridge.getData(key).
    public func putData(_ key: String, _ value: Any) {
        setData(key, value, source: "native")
    }

    /// Shared store — all attached WebViews receive `onData` with optional `source` id.
    func setData(_ key: String, _ value: Any?, source: String?) {
        guard !key.isEmpty else { return }
        dataLock.lock(); dataStore[key] = value ?? NSNull(); dataLock.unlock()
        var payload: [String: Any] = ["key": key, "value": value ?? NSNull()]
        if let source = source { payload["source"] = source }
        emit("data", payload)
    }

    func removeData(_ key: String, source: String?) {
        guard !key.isEmpty else { return }
        dataLock.lock(); dataStore.removeValue(forKey: key); dataLock.unlock()
        var payload: [String: Any] = ["key": key, "value": NSNull(), "removed": true]
        if let source = source { payload["source"] = source }
        emit("data", payload)
    }

    func webViewId(for webView: WKWebView) -> String {
        if let existing = webViewIds.object(forKey: webView) as String? { return existing }
        webViewSeq += 1
        let id = "wv_\(webViewSeq)"
        webViewIds.setObject(id as NSString, forKey: webView)
        return id
    }

    // MARK: - WebView wiring

    /// Attach to any WKWebView. The drop-in `BridgeWebView` does this for you.
    public func attach(_ webView: WKWebView) {
        let controller = webView.configuration.userContentController
        controller.removeScriptMessageHandler(forName: "iosBridge")
        controller.add(self, name: "iosBridge")

        // Inject the SDK at document start for every navigation — no web changes.
        let alreadyInjected = controller.userScripts.contains {
            $0.source.contains("__installed")
        }
        if !alreadyInjected {
            let script = WKUserScript(
                source: BridgeScript.js,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: false
            )
            controller.addUserScript(script)
        }
        attached.add(webView)
        _ = webViewId(for: webView)
        emit("app.state", AppStateMonitor.currentDictionary())
        injectSafeAreaCss(AppStateMonitor.safeAreaInsetsValue())
    }

    func injectSafeAreaCss(_ insets: SafeAreaInsets) {
        for webView in attached.allObjects {
            BridgeSafeArea.injectCss(into: webView, insets: insets)
        }
    }

    /// Push any custom realtime event to all attached WebViews (e.g. chat typing, call state).
    public func publishEvent(_ event: String, _ payload: Any) {
        emit(event, payload)
    }

    /// Handle every `NativeBridge.send(event, payload)` from web (app-wide).
    public func setWebEventHandler(_ handler: WebEventHandler?) {
        globalWebEventHandler = handler
    }

    func setWebViewEventHandler(
        _ webView: WKWebView,
        handler: ((String, Any?) -> Void)?
    ) {
        if let handler = handler {
            perWebViewEventHandlers.setObject(handler as AnyObject, forKey: webView)
        } else {
            perWebViewEventHandlers.removeObject(forKey: webView)
        }
    }

    private func deliverWebEvent(
        event: String,
        payload: Any?,
        webViewId: String,
        webView: WKWebView?
    ) {
        guard let webView = webView else { return }
        globalWebEventHandler?(event, payload, webViewId, webView)
        if let perView = perWebViewEventHandlers.object(forKey: webView) as? ((String, Any?) -> Void) {
            perView(event, payload)
        }
    }

    // MARK: - Outbound events

      public func addEmitListener(_ listener: @escaping EmitListener) {
        emitLock.lock()
        emitListeners.append(listener)
        emitLock.unlock()
    }

    public func removeEmitListener(_ listener: @escaping EmitListener) {
        emitLock.lock()
        emitListeners.removeAll { $0 as AnyObject === listener as AnyObject }
        emitLock.unlock()
    }

    public func emit(_ event: String, _ payload: Any) {
        let js = "window.NativeBridge && window.NativeBridge.__emit(\(jsonString(event)), \(jsonString(payload)))"
        runJS(js)
        emitLock.lock()
        let listeners = emitListeners
        emitLock.unlock()
        listeners.forEach { $0(event, payload) }
    }

    func recordApiCall(_ call: ApiCall) {
        apiCalls.add(call)
        emit("api.call", call.toDictionary())
    }

    public func recordNotification(_ record: NotificationRecord) {
        notifications.add(record)
        emit("notification", record.toDictionary())
    }

    // MARK: - Inbound requests (WKScriptMessageHandler)

    private func dispatch(method: String, params: [String: Any], sourceWebViewId: String?) throws -> Any? {
        switch method {
        case "bridge.getApiCalls":
            return filterApiCalls(params).map { $0.toDictionary() }
        case "bridge.getNotifications":
            return notifications.all().map { $0.toDictionary() }
        case "bridge.getData":
            let key = params["key"] as? String ?? ""
            dataLock.lock(); defer { dataLock.unlock() }
            return dataStore[key]
        case "bridge.getAllData":
            dataLock.lock(); defer { dataLock.unlock() }
            return dataStore
        case "bridge.setData":
            let key = params["key"] as? String ?? ""
            setData(key, params["value"], source: sourceWebViewId)
            return ["key": key, "stored": true]
        case "bridge.removeData":
            let key = params["key"] as? String ?? ""
            removeData(key, source: sourceWebViewId)
            return ["key": key, "removed": true]
        case "bridge.getWebViewId":
            return ["id": sourceWebViewId ?? "unknown"]
        case "bridge.getDeviceInfo":
            return DeviceInfo.current()
        case "bridge.getAppState":
            return AppStateMonitor.currentDictionary()
        case "bridge.openSettings":
            return ["opened": BridgePermissions.openAppSettings()]
        case "bridge.getUpiApps":
            return BridgePaymentApps.getUpiApps()
        case "bridge.getPaymentApps":
            return BridgePaymentApps.getPaymentApps()
        case "bridge.canOpenUrl":
            return BridgeIntents.canOpenUrl(params["url"] as? String ?? "")
        case "bridge.openUrl":
            return BridgeIntents.openUrl(params["url"] as? String ?? "")
        case "bridge.launchIntent":
            return BridgeIntents.launchIntent(params)
        case "bridge.queryIntents":
            return BridgeIntents.queryIntents(params)
        case "bridge.openUpiPayment":
            return BridgePaymentApps.openUpiPayment(params)
        case "bridge.buildUpiUri":
            return ["uri": BridgePaymentApps.buildUpiUri(params)]
        case "bridge.getCapabilities":
            return BridgeDevice.getCapabilities()
        case "bridge.getClipboard":
            return BridgeDevice.getClipboard()
        case "bridge.setClipboard":
            return BridgeDevice.setClipboard(params["text"] as? String ?? "")
        case "bridge.vibrate":
            return BridgeDevice.vibrate(params)
        case "bridge.dial":
            return BridgeDevice.dial(params["phone"] as? String ?? "")
        case "bridge.sendSms":
            return BridgeDevice.sendSms(
                phone: params["phone"] as? String ?? "",
                body: params["body"] as? String ?? ""
            )
        case "bridge.openMaps":
            return BridgeDevice.openMaps(params)
        case "bridge.setTorch":
            return BridgeDevice.setTorch(params["enabled"] as? Bool ?? true)
        case "bridge.getDeviceResourceStatus":
            return BridgeDevice.getDeviceResourceStatus()
        case "bridge.releaseDeviceResources":
            return BridgeDevice.releaseDeviceResources(params)
        case "bridge.cancelDeviceOperation":
            return BridgeDevice.cancelDeviceOperation(params)
        case "bridge.getSafeAreaInsets":
            return AppStateMonitor.safeAreaDictionary()
        case "bridge.setSystemUi":
            return BridgeSystemUi.set(params)
        case "bridge.getSystemUi":
            return BridgeSystemUi.get()
        case "bridge.cancelNotification":
            let id = params["id"] as? String ?? ""
            return BridgeNotifications.cancel(identifier: id)
        case "bridge.cancelAllNotifications":
            return BridgeNotifications.cancelAll()
        case "bridge.openNotificationSettings":
            return BridgeNotifications.openSettings()
        case "bridge.getWebViewCacheInfo":
            return BridgeWebViewCache.getInfo(webView: nil)
        case "bridge.setWebViewCachePolicy":
            return BridgeWebViewCache.setPolicy(webView: nil, params: params)
        default:
            throw BridgeError.unknownMethod(method)
        }
    }

    /// Async methods — resolve via callback, not synchronous return.
    func dispatchAsync(method: String, id: String, params: [String: Any]) -> Bool {
        if dispatchDeviceAsync(method: method, id: id, params: params) { return true }
        switch method {
        case "bridge.requestPermission":
            let name = params["permission"] as? String ?? ""
            BridgePermissions.request(name) { result in
                self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result.toDictionary())))")
            }
            return true
        case "bridge.getPermissionStatus":
            let name = params["permission"] as? String ?? ""
            BridgePermissions.getStatus(name) { result in
                self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result.toDictionary())))")
            }
            return true
        case "bridge.getPermissions":
            BridgePermissions.getAllStatuses { list in
                self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(list)))")
            }
            return true
        case "bridge.getNotificationSettings":
            BridgeNotifications.getSettings { result in
                self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
            }
            return true
        default:
            return false
        }
    }

    /// Route a bridge method from React Native (same API as the web SDK).
    public func dispatchBridge(
        method: String,
        params: [String: Any],
        completion: @escaping (Any?, Error?) -> Void
    ) {
        if dispatchDeviceAsyncBridge(method: method, params: params, completion: completion) {
            return
        }
        if dispatchAsyncBridge(method: method, params: params, completion: completion) {
            return
        }
        do {
            completion(try dispatch(method: method, params: params, sourceWebViewId: "rn"), nil)
        } catch {
            completion(nil, error)
        }
    }

    private func dispatchDeviceAsync(method: String, id: String, params: [String: Any]) -> Bool {
        let resolve: ([String: Any]?) -> Void = { result in
            self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
        }
        let reject: (Error) -> Void = { error in
            self.runJS("window.NativeBridge.__reject('\(id)', \(self.jsonString(error.localizedDescription)))")
        }
        switch method {
        case "bridge.getCurrentLocation":
            BridgeDevice.getCurrentLocation(params) { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        case "bridge.takePhoto":
            BridgeDevice.takePhoto(params) { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        case "bridge.pickImage":
            BridgeDevice.pickImage(params) { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        case "bridge.pickImages":
            BridgeDevice.pickImages(params) { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        case "bridge.pickContact":
            BridgeDevice.pickContact { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        case "bridge.share":
            BridgeDevice.share(params) { result, error in
                if let error = error { reject(error) } else { resolve(result) }
            }
            return true
        default:
            return false
        }
    }

    private func dispatchDeviceAsyncBridge(
        method: String,
        params: [String: Any],
        completion: @escaping (Any?, Error?) -> Void
    ) -> Bool {
        switch method {
        case "bridge.getCurrentLocation":
            BridgeDevice.getCurrentLocation(params) { result, error in completion(result, error) }
            return true
        case "bridge.takePhoto":
            BridgeDevice.takePhoto(params) { result, error in completion(result, error) }
            return true
        case "bridge.pickImage":
            BridgeDevice.pickImage(params) { result, error in completion(result, error) }
            return true
        case "bridge.pickImages":
            BridgeDevice.pickImages(params) { result, error in completion(result, error) }
            return true
        case "bridge.pickContact":
            BridgeDevice.pickContact { result, error in completion(result, error) }
            return true
        case "bridge.share":
            BridgeDevice.share(params) { result, error in completion(result, error) }
            return true
        default:
            return false
        }
    }

    private func dispatchAsyncBridge(
        method: String,
        params: [String: Any],
        completion: @escaping (Any?, Error?) -> Void
    ) -> Bool {
        switch method {
        case "bridge.requestPermission":
            let name = params["permission"] as? String ?? ""
            BridgePermissions.request(name) { result in
                completion(result.toDictionary(), nil)
            }
            return true
        case "bridge.getPermissionStatus":
            let name = params["permission"] as? String ?? ""
            BridgePermissions.getStatus(name) { result in
                completion(result.toDictionary(), nil)
            }
            return true
        case "bridge.getPermissions":
            BridgePermissions.getAllStatuses { list in
                completion(list, nil)
            }
            return true
        default:
            return false
        }
    }

    private func filterApiCalls(_ params: [String: Any]) -> [ApiCall] {
        var calls = apiCalls.all()
        if let urlContains = params["urlContains"] as? String, !urlContains.isEmpty {
            calls = calls.filter { $0.url.range(of: urlContains, options: .caseInsensitive) != nil }
        }
        if let method = params["method"] as? String, !method.isEmpty {
            calls = calls.filter { $0.method.caseInsensitiveCompare(method) == .orderedSame }
        }
        if let limit = params["limit"] as? Int, calls.count > limit {
            calls = Array(calls.suffix(limit))
        }
        return calls
    }

    // MARK: - JSON helpers

    private func jsonString(_ value: Any?) -> String {
        guard let value = value else { return "null" }
        guard let data = try? JSONSerialization.data(withJSONObject: [value]),
              let s = String(data: data, encoding: .utf8) else { return "null" }
        return String(s.dropFirst().dropLast()) // unwrap the [ ] so scalars work too
    }

    private func runJS(_ js: String) {
        DispatchQueue.main.async {
            for webView in self.attached.allObjects {
                webView.evaluateJavaScript(js, completionHandler: nil)
            }
        }
    }

    enum BridgeError: Error, LocalizedError {
        case unknownMethod(String)
        var errorDescription: String? {
            switch self {
            case .unknownMethod(let m): return "Unknown method: \(m)"
            }
        }
    }
}

extension NativeBridge: WKScriptMessageHandler {
    public func userContentController(
        _ controller: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        guard let body = message.body as? String,
              let data = body.data(using: .utf8),
              let msg = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return }

        let sourceId: String? = {
            guard let wv = message.webView else { return nil }
            return webViewId(for: wv)
        }()

        if msg["type"] as? String == "event" {
            let event = msg["event"] as? String ?? ""
            guard !event.isEmpty, let wv = message.webView else { return }
            deliverWebEvent(
                event: event,
                payload: msg["payload"],
                webViewId: sourceId ?? "unknown",
                webView: wv
            )
            return
        }

        guard msg["type"] as? String == "request",
              let id = msg["id"] as? String,
              let method = msg["method"] as? String
        else { return }

        let params = msg["params"] as? [String: Any] ?? [:]

        if dispatchAsync(method: method, id: id, params: params) { return }

        if let wv = message.webView {
            switch method {
            case "bridge.canGoBackInWebView":
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(BridgeBackPress.canGoBack(wv)))")
                return
            case "bridge.goBackInWebView":
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(BridgeBackPress.goBack(wv)))")
                return
            case "bridge.setApplySafeAreaPadding":
                let enabled = params["enabled"] as? Bool ?? true
                if let bridge = wv as? BridgeWebView {
                    bridge.setApplySafeAreaPadding(enabled)
                }
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(["enabled": enabled])))")
                return
            case "bridge.setWebViewCachePolicy":
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(BridgeWebViewCache.setPolicy(webView: wv, params: params)))")
                return
            case "bridge.getWebViewCacheInfo":
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(BridgeWebViewCache.getInfo(webView: wv)))")
                return
            case "bridge.clearWebViewCache":
                BridgeWebViewCache.clear(webView: wv, params: params) { result in
                    self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
                }
                return
            case "bridge.reloadWebView":
                let bypass = params["bypassCache"] as? Bool ?? true
                runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(BridgeWebViewCache.reload(webView: wv, bypassCache: bypass)))")
                return
            case "bridge.setCookie":
                do {
                    let result = try BridgeCookies.setCookie(webView: wv, params: params)
                    runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(result)))")
                } catch {
                    runJS("window.NativeBridge.__reject('\(id)', \(jsonString(error.localizedDescription)))")
                }
                return
            case "bridge.setCookies":
                do {
                    let result = try BridgeCookies.setCookies(webView: wv, params: params)
                    runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(result)))")
                } catch {
                    runJS("window.NativeBridge.__reject('\(id)', \(jsonString(error.localizedDescription)))")
                }
                return
            case "bridge.getCookies":
                BridgeCookies.getCookies(webView: wv, params: params) { result in
                    self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
                }
                return
            case "bridge.removeCookie":
                BridgeCookies.removeCookie(webView: wv, params: params) { result in
                    self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
                }
                return
            case "bridge.clearCookies":
                BridgeCookies.clearCookies(webView: wv, params: params) { result in
                    self.runJS("window.NativeBridge.__resolve('\(id)', \(self.jsonString(result)))")
                }
                return
            default:
                break
            }
        }

        do {
            let result = try dispatch(method: method, params: params, sourceWebViewId: sourceId)
            runJS("window.NativeBridge.__resolve('\(id)', \(jsonString(result)))")
        } catch {
            runJS("window.NativeBridge.__reject('\(id)', \(jsonString(error.localizedDescription)))")
        }
    }
}

enum DeviceInfo {
    static func current() -> [String: Any] {
        #if canImport(UIKit)
        return [
            "platform": "ios",
            "model": deviceModel(),
            "osVersion": osVersion(),
            "appBundleId": Bundle.main.bundleIdentifier ?? "",
        ]
        #else
        return ["platform": "ios"]
        #endif
    }
    private static func deviceModel() -> String {
        var sysinfo = utsname(); uname(&sysinfo)
        return withUnsafePointer(to: &sysinfo.machine) {
            $0.withMemoryRebound(to: CChar.self, capacity: 1) { String(validatingUTF8: $0) ?? "" }
        }
    }
    private static func osVersion() -> String {
        let v = ProcessInfo.processInfo.operatingSystemVersion
        return "\(v.majorVersion).\(v.minorVersion).\(v.patchVersion)"
    }
}
