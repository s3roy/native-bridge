/**
 * NativeBridge — web SDK
 *
 * This file is AUTO-INJECTED by the native package at document start, so your
 * web pages do NOT need to bundle or import anything. It is published here only
 * for reference / TypeScript typing.
 *
 * Usage from any web page running inside the native WebView:
 *
 *   await NativeBridge.getApiCalls({ urlContains: "/chat" })
 *   NativeBridge.onApiCall(call => console.log(call))
 *   await NativeBridge.getNotifications()
 *   NativeBridge.onNotification(n => render(n))
 *   await NativeBridge.getData("authToken")
 *   await NativeBridge.request("any.method", { ... })   // generic escape hatch
 */
(function () {
  if (window.NativeBridge && window.NativeBridge.__installed) return;

  var pending = {};
  var listeners = {};
  var seq = 0;

  function send(payload) {
    var s = JSON.stringify(payload);
    if (window.AndroidBridge && window.AndroidBridge.postMessage) {
      window.AndroidBridge.postMessage(s); // Android
    } else if (
      window.webkit &&
      window.webkit.messageHandlers &&
      window.webkit.messageHandlers.iosBridge
    ) {
      window.webkit.messageHandlers.iosBridge.postMessage(s); // iOS
    } else if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {
      window.ReactNativeWebView.postMessage(s); // React Native WebView
    } else {
      console.warn("[NativeBridge] native host not found (running in a browser?)");
    }
  }

  function request(method, params, opts) {
    params = params || {};
    var timeout = (opts && opts.timeout) || 15000;
    var id = "r" + ++seq;
    return new Promise(function (resolve, reject) {
      var t = setTimeout(function () {
        delete pending[id];
        reject(new Error("NativeBridge timeout: " + method));
      }, timeout);
      pending[id] = { resolve: resolve, reject: reject, t: t };
      send({ type: "request", id: id, method: method, params: params });
    });
  }

  function on(event, cb) {
    (listeners[event] || (listeners[event] = [])).push(cb);
    return function off() {
      listeners[event] = (listeners[event] || []).filter(function (f) {
        return f !== cb;
      });
    };
  }

  var NativeBridge = {
    __installed: true,
    _backListeners: [],

    isAvailable: function () {
      return !!(
        window.AndroidBridge ||
        (window.webkit &&
          window.webkit.messageHandlers &&
          window.webkit.messageHandlers.iosBridge) ||
        (window.ReactNativeWebView && window.ReactNativeWebView.postMessage)
      );
    },

    // ---- generic channel (call any native method on demand) ----
    request: request,
    on: on,
    /** Fire-and-forget event to native — any event name + JSON-serializable payload. */
    send: function (event, payload) {
      send({
        type: "event",
        event: event,
        payload: payload !== undefined ? payload : null,
      });
    },

    /** Built-in web → native event names. */
    EVENTS: {
      WEBVIEW_LOADED: "WEBVIEW_LOADED",
    },

    _buildWebViewLoadedPayload: function (phase, extra) {
      var payload = {
        event: "WEBVIEW_LOADED",
        url: location.href,
        title: document.title || "",
        timestamp: Date.now(),
        readyState: document.readyState,
        phase: phase || "manual",
        referrer: document.referrer || null,
      };
      if (extra && typeof extra === "object") {
        Object.keys(extra).forEach(function (k) {
          payload[k] = extra[k];
        });
      }
      return payload;
    },

    /**
     * Notify native that this web page is ready. Called automatically on DOM/load;
     * call manually after SPA client-side navigations.
     */
    notifyWebViewLoaded: function (extra) {
      extra = extra || {};
      var self = this;
      var phase = extra.phase || "manual";
      var payload = self._buildWebViewLoadedPayload(phase, extra);

      function deliver() {
        self.send(self.EVENTS.WEBVIEW_LOADED, payload);
        return payload;
      }

      if (!self.isAvailable()) return Promise.resolve(deliver());

      return self
        .getWebViewId()
        .then(function (id) {
          payload.webViewId = id && (id.id || id.webViewId || id);
          return deliver();
        })
        .catch(function () {
          return deliver();
        });
    },

    // ---- built-in API (no native code required) ----
    /** Captured native API calls. filter: { urlContains, method, limit } */
    getApiCalls: function (filter) {
      return request("bridge.getApiCalls", filter || {});
    },
    /** Live stream of every native API call as it completes. */
    onApiCall: function (cb) {
      return on("api.call", cb);
    },
    /** All captured push/local notifications. */
    getNotifications: function () {
      return request("bridge.getNotifications", {});
    },
    /** Live stream of notifications as they arrive. */
    onNotification: function (cb) {
      return on("notification", cb);
    },
    /** Arbitrary key/value the native app published (e.g. authToken, userId). */
    getData: function (key) {
      return request("bridge.getData", { key: key });
    },
    getAllData: function () {
      return request("bridge.getAllData", {});
    },
    /** Write to shared store — all WebViews in the app can read via getData / onData. */
    setData: function (key, value) {
      return request("bridge.setData", { key: key, value: value });
    },
    removeData: function (key) {
      return request("bridge.removeData", { key: key });
    },
    /** This WebView instance id (wv_1, wv_2, …) — useful with multiple WebViews. */
    getWebViewId: function () {
      return request("bridge.getWebViewId", {});
    },
    /** Live stream when any WebView or native updates shared data. */
    onData: function (cb) {
      return on("data", cb);
    },
    getDeviceInfo: function () {
      return request("bridge.getDeviceInfo", {});
    },

    /** Full snapshot: lifecycle, PiP, network, orientation, webview. */
    getAppState: function () {
      return request("bridge.getAppState", {});
    },
    /** Fires on ANY app-state change (includes `changed` field). */
    onAppState: function (cb) {
      return on("app.state", cb);
    },
    onLifecycle: function (cb) {
      return on("app.lifecycle", cb);
    },
    onPiP: function (cb) {
      return on("app.pip", cb);
    },
    onNetwork: function (cb) {
      return on("app.network", cb);
    },
    onOrientation: function (cb) {
      return on("app.orientation", cb);
    },
    onWebViewState: function (cb) {
      return on("app.webview", cb);
    },
    onKeyboard: function (cb) {
      return on("app.keyboard", cb);
    },
    onSafeArea: function (cb) {
      return on("app.safeArea", cb);
    },
    getSafeArea: function () {
      return request("bridge.getSafeAreaInsets", {});
    },
    getSafeAreaInsets: function () {
      return request("bridge.getSafeAreaInsets", {});
    },
    setApplySafeAreaPadding: function (enabled) {
      return request("bridge.setApplySafeAreaPadding", { enabled: enabled !== false });
    },
    setSystemUi: function (opts) {
      return request("bridge.setSystemUi", opts || {});
    },
    getSystemUi: function () {
      return request("bridge.getSystemUi", {});
    },
    getNotificationSettings: function () {
      return request("bridge.getNotificationSettings", {});
    },
    cancelNotification: function (opts) {
      return request("bridge.cancelNotification", opts || {});
    },
    cancelAllNotifications: function () {
      return request("bridge.cancelAllNotifications", {});
    },
    openNotificationSettings: function () {
      return request("bridge.openNotificationSettings", {});
    },
    onBattery: function (cb) {
      return on("app.battery", cb);
    },
    onAudio: function (cb) {
      return on("app.audio", cb);
    },
    onDisplay: function (cb) {
      return on("app.display", cb);
    },
    onTheme: function (cb) {
      return on("app.theme", cb);
    },
    onLocale: function (cb) {
      return on("app.locale", cb);
    },
    onCall: function (cb) {
      return on("app.call", cb);
    },

    /** Permission names: camera, microphone, location, locationCoarse, notifications, photos, bluetooth, contacts */
    getPermissionStatus: function (permission) {
      return request("bridge.getPermissionStatus", { permission: permission });
    },
    requestPermission: function (permission) {
      return request("bridge.requestPermission", { permission: permission });
    },
    getPermissions: function () {
      return request("bridge.getPermissions", {});
    },
    openSettings: function () {
      return request("bridge.openSettings", {});
    },
    onPermissionChange: function (cb) {
      return on("permission.change", cb);
    },

    Permissions: {
      CAMERA: "camera",
      MICROPHONE: "microphone",
      LOCATION: "location",
      LOCATION_COARSE: "locationCoarse",
      NOTIFICATIONS: "notifications",
      PHOTOS: "photos",
      BLUETOOTH: "bluetooth",
      CONTACTS: "contacts",
    },

    /** Installed UPI apps (PhonePe, GPay, Paytm, BHIM, …). */
    getUpiApps: function () {
      return request("bridge.getUpiApps", {});
    },
    /** UPI + wallets + messaging apps with installed flags. */
    getPaymentApps: function () {
      return request("bridge.getPaymentApps", {});
    },
    canOpenUrl: function (url) {
      return request("bridge.canOpenUrl", { url: url });
    },
    openUrl: function (url) {
      return request("bridge.openUrl", { url: url });
    },
    /** params: { vpa, amount, name, note, txnId, currency } */
    openUpiPayment: function (params) {
      return request("bridge.openUpiPayment", params || {});
    },
    buildUpiUri: function (params) {
      return request("bridge.buildUpiUri", params || {});
    },
    /** Android: launch any intent. iOS: open URL / deep link. */
    launchIntent: function (params) {
      return request("bridge.launchIntent", params || {});
    },
    /** List apps that can handle an intent / URL. */
    queryIntents: function (params) {
      return request("bridge.queryIntents", params || {});
    },

    /** Request permission if not already granted. */
    ensurePermission: function (permission) {
      return request("bridge.getPermissionStatus", { permission: permission }).then(function (r) {
        if (r.status === "granted") return r;
        return request("bridge.requestPermission", { permission: permission });
      });
    },

    // ---- Device: location, camera, gallery, contacts, clipboard, share, … ----
    getCapabilities: function () {
      return request("bridge.getCapabilities", {});
    },
    getCurrentLocation: function (opts) {
      opts = opts || {};
      return request("bridge.getCurrentLocation", opts, { timeout: opts.timeout || 20000 });
    },
    takePhoto: function (opts) {
      return request("bridge.takePhoto", opts || {}, { timeout: 120000 });
    },
    pickImage: function (opts) {
      return request("bridge.pickImage", opts || {}, { timeout: 120000 });
    },
    pickContact: function () {
      return request("bridge.pickContact", {}, { timeout: 120000 });
    },
    getClipboard: function () {
      return request("bridge.getClipboard", {});
    },
    setClipboard: function (text) {
      return request("bridge.setClipboard", { text: text });
    },
    share: function (opts) {
      return request("bridge.share", opts || {});
    },
    vibrate: function (opts) {
      return request("bridge.vibrate", opts || {});
    },
    dial: function (phone) {
      return request("bridge.dial", { phone: phone });
    },
    sendSms: function (phone, body) {
      return request("bridge.sendSms", { phone: phone, body: body || "" });
    },
    openMaps: function (opts) {
      return request("bridge.openMaps", opts || {});
    },
    setTorch: function (enabled) {
      return request("bridge.setTorch", { enabled: enabled !== false });
    },
    pickImages: function (opts) {
      return request("bridge.pickImages", opts || {}, { timeout: 120000 });
    },
    getDeviceResourceStatus: function () {
      return request("bridge.getDeviceResourceStatus", {});
    },
    releaseDeviceResources: function (opts) {
      return request("bridge.releaseDeviceResources", opts || {});
    },
    cancelDeviceOperation: function (typeOrOpts) {
      var p = typeof typeOrOpts === "string" ? { type: typeOrOpts } : (typeOrOpts || {});
      return request("bridge.cancelDeviceOperation", p);
    },
    stopMediaStream: function (stream) {
      if (stream && stream.getTracks) {
        stream.getTracks().forEach(function (t) { try { t.stop(); } catch (e) {} });
      }
      return Promise.resolve({ stopped: true });
    },

    /** Hardware / gesture back — return true to consume (block app exit). */
    onBackPress: function (cb) {
      NativeBridge._backListeners.push(cb);
      return function () {
        NativeBridge._backListeners = NativeBridge._backListeners.filter(function (f) { return f !== cb; });
      };
    },
    canGoBackInWebView: function () {
      return request("bridge.canGoBackInWebView", {});
    },
    goBackInWebView: function () {
      return request("bridge.goBackInWebView", {});
    },

    /**
     * Cache policy — smart (default): fresh HTML, cached hashed JS/CSS chunks.
     * mode: "smart" | "noCache" | "default" | "cacheOnly"
     */
    setWebViewCachePolicy: function (opts) {
      return request("bridge.setWebViewCachePolicy", opts || {});
    },
    getWebViewCacheInfo: function () {
      return request("bridge.getWebViewCacheInfo", {});
    },
    clearWebViewCache: function (opts) {
      return request("bridge.clearWebViewCache", opts || {});
    },
    reloadWebView: function (opts) {
      return request("bridge.reloadWebView", opts || { bypassCache: true });
    },

    setCookie: function (opts) {
      return request("bridge.setCookie", opts || {});
    },
    setCookies: function (opts) {
      return request("bridge.setCookies", opts || {});
    },
    getCookies: function (opts) {
      return request("bridge.getCookies", opts || {});
    },
    removeCookie: function (opts) {
      return request("bridge.removeCookie", opts || {});
    },
    clearCookies: function (opts) {
      return request("bridge.clearCookies", opts || {});
    },

    Device: {
      getCapabilities: function () { return NativeBridge.getCapabilities(); },
      getCurrentLocation: function (o) { return NativeBridge.getCurrentLocation(o); },
      takePhoto: function (o) { return NativeBridge.takePhoto(o); },
      pickImage: function (o) { return NativeBridge.pickImage(o); },
      pickContact: function () { return NativeBridge.pickContact(); },
      getClipboard: function () { return NativeBridge.getClipboard(); },
      setClipboard: function (t) { return NativeBridge.setClipboard(t); },
      share: function (o) { return NativeBridge.share(o); },
      vibrate: function (o) { return NativeBridge.vibrate(o); },
      dial: function (p) { return NativeBridge.dial(p); },
      sendSms: function (p, b) { return NativeBridge.sendSms(p, b); },
      openMaps: function (o) { return NativeBridge.openMaps(o); },
      setTorch: function (e) { return NativeBridge.setTorch(e); },
      pickImages: function (o) { return NativeBridge.pickImages(o); },
      getDeviceResourceStatus: function () { return NativeBridge.getDeviceResourceStatus(); },
      releaseDeviceResources: function (o) { return NativeBridge.releaseDeviceResources(o); },
      cancelDeviceOperation: function (t) { return NativeBridge.cancelDeviceOperation(t); },
      stopMediaStream: function (s) { return NativeBridge.stopMediaStream(s); },
    },

    // ---- internal: invoked by native, do not call directly ----
    __resolve: function (id, result) {
      var p = pending[id];
      if (!p) return;
      clearTimeout(p.t);
      delete pending[id];
      p.resolve(result);
    },
    __reject: function (id, err) {
      var p = pending[id];
      if (!p) return;
      clearTimeout(p.t);
      delete pending[id];
      p.reject(new Error(err));
    },
    __emit: function (event, payload) {
      (listeners[event] || []).forEach(function (cb) {
        try {
          cb(payload);
        } catch (e) {
          /* swallow */
        }
      });
    },
  };

  window.NativeBridge = NativeBridge;
  try {
    window.dispatchEvent(new Event("nativebridgeready"));
  } catch (e) {}
})();
