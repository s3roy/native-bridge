# API reference

Complete Web SDK surface (`window.NativeBridge`). Identical across Android, iOS, and React Native WebView unless noted.

TypeScript: [bridge-js/native-bridge.d.ts](../bridge-js/native-bridge.d.ts)

---

## Core

### `NativeBridge.isAvailable(): boolean`

Returns `true` when running inside a native WebView with bridge wired.

```javascript
if (!NativeBridge.isAvailable()) {
  // fallback for mobile browser
}
```

### `NativeBridge.request<T>(method, params?, opts?): Promise<T>`

Generic RPC. `opts.timeout` in milliseconds (default `15000`).

```javascript
const result = await NativeBridge.request("bridge.getAppState", {});
```

### `NativeBridge.on(event, callback): () => void`

Subscribe to native events. Returns unsubscribe function.

```javascript
const off = NativeBridge.on("app.state", (state) => console.log(state));
off(); // unsubscribe
```

**Built-in events:** `api.call`, `notification`, `data`, `app.state`, `app.lifecycle`, `app.pip`, `app.network`, `app.orientation`, `app.webview`, `app.keyboard`, `app.safeArea`, `app.battery`, `app.audio`, `app.display`, `app.theme`, `app.locale`, `app.call`, `permission.change`, plus custom via `publishEvent`.

### `NativeBridge.send(event, payload?): void`

Fire-and-forget custom event from web → native. Payload can be any JSON-serializable structure (object, array, string, number, boolean, or `null`).

```javascript
NativeBridge.send("analytics.track", { name: "checkout", amount: 499 });
NativeBridge.send("navigation.request", { screen: "Profile", params: { id: 42 } });
```

**Android — receive all web events:**

```kotlin
NativeBridge.setWebEventListener { event, payload, webViewId, webView ->
    when (event) {
        "analytics.track" -> analytics.log(payload)
        else -> Log.d("Bridge", "$event from $webViewId: $payload")
    }
}

// Or per WebView:
bridgeWebView.setOnWebEvent { event, payload -> /* … */ }
```

**Android — send to web:**

```kotlin
NativeBridge.publishEvent("order.updated", JSONObject().put("id", orderId))
// or: bridgeWebView.publishEvent("order.updated", mapOf("id" to orderId))
```

**iOS — receive:**

```swift
NativeBridge.shared.setWebEventHandler { event, payload, webViewId, webView in
    // handle any structure
}
bridgeWebView.setOnWebEvent { event, payload in /* … */ }
```

**iOS — send:**

```swift
NativeBridge.shared.publishEvent("order.updated", ["id": orderId])
```

### `WEBVIEW_LOADED` (web → native, built-in)

Fired automatically when the page DOM is ready (`phase: "dom"`) and again when all resources finish (`phase: "complete"`). Includes `url`, `title`, `webViewId`, `timestamp`, and `readyState`.

**Web — automatic (no code required):**

```javascript
// Emitted twice per full page load: dom, then complete.
// Listen on native (below) or send manually for SPA routes:
router.afterEach((to) => {
  NativeBridge.notifyWebViewLoaded({ phase: "manual", route: to.path });
});
```

**Web — explicit:**

```javascript
NativeBridge.send(NativeBridge.EVENTS.WEBVIEW_LOADED, { route: "/checkout" });
// or:
await NativeBridge.notifyWebViewLoaded({ route: "/checkout" });
```

**Android — typed listener:**

```kotlin
NativeBridge.setOnWebViewLoaded { payload, webViewId, webView ->
    when (payload.phase) {
        WebViewLoadedPhase.DOM -> hideSplash()
        WebViewLoadedPhase.COMPLETE -> trackPageView(payload.url)
        WebViewLoadedPhase.MANUAL -> navigateFromSpa(payload.route)
    }
}

bridgeWebView.setOnWebViewLoaded { payload ->
    Log.d("Bridge", "loaded ${payload.url} (${payload.phase})")
}
```

**iOS — typed listener:**

```swift
NativeBridge.shared.setWebViewLoadedHandler { payload, webViewId, webView in
    if payload.phase == .complete {
        analytics.trackScreen(payload.url)
    }
}

bridgeWebView.setOnWebViewLoaded { payload in
    print("loaded \(payload.url)")
}
```

**React Native:**

```typescript
useEffect(() => {
  return NativeBridge.onWebViewLoaded((payload, webViewId) => {
    if (payload.phase === 'complete') hideSplash();
  });
}, []);
```

### `nativebridgeready` event

```javascript
window.addEventListener("nativebridgeready", () => initBridge());
```

---

## Data & device

### `getData(key: string): Promise<T>`

Read value from the shared app-wide store (native or any WebView).

### `getAllData(): Promise<Record<string, unknown>>`

All keys in the shared store.

### `setData(key: string, value: unknown): Promise<{ key, stored }>`

Write to the shared store. **All other WebViews** receive `onData` immediately.

```javascript
await NativeBridge.setData("checkoutCart", { items: [...], total: 499 });
```

### `removeData(key: string): Promise<{ key, removed }>`

Delete a key; all WebViews get `onData` with `removed: true`.

### `getWebViewId(): Promise<{ id: string }>`

Returns this WebView instance id (`wv_1`, `wv_2`, …). Useful when multiple WebViews share data.

### `onData(callback): () => void`

`{ key, value, source?, removed? }` when **any** WebView or native updates data.

`source` is `wv_1` / `wv_2` / `native`. See [DATA-SHARE.md](../DATA-SHARE.md).

### `getDeviceInfo(): Promise<DeviceInfo>`

```typescript
{ platform: "android" | "ios", model?, osVersion?, appPackage? | appBundleId? }
```

---

## API capture

### `getApiCalls(filter?): Promise<ApiCall[]>`

```javascript
await NativeBridge.getApiCalls({
  urlContains: "/api/v1",
  method: "POST",
  limit: 50,
});
```

### `onApiCall(callback): () => void`

Realtime stream of captured HTTP calls.

---

## Notifications

### `getNotifications(): Promise<NotificationRecord[]>`

### `onNotification(callback): () => void`

---

## App state

### `getAppState(): Promise<AppStateSnapshot>`

Full snapshot — lifecycle, PiP, network, orientation, webview, keyboard, safe area, battery, audio, display, theme, locale, call.

### Channel listeners

| Method | Payload |
|--------|---------|
| `onAppState(cb)` | Full `AppStateSnapshot` + `changed` |
| `onLifecycle(cb)` | `{ lifecycle, isForeground, timestamp }` |
| `onPiP(cb)` | `{ isInPiP, timestamp }` |
| `onNetwork(cb)` | `{ connected, type }` |
| `onOrientation(cb)` | `{ orientation, timestamp }` |
| `onWebViewState(cb)` | `{ visible, focused, url }` |
| `onKeyboard(cb)` | `{ visible, height }` |
| `onSafeArea(cb)` | `{ top, bottom, left, right }` |
| `onBattery(cb)` | `{ level, charging, lowPowerMode }` |
| `onAudio(cb)` | `{ route, bluetoothConnected, deviceName }` |
| `onDisplay(cb)` | `{ width, height, density/scale }` |
| `onTheme(cb)` | `{ darkMode }` |
| `onLocale(cb)` | `{ language, region, timezone }` |
| `onCall(cb)` | `{ inCall, state }` |

---

## Permissions

### `NativeBridge.Permissions`

```javascript
NativeBridge.Permissions.CAMERA        // "camera"
NativeBridge.Permissions.MICROPHONE    // "microphone"
NativeBridge.Permissions.LOCATION
NativeBridge.Permissions.NOTIFICATIONS
NativeBridge.Permissions.PHOTOS
NativeBridge.Permissions.BLUETOOTH
NativeBridge.Permissions.CONTACTS
```

### `getPermissionStatus(permission): Promise<PermissionResult>`

```typescript
{ permission, status: "granted"|"denied"|"blocked"|"unavailable", canAskAgain }
```

### `requestPermission(permission): Promise<PermissionResult>`

Shows system permission dialog.

### `getPermissions(): Promise<PermissionResult[]>`

All supported permissions at once.

### `openSettings(): Promise<{ opened: boolean }>`

Opens app settings (when permission is blocked).

### `onPermissionChange(callback): () => void`

---

## Payments (India UPI)

### `getUpiApps(): Promise<PaymentApp[]>`

Installed UPI handlers only.

### `getPaymentApps(): Promise<PaymentAppsSnapshot>`

```typescript
{ upi, wallets, messaging, all, platform }
```

### `openUpiPayment(params): Promise<OpenUpiPaymentResult>`

| Param | Aliases | Required |
|-------|---------|----------|
| Payee VPA | `vpa`, `pa` | Yes |
| Amount | `amount`, `am` | No |
| Payee name | `name`, `pn` | No |
| Note | `note`, `tn` | No |
| Transaction id | `txnId`, `tr` | No |
| Currency | `currency`, `cu` | Default `INR` |

### `buildUpiUri(params): Promise<{ uri: string }>`

Build `upi://pay?...` without opening.

### `canOpenUrl(url): Promise<{ url, canOpen }>`

### `openUrl(url): Promise<{ opened, url, error? }>`

---

## Intents & deep links

### `launchIntent(params): Promise<LaunchIntentResult>`

**Android:**

```javascript
await NativeBridge.launchIntent({
  action: "android.intent.action.VIEW",
  data: "upi://pay?pa=shop@upi&am=100",
  package: "com.phonepe.app",
  chooser: true,
  chooserTitle: "Pay with",
  extras: { "android.intent.extra.TEXT": "Hello" },
});
```

**iOS:** maps to `UIApplication.open` — use `url` or `data`.

### `queryIntents(params): Promise<IntentHandler[]>`

```javascript
await NativeBridge.queryIntents({ data: "upi://pay" });
```

---

## Native bridge methods (internal contract)

Web SDK maps to these `bridge.*` methods:

| Method | Sync/async |
|--------|------------|
| `bridge.getApiCalls` | sync |
| `bridge.getNotifications` | sync |
| `bridge.getData` | sync |
| `bridge.getAllData` | sync |
| `bridge.setData` | sync |
| `bridge.removeData` | sync |
| `bridge.getWebViewId` | sync |
| `bridge.getDeviceInfo` | sync |
| `bridge.getAppState` | sync |
| `bridge.getPermissionStatus` | async |
| `bridge.requestPermission` | async |
| `bridge.getPermissions` | async |
| `bridge.openSettings` | sync |
| `bridge.getUpiApps` | sync |
| `bridge.getPaymentApps` | sync |
| `bridge.canOpenUrl` | sync |
| `bridge.openUrl` | sync |
| `bridge.launchIntent` | sync |
| `bridge.queryIntents` | sync |
| `bridge.openUpiPayment` | sync |
| `bridge.buildUpiUri` | sync |

---

## Error handling

```javascript
try {
  const apps = await NativeBridge.getUpiApps();
} catch (e) {
  // NativeBridge timeout: bridge.getUpiApps
  // or native rejection message
}
```

Always guard with `isAvailable()` for browser fallback.

---

## React Native module

Same methods via `import { NativeBridge } from 'native-webview-bridge-react-native'`:

```typescript
await NativeBridge.start();
await NativeBridge.getUpiApps();
await NativeBridge.launchIntent({ data: 'tel:+911234567890' });
```
