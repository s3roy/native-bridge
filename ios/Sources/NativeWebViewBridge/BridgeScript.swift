import Foundation

/// The web SDK injected into every attached WKWebView at document start. Kept
/// identical to bridge-js/native-bridge.js. Web pages bundle nothing.
enum BridgeScript {
    static let js: String = """
    (function () {
      if (window.NativeBridge && window.NativeBridge.__installed) return;
      var pending = {}, listeners = {}, seq = 0;
      function send(payload) {
        var s = JSON.stringify(payload);
        if (window.AndroidBridge && window.AndroidBridge.postMessage) window.AndroidBridge.postMessage(s);
        else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) window.webkit.messageHandlers.iosBridge.postMessage(s);
        else if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) window.ReactNativeWebView.postMessage(s);
        else console.warn("[NativeBridge] native host not found");
      }
      function request(method, params, opts) {
        params = params || {};
        var timeout = (opts && opts.timeout) || 15000;
        var id = "r" + (++seq);
        return new Promise(function (resolve, reject) {
          var t = setTimeout(function () { delete pending[id]; reject(new Error("NativeBridge timeout: " + method)); }, timeout);
          pending[id] = { resolve: resolve, reject: reject, t: t };
          send({ type: "request", id: id, method: method, params: params });
        });
      }
      function on(event, cb) {
        (listeners[event] || (listeners[event] = [])).push(cb);
        return function () { listeners[event] = (listeners[event] || []).filter(function (f) { return f !== cb; }); };
      }
      window.NativeBridge = {
        __installed: true,
        _backListeners: [],
        isAvailable: function () { return !!(window.AndroidBridge || (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosBridge) || (window.ReactNativeWebView && window.ReactNativeWebView.postMessage)); },
        request: request,
        on: on,
        send: function (ev, pl) { send({ type: "event", event: ev, payload: pl !== undefined ? pl : null }); },
        getApiCalls: function (f) { return request("bridge.getApiCalls", f || {}); },
        onApiCall: function (cb) { return on("api.call", cb); },
        getNotifications: function () { return request("bridge.getNotifications", {}); },
        onNotification: function (cb) { return on("notification", cb); },
        getData: function (k) { return request("bridge.getData", { key: k }); },
        getAllData: function () { return request("bridge.getAllData", {}); },
        setData: function (k, v) { return request("bridge.setData", { key: k, value: v }); },
        removeData: function (k) { return request("bridge.removeData", { key: k }); },
        getWebViewId: function () { return request("bridge.getWebViewId", {}); },
        onData: function (cb) { return on("data", cb); },
        getDeviceInfo: function () { return request("bridge.getDeviceInfo", {}); },
        getAppState: function () { return request("bridge.getAppState", {}); },
        onAppState: function (cb) { return on("app.state", cb); },
        onLifecycle: function (cb) { return on("app.lifecycle", cb); },
        onOrientation: function (cb) { return on("app.orientation", cb); },
        onPiP: function (cb) { return on("app.pip", cb); },
        onNetwork: function (cb) { return on("app.network", cb); },
        onWebViewState: function (cb) { return on("app.webview", cb); },
        onKeyboard: function (cb) { return on("app.keyboard", cb); },
        onSafeArea: function (cb) { return on("app.safeArea", cb); },
        getSafeArea: function () { return request("bridge.getSafeAreaInsets", {}); },
        getSafeAreaInsets: function () { return request("bridge.getSafeAreaInsets", {}); },
        setApplySafeAreaPadding: function (e) { return request("bridge.setApplySafeAreaPadding", { enabled: e !== false }); },
        setSystemUi: function (o) { return request("bridge.setSystemUi", o || {}); },
        getSystemUi: function () { return request("bridge.getSystemUi", {}); },
        getNotificationSettings: function () { return request("bridge.getNotificationSettings", {}); },
        cancelNotification: function (o) { return request("bridge.cancelNotification", o || {}); },
        cancelAllNotifications: function () { return request("bridge.cancelAllNotifications", {}); },
        openNotificationSettings: function () { return request("bridge.openNotificationSettings", {}); },
        onBattery: function (cb) { return on("app.battery", cb); },
        onAudio: function (cb) { return on("app.audio", cb); },
        onDisplay: function (cb) { return on("app.display", cb); },
        onTheme: function (cb) { return on("app.theme", cb); },
        onLocale: function (cb) { return on("app.locale", cb); },
        onCall: function (cb) { return on("app.call", cb); },
        getPermissionStatus: function (p) { return request("bridge.getPermissionStatus", { permission: p }); },
        requestPermission: function (p) { return request("bridge.requestPermission", { permission: p }); },
        getPermissions: function () { return request("bridge.getPermissions", {}); },
        openSettings: function () { return request("bridge.openSettings", {}); },
        onPermissionChange: function (cb) { return on("permission.change", cb); },
        Permissions: { CAMERA:"camera", MICROPHONE:"microphone", LOCATION:"location", LOCATION_COARSE:"locationCoarse", NOTIFICATIONS:"notifications", PHOTOS:"photos", BLUETOOTH:"bluetooth", CONTACTS:"contacts" },
        getUpiApps: function () { return request("bridge.getUpiApps", {}); },
        getPaymentApps: function () { return request("bridge.getPaymentApps", {}); },
        canOpenUrl: function (u) { return request("bridge.canOpenUrl", { url: u }); },
        openUrl: function (u) { return request("bridge.openUrl", { url: u }); },
        openUpiPayment: function (p) { return request("bridge.openUpiPayment", p || {}); },
        buildUpiUri: function (p) { return request("bridge.buildUpiUri", p || {}); },
        launchIntent: function (p) { return request("bridge.launchIntent", p || {}); },
        queryIntents: function (p) { return request("bridge.queryIntents", p || {}); },
        ensurePermission: function (perm) { return request("bridge.getPermissionStatus", { permission: perm }).then(function (r) { return r.status === "granted" ? r : request("bridge.requestPermission", { permission: perm }); }); },
        getCapabilities: function () { return request("bridge.getCapabilities", {}); },
        getCurrentLocation: function (o) { o = o || {}; return request("bridge.getCurrentLocation", o, { timeout: o.timeout || 20000 }); },
        takePhoto: function (o) { return request("bridge.takePhoto", o || {}, { timeout: 120000 }); },
        pickImage: function (o) { return request("bridge.pickImage", o || {}, { timeout: 120000 }); },
        pickContact: function () { return request("bridge.pickContact", {}, { timeout: 120000 }); },
        getClipboard: function () { return request("bridge.getClipboard", {}); },
        setClipboard: function (t) { return request("bridge.setClipboard", { text: t }); },
        share: function (o) { return request("bridge.share", o || {}); },
        vibrate: function (o) { return request("bridge.vibrate", o || {}); },
        dial: function (p) { return request("bridge.dial", { phone: p }); },
        sendSms: function (p, b) { return request("bridge.sendSms", { phone: p, body: b || "" }); },
        openMaps: function (o) { return request("bridge.openMaps", o || {}); },
        setTorch: function (e) { return request("bridge.setTorch", { enabled: e !== false }); },
        pickImages: function (o) { return request("bridge.pickImages", o || {}, { timeout: 120000 }); },
        getDeviceResourceStatus: function () { return request("bridge.getDeviceResourceStatus", {}); },
        releaseDeviceResources: function (o) { return request("bridge.releaseDeviceResources", o || {}); },
        cancelDeviceOperation: function (t) { var p = typeof t === "string" ? { type: t } : (t || {}); return request("bridge.cancelDeviceOperation", p); },
        stopMediaStream: function (s) { if (s && s.getTracks) s.getTracks().forEach(function (t) { try { t.stop(); } catch (e) {} }); return Promise.resolve({ stopped: true }); },
        onBackPress: function (cb) { NativeBridge._backListeners.push(cb); return function () { NativeBridge._backListeners = NativeBridge._backListeners.filter(function (f) { return f !== cb; }); }; },
        canGoBackInWebView: function () { return request("bridge.canGoBackInWebView", {}); },
        goBackInWebView: function () { return request("bridge.goBackInWebView", {}); },
        setWebViewCachePolicy: function (o) { return request("bridge.setWebViewCachePolicy", o || {}); },
        getWebViewCacheInfo: function () { return request("bridge.getWebViewCacheInfo", {}); },
        clearWebViewCache: function (o) { return request("bridge.clearWebViewCache", o || {}); },
        reloadWebView: function (o) { return request("bridge.reloadWebView", o || { bypassCache: true }); },
        setCookie: function (o) { return request("bridge.setCookie", o || {}); },
        setCookies: function (o) { return request("bridge.setCookies", o || {}); },
        getCookies: function (o) { return request("bridge.getCookies", o || {}); },
        removeCookie: function (o) { return request("bridge.removeCookie", o || {}); },
        clearCookies: function (o) { return request("bridge.clearCookies", o || {}); },
        __resolve: function (id, r) { var p = pending[id]; if (!p) return; clearTimeout(p.t); delete pending[id]; p.resolve(r); },
        __reject: function (id, e) { var p = pending[id]; if (!p) return; clearTimeout(p.t); delete pending[id]; p.reject(new Error(e)); },
        __emit: function (ev, pl) { (listeners[ev] || []).forEach(function (cb) { try { cb(pl); } catch (e) {} }); }
      };
      try { window.dispatchEvent(new Event("nativebridgeready")); } catch (e) {}
    })();
    """
}
