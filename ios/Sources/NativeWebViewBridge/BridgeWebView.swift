import Foundation
import WebKit

/// Drop-in WKWebView with full bridge + permission wiring.
public final class BridgeWebView: WKWebView, WKNavigationDelegate, WKUIDelegate {

    public init(frame: CGRect = .zero) {
        let config = WKWebViewConfiguration()
        super.init(frame: frame, configuration: config)
        commonInit()
    }

    public init(frame: CGRect, configuration: WKWebViewConfiguration) {
        super.init(frame: frame, configuration: configuration)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private var interceptBackPress = true
    private var applySafeAreaPadding = true

    private func commonInit() {
        navigationDelegate = self
        uiDelegate = self
        allowsBackForwardNavigationGestures = true
        NativeBridge.shared.attach(self)
        BridgeWebViewCache.applyOnAttach(webView: self)
    }

    /// Load URL — smart mode bypasses HTML cache, keeps static chunks cached.
    public func loadFresh(_ url: URL) {
        BridgeWebViewCache.load(self, url: url)
    }

    public func setWebViewCachePolicy(_ params: [String: Any]) {
        _ = BridgeWebViewCache.setPolicy(webView: self, params: params)
    }

    /// Set an HTTP cookie visible to the web page (e.g. session token).
    public func setCookie(name: String, value: String, url: String? = nil) {
        var params: [String: Any] = ["name": name, "value": value]
        if let url = url { params["url"] = url }
        _ = try? BridgeCookies.setCookie(webView: self, params: params)
    }

    /// Remove every cookie from this WebView.
    public func clearAllCookies(completion: (() -> Void)? = nil) {
        BridgeCookies.clearCookies(webView: self, params: [:]) { _ in completion?() }
    }

    /// Push a custom event with any JSON-serializable payload to all WebViews.
    public func publishEvent(_ event: String, _ payload: Any = NSNull()) {
        NativeBridge.shared.publishEvent(event, payload)
    }

    /// Handle `NativeBridge.send` from this WebView only.
    public func setOnWebEvent(_ handler: ((String, Any?) -> Void)?) {
        NativeBridge.shared.setWebViewEventHandler(self, handler: handler)
    }

    /// Handle `WEBVIEW_LOADED` from this WebView only.
    public func setOnWebViewLoaded(_ handler: ((WebViewLoadedPayload) -> Void)?) {
        NativeBridge.shared.setWebViewLoadedHandler(self, handler: handler)
    }

    /// When true (default), edge-swipe back notifies the web page via `back.press`.
    public func setInterceptBackPress(_ enabled: Bool) {
        interceptBackPress = enabled
    }

    /// Programmatic back — same flow as Android hardware back (for nav bar buttons).
    public func handleBackPress(completion: ((Bool) -> Void)? = nil) {
        BridgeBackPress.handle(webView: self) { consumed in
            completion?(consumed)
        }
    }

    public override func didMoveToWindow() {
        super.didMoveToWindow()
        AppStateMonitor.setWebViewVisible(window != nil)
        AppStateMonitor.refreshSafeArea(from: self)
    }

    /// When true (default), content inset keeps pages below notch / home indicator.
    public func setApplySafeAreaPadding(_ enabled: Bool) {
        applySafeAreaPadding = enabled
        applySafeAreaContentInset()
    }

    public func isApplySafeAreaPadding() -> Bool { applySafeAreaPadding }

    public override func layoutSubviews() {
        super.layoutSubviews()
        applySafeAreaContentInset()
        AppStateMonitor.refreshSafeArea(from: self)
    }

    public override func safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()
        applySafeAreaContentInset()
        AppStateMonitor.refreshSafeArea(from: self)
    }

    private func applySafeAreaContentInset() {
        if #available(iOS 11.0, *) {
            scrollView.contentInsetAdjustmentBehavior = .never
        }
        if applySafeAreaPadding {
            let insets = safeAreaInsets
            scrollView.contentInset = UIEdgeInsets(
                top: insets.top, left: insets.left, bottom: insets.bottom, right: insets.right
            )
            scrollView.scrollIndicatorInsets = scrollView.contentInset
        } else {
            scrollView.contentInset = .zero
            scrollView.scrollIndicatorInsets = .zero
        }
    }

    public func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        AppStateMonitor.setWebViewUrl(webView.url?.absoluteString)
        AppStateMonitor.refreshSafeArea(from: webView)
    }

    public func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        if interceptBackPress && navigationAction.navigationType == .backForward {
            BridgeBackPress.emitGestureBack(webView: webView)
        }
        if navigationAction.targetFrame?.isMainFrame == true,
           BridgeWebViewCache.shouldBypassDocumentRequest(navigationAction.request, webView: webView) {
            let req = BridgeWebViewCache.documentRequest(from: navigationAction.request)
            webView.load(req)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    // MARK: - WKUIDelegate (getUserMedia in web pages)

    @available(iOS 15.0, *)
    public func webView(
        _ webView: WKWebView,
        requestMediaCapturePermissionFor origin: WKSecurityOrigin,
        initiatedByFrame frame: WKFrameInfo,
        type: WKMediaCaptureType,
        decisionHandler: @escaping (WKPermissionDecision) -> Void
    ) {
        let granted: Bool
        switch type {
        case .camera:
            granted = BridgePermissions.isGranted(BridgePermissionNames.camera)
        case .microphone:
            granted = BridgePermissions.isGranted(BridgePermissionNames.microphone)
        case .cameraAndMicrophone:
            granted = BridgePermissions.isGranted(BridgePermissionNames.camera)
                && BridgePermissions.isGranted(BridgePermissionNames.microphone)
        @unknown default:
            granted = false
        }
        decisionHandler(granted ? .grant : .deny)
    }
}
