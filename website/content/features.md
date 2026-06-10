# Feature catalog

Complete list of capabilities delivered by **native-webview-bridge**, organized by product domain.

**Legend:** ✅ Supported · ⚠️ Partial / platform-specific · 🔧 Requires host setup · 📡 Realtime push

---

## 1. Core bridge

| Feature | Description | Web API | Android | iOS | RN |
|---------|-------------|---------|---------|-----|-----|
| Auto-injected Web SDK | No web bundle; SDK injected at document start | ✅ | ✅ | ✅ | ✅ |
| Availability check | Detect native host vs browser | `isAvailable()` | ✅ | ✅ | ✅ |
| Generic RPC | Call any registered native method | `request(method, params)` | ✅ | ✅ | ✅ |
| Event bus | Subscribe to native → web events | `on(event, cb)` | ✅ | ✅ | WebView only |
| Request timeout | Configurable per call (default 15s) | `request(..., { timeout })` | ✅ | ✅ | ✅ |
| TypeScript contracts | Full `.d.ts` for web teams | ✅ | — | — | — |

---

## 2. Native data bridge

| Feature | Description | Web API | Android | iOS | RN |
|---------|-------------|---------|---------|-----|-----|
| Key-value store | Shared across all WebViews in the app | `getData` / `getAllData` | ✅ | ✅ | ✅ |
| Web → Web write | WebView A stores, WebView B reads | `setData` / `removeData` | ✅ | ✅ | ✅ |
| Multi-WebView sync | Live push to every attached WebView | `onData` 📡 | ✅ | ✅ | WebView only |
| WebView identity | Know which WebView wrote data | `getWebViewId` + `source` in `onData` | ✅ | ✅ | ✅ |
| Live data updates | Push when native `putData` changes | `onData` 📡 | ✅ | ✅ | WebView only |
| Device info | Platform, model, OS version, bundle id | `getDeviceInfo` | ✅ | ✅ | ✅ |
| Custom events | Native pushes arbitrary events | `on("custom.event")` 📡 | ✅ | ✅ | WebView only |

**Native publish (host app):**

```kotlin
NativeBridge.putData("authToken", token)
NativeBridge.publishEvent("order.updated", payload)
```

```swift
NativeBridge.shared.putData("authToken", token)
NativeBridge.shared.publishEvent("order.updated", payload)
```

---

## 3. Network capture (native API observability)

| Feature | Description | Web API | Android | iOS | RN |
|---------|-------------|---------|---------|-----|-----|
| HTTP call capture | Request/response stored in ring buffer | `getApiCalls` | ✅ 🔧 | ✅ 🔧 | ✅ |
| Live API stream | New calls pushed to web | `onApiCall` 📡 | ✅ | ✅ | WebView only |
| Filter by URL/method | Query captured traffic | `getApiCalls({ urlContains, method, limit })` | ✅ | ✅ | ✅ |

**Host setup required:**

- Android: `OkHttpClient.Builder().addInterceptor(BridgeInterceptor())`
- iOS: `NativeBridge.start()` in AppDelegate

**Captured fields:** method, URL, headers, body, status, response, duration, timestamp.

---

## 4. Notification capture

| Feature | Description | Web API | Android | iOS | RN |
|---------|-------------|---------|---------|-----|-----|
| Push notification history | Ring buffer of received notifications | `getNotifications` | ⚠️ FCM optional | ✅ | ✅ |
| Live notification stream | Push to web on arrival | `onNotification` 📡 | ⚠️ | ✅ | WebView only |

**Android:** optional `BridgeMessagingService` for FCM, or call `NativeBridge.recordNotification()` from existing service.

---

## 5. Realtime app state

All signals below support **pull** (`getAppState`) and **push** (`onAppState` or channel-specific listeners).

| Signal | Web listeners | Android | iOS | Use cases |
|--------|---------------|---------|-----|-----------|
| App lifecycle | `onLifecycle` | ✅ 📡 | ✅ 📡 | Pause video, sync on resume |
| Foreground / background | `onAppState` | ✅ | ✅ | Background task handling |
| Picture-in-Picture | `onPiP` | ✅ 📡 | ✅ 📡 | Compact UI in PiP |
| Network type | `onNetwork` | ✅ 📡 | ✅ 📡 | Offline banner, quality toggle |
| Orientation | `onOrientation` | ✅ 📡 | ✅ 📡 | Layout reflow |
| WebView visibility/focus/URL | `onWebViewState` | ✅ 📡 | ✅ 📡 | Pause when hidden |
| Keyboard height | `onKeyboard` | ✅ 📡 | ✅ 📡 | Scroll input into view |
| Safe area insets | `onSafeArea` | ✅ 📡 | ✅ 📡 | Notch / home indicator padding |
| Battery level/charging | `onBattery` | ✅ 📡 | ✅ 📡 | Low-power UI mode |
| Audio route (BT/wired) | `onAudio` | ✅ 📡 | ✅ 📡 | Headset indicators |
| Display size/density | `onDisplay` | ✅ 📡 | ✅ 📡 | Responsive layout |
| Dark mode | `onTheme` | ✅ 📡 | ✅ 📡 | Theme sync |
| Locale / timezone | `onLocale` | ✅ 📡 | ✅ 📡 | i18n |
| Phone call state | `onCall` | ✅ 📡 | ⚠️ | Mute chat during call |
| Low memory | `on("app.memory")` | ✅ 📡 | — | Reduce animations |

---

## 6. Permissions

| Permission | Web name | Android | iOS | Web API |
|------------|----------|---------|-----|---------|
| Camera | `camera` | ✅ | ✅ 🔧 | `getPermissionStatus`, `requestPermission` |
| Microphone | `microphone` | ✅ | ✅ 🔧 | same |
| Fine location | `location` | ✅ | ✅ 🔧 | same |
| Coarse location | `locationCoarse` | ✅ | — | same |
| Notifications | `notifications` | ✅ | ✅ | same |
| Photos / gallery | `photos` | ✅ | ✅ 🔧 | same |
| Bluetooth | `bluetooth` | ✅ | ✅ | same |
| Contacts | `contacts` | ✅ | ✅ 🔧 | same |
| Open app settings | — | ✅ | ✅ | `openSettings` |
| Permission change events | — | ✅ 📡 | ✅ 📡 | `onPermissionChange` |
| getUserMedia in WebView | — | ✅ | ✅ | Auto via BridgeWebView |

🔧 iOS host must add `Info.plist` usage description strings.

See [PERMISSIONS.md](../PERMISSIONS.md).

---

## 6b. Device hardware & system

| Feature | Web API | Android | iOS |
|---------|---------|---------|-----|
| Capability probe | `getCapabilities` | ✅ | ✅ |
| Current location | `getCurrentLocation` | ✅ | ✅ |
| Take photo | `takePhoto` | ✅ | ✅ |
| Pick gallery image | `pickImage` | ✅ | ✅ |
| Pick contact | `pickContact` | ✅ | ✅ |
| Clipboard read/write | `getClipboard` / `setClipboard` | ✅ | ✅ |
| Native share sheet | `share` | ✅ | ✅ |
| Vibrate / haptic | `vibrate` | ✅ | ✅ |
| Dial phone | `dial` | ✅ | ✅ |
| Send SMS | `sendSms` | ✅ | ✅ |
| Open maps | `openMaps` | ✅ | ✅ |
| Flashlight | `setTorch` | ✅ | ✅ |
| Permission helper | `ensurePermission` | ✅ | ✅ |
| Live camera in web | `getUserMedia` | ✅ | ✅ |

See [DEVICE.md](../DEVICE.md).

---

## 7. Payments & India UPI

| Feature | Description | Web API | Android | iOS |
|---------|-------------|---------|---------|-----|
| List installed UPI apps | PhonePe, GPay, Paytm, BHIM, … | `getUpiApps` | ✅ | ✅ 🔧 |
| Full payment app snapshot | UPI + wallets + messaging | `getPaymentApps` | ✅ | ✅ 🔧 |
| Open UPI payment | System chooser / default handler | `openUpiPayment` | ✅ | ✅ |
| Build UPI URI | Without opening | `buildUpiUri` | ✅ | ✅ |
| Check URL handler | Before showing button | `canOpenUrl` | ✅ | ✅ 🔧 |
| Open URL / deep link | tel:, mailto:, https:, app schemes | `openUrl` | ✅ | ✅ |

🔧 iOS: `LSApplicationQueriesSchemes` in host Info.plist.

See [PAYMENTS.md](../PAYMENTS.md).

---

## 8. Intents & deep links

| Feature | Description | Web API | Android | iOS |
|---------|-------------|---------|---------|-----|
| Launch Android Intent | VIEW, SEND, DIAL, custom | `launchIntent` | ✅ | — |
| Launch URL / deep link | Universal links, custom schemes | `launchIntent` / `openUrl` | ✅ | ✅ |
| Query intent handlers | Apps that can handle URI | `queryIntents` | ✅ | ⚠️ scheme probe |
| Force package | Open in specific app | `launchIntent({ package })` | ✅ | — |
| System chooser | Share / pay picker | `launchIntent({ chooser: true })` | ✅ | — |
| Intent extras | SEND text, etc. | `launchIntent({ extras })` | ✅ | — |

See [INTENTS.md](../INTENTS.md).

---

## 9. React Native

| Feature | Description |
|---------|-------------|
| `BridgeWebView` | Drop-in `react-native-webview` with SDK injection |
| `NativeBridge` module | Same API from RN JavaScript (no WebView required) |
| Message relay | WebView `postMessage` → native module → `injectJavaScript` resolve |
| Autolinking | iOS pod + Android package via `react-native.config.js` |

See [REACT-NATIVE.md](../react-native/REACT-NATIVE.md).

---

## 10. Roadmap candidates (not yet implemented)

Features commonly requested in enterprise RFPs — available as extension points via `request()` / `publishEvent()`:

| Feature | Extension approach |
|---------|-------------------|
| Biometric auth | Native handler for `bridge.authenticate` |
| Secure storage | Native handler wrapping Keychain / EncryptedSharedPreferences |
| File picker / camera roll | Native handler + permission bridge |
| Analytics forwarding | `publishEvent` → native analytics SDK |
| SSL pinning visibility | Extend `BridgeInterceptor` / URLProtocol |
| Method allowlist | Host-configurable dispatcher filter |

---

## Feature packaging for client proposals

### Starter tier
- BridgeWebView + device info + app state + data bridge

### Standard tier
- Starter + permissions + API capture + notifications

### India commerce tier
- Standard + UPI detection + payment intents

### Enterprise tier
- Full platform matrix + security review + custom `request()` handlers + RN support

Use [PLATFORM-MATRIX.md](./PLATFORM-MATRIX.md) for exact parity when scoping SOWs.
