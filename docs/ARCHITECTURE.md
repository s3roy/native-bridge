# Architecture

System design for **native-webview-bridge** — intended for solution architects and senior mobile/web engineers.

---

## 1. High-level overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Host mobile application                          │
│  ┌─────────────┐   ┌──────────────────┐   ┌─────────────────────────┐ │
│  │ BridgeWebView│◄──│  NativeBridge    │──►│ Capture layer           │ │
│  │ (WebView)    │   │  (singleton)     │   │ · OkHttp interceptor    │ │
│  └──────┬───────┘   │                  │   │ · URLSession swizzle    │ │
│         │           │  · data store    │   │ · Notification delegate │ │
│         │           │  · dispatch      │   │ · AppStateMonitor       │ │
│         │           │  · emit events   │   └─────────────────────────┘ │
│         ▼           └────────▲─────────┘                               │
│  ┌─────────────┐             │                                         │
│  │ Web page    │  postMessage│ resolve / reject / emit                  │
│  │ NativeBridge│─────────────┘                                         │
│  │ (injected)  │                                                       │
│  └─────────────┘                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Components

| Component | Responsibility |
|-----------|----------------|
| **BridgeWebView** | Drop-in WebView; wires JS interface, injects SDK, tracks WebView-specific state |
| **NativeBridge** | Singleton hub: attached WebViews, data store, event fan-out, request dispatch |
| **BridgeScript** | Minified web SDK injected at document start |
| **BridgeDispatcher** | Shared method router (WebView JS + React Native module) |
| **AppStateMonitor** | OS signal collectors; pushes deltas to web |
| **BridgeInterceptor** (Android) | OkHttp interceptor for HTTP capture |
| **BridgeURLProtocol** (iOS) | URLSession hook for HTTP capture |
| **BridgePermissions** | Runtime permission flow with transparent Activity (Android) |
| **BridgePaymentApps / BridgeIntents** | UPI detection and intent/URL launching |

---

## 2. Message protocol

### Web → Native (request)

```json
{
  "type": "request",
  "id": "r42",
  "method": "bridge.getAppState",
  "params": {}
}
```

### Native → Web (response)

```javascript
window.NativeBridge.__resolve("r42", { lifecycle: "active", ... });
window.NativeBridge.__reject("r42", "Permission denied");
```

### Native → Web (event)

```javascript
window.NativeBridge.__emit("app.state", { lifecycle: "background", changed: "lifecycle" });
window.NativeBridge.__emit("api.call", { url: "...", status: 200 });
```

### Transport by platform

| Platform | Channel |
|----------|---------|
| Android | `@JavascriptInterface AndroidBridge.postMessage(string)` |
| iOS | `WKScriptMessageHandler` named `iosBridge` |
| React Native WebView | `ReactNativeWebView.postMessage` → RN module → `injectJavaScript(__resolve)` |

---

## 3. Request lifecycle

```
Web page                    NativeBridge                 Handler
   │                            │                          │
   │── request(id, method) ────►│                          │
   │                            │── dispatch(method) ─────►│
   │                            │◄── result / error ───────│
   │◄── __resolve(id, result) ──│                          │
```

**Async methods** (permissions): native resolves on callback thread, then posts to WebView main thread.

**Timeout:** Web SDK rejects after 15s (configurable) if no `__resolve` received.

---

## 4. Data stores

| Store | Capacity | Eviction | Access |
|-------|----------|----------|--------|
| API calls | Ring buffer (~200) | FIFO | `getApiCalls`, `onApiCall` |
| Notifications | Ring buffer (~100) | FIFO | `getNotifications`, `onNotification` |
| Key-value | Unbounded | Host-managed | `putData` / `getData` |

Stores are **in-memory** — not persisted across process death unless host app re-publishes via `putData`.

---

## 5. App state pipeline

```
OS signals (lifecycle, network, battery, …)
        │
        ▼
 AppStateMonitor (native)
        │
        ├── merge into AppStateSnapshot
        │
        ├── emit("app.state", snapshot)     ← full snapshot + changed field
        └── emit("app.<channel>", delta)    ← e.g. app.keyboard, app.network
```

Web teams can subscribe broadly (`onAppState`) or to specific channels (`onKeyboard`) for lower handler cost.

---

## 6. Security boundary

```
┌────────────────────────────────────────┐
│  Untrusted web content (your URL)      │
│  Can only call declared bridge.* APIs  │
└────────────────┬───────────────────────┘
                 │ whitelisted methods
                 ▼
┌────────────────────────────────────────┐
│  BridgeDispatcher                      │
│  No arbitrary Java/ObjC reflection     │
└────────────────┬───────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────┐
│  OS APIs (permissions, intents, …)     │
└────────────────────────────────────────┘
```

**Custom methods:** Host apps can extend dispatch by registering handlers — not exposed to web unless explicitly wired.

See [SECURITY.md](./SECURITY.md) for hardening guidance.

---

## 7. React Native architecture

```
┌──────────────── WebView page ────────────────┐
│  window.NativeBridge → ReactNativeWebView    │
└──────────────────────┬───────────────────────┘
                       │ postMessage
                       ▼
┌──────────────── BridgeWebView (RN) ──────────┐
│  onMessage → dispatchBridgeMessage()         │
└──────────────────────┬───────────────────────┘
                       │ NativeModules.NativeWebViewBridge.dispatch
                       ▼
┌──────────────── BridgeDispatcher (shared) ─────┐
│  Same handlers as native Android/iOS         │
└──────────────────────────────────────────────┘
```

RN JavaScript can also call `NativeBridge.*` directly without a WebView.

---

## 8. Module dependency graph (Android)

```
app
 └── native-webview-bridge (AAR)
      ├── BridgeWebView → NativeBridge
      ├── BridgeDispatcher → BridgePermissions, BridgePaymentApps, BridgeIntents, AppStateMonitor
      ├── BridgeInitProvider (auto-start)
      └── BridgeInterceptor (optional, host OkHttp)

react-native wrapper
 └── native-webview-bridge (AAR)
 └── NativeWebViewBridgeModule → BridgeDispatcher
```

---

## 9. Extension points

| Extension | How |
|-----------|-----|
| Custom web methods | Add case in `BridgeDispatcher` + document in API reference |
| Custom events | `NativeBridge.publishEvent(name, payload)` |
| Device info fields | `NativeBridge.setDeviceInfoProvider { ... }` (Android) |
| API capture filter | Extend `BridgeInterceptor` / `BridgeURLProtocol` |
| Permission allowlist | Filter in dispatcher before `BridgePermissions.request` |

---

## 10. Non-goals (by design)

- Not a replacement for Capacitor/Cordova plugin ecosystem
- Not a JS-to-native RPC for arbitrary native code execution
- Not persistent encrypted storage (host must implement)
- Not payment gateway / PSP integration (UPI opens external apps only)
