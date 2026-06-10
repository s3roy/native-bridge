# Platform matrix

Side-by-side comparison for scoping, SOWs, and engineering estimates.

**Last updated:** package v1.0.0

---

## Integration effort

| Task | Android | iOS | React Native |
|------|---------|-----|--------------|
| Add dependency | Gradle module | Swift Package | npm + Gradle + pod |
| Minimum host code | `BridgeWebView` + optional interceptor | `NativeBridge.start()` + `BridgeWebView` | `NativeBridge.start()` + `BridgeWebView` |
| Auto-init | ✅ ContentProvider | Manual `start()` | Android auto / iOS manual |
| Web team work | None | None | None |
| Typical POC time | 0.5–1 day | 0.5–1 day | 1–2 days |

---

## Feature parity

| Feature | Android | iOS | RN WebView | RN JS |
|---------|:-------:|:---:|:----------:|:-----:|
| Auto-injected SDK | ✅ | ✅ | ✅ | — |
| `getData` / `putData` | ✅ | ✅ | ✅ | ✅ |
| `getDeviceInfo` | ✅ | ✅ | ✅ | ✅ |
| `getAppState` + push | ✅ | ✅ | ✅ | pull only |
| API capture | ✅¹ | ✅² | ✅ | ✅ |
| Notification capture | ⚠️³ | ✅ | ✅ | ✅ |
| Permissions | ✅ | ✅ | ✅ | ✅ |
| UPI apps list | ✅ | ✅⁴ | ✅ | ✅ |
| `openUpiPayment` | ✅ | ✅ | ✅ | ✅ |
| `launchIntent` (full) | ✅ | — | ✅ | ✅ |
| `openUrl` / deep links | ✅ | ✅ | ✅ | ✅ |
| `queryIntents` | ✅ | ⚠️⁵ | ✅ | ✅ |
| Custom `publishEvent` | ✅ | ✅ | ✅ | WebView only |

¹ Requires `BridgeInterceptor()` on OkHttp  
² Requires `NativeBridge.start()`  
³ Optional FCM service or manual `recordNotification`  
⁴ Requires `LSApplicationQueriesSchemes` in Info.plist  
⁵ iOS returns scheme probes, not full intent resolver list  

---

## OS requirements

| | Android | iOS |
|--|---------|-----|
| Min version | API 21 (5.0) | iOS 13 |
| Language | Kotlin | Swift |
| WebView | System WebView | WKWebView |
| RN min | 0.71 | 0.71 |

---

## Permissions parity

| Permission | Android | iOS |
|------------|:-------:|:---:|
| Camera | ✅ | ✅ |
| Microphone | ✅ | ✅ |
| Location (fine) | ✅ | ✅ |
| Location (coarse) | ✅ | — |
| Notifications | ✅ | ✅ |
| Photos | ✅ | ✅ |
| Bluetooth | ✅ | ✅ |
| Contacts | ✅ | ✅ |

---

## App state signals

| Signal | Android | iOS |
|--------|:-------:|:---:|
| Lifecycle | ✅ | ✅ |
| PiP | ✅ | ✅ |
| Network | ✅ | ✅ |
| Orientation | ✅ | ✅ |
| WebView state | ✅ | ✅ |
| Keyboard | ✅ | ✅ |
| Safe area | ✅ | ✅ |
| Battery | ✅ | ✅ |
| Audio route | ✅ | ✅ |
| Display | ✅ | ✅ |
| Theme | ✅ | ✅ |
| Locale | ✅ | ✅ |
| Phone call | ✅ | ⚠️ limited |
| Low memory | ✅ | — |

---

## Distribution

| Channel | Status | Consumer command |
|---------|--------|------------------|
| Local Gradle module | ✅ Ready | `implementation project(':native-webview-bridge')` |
| Maven Central | 🔲 Publish required | `implementation 'com.you:native-webview-bridge:1.0.0'` |
| Swift Package (local) | ✅ Ready | Xcode → Add Local Package → `ios/` |
| Swift Package (Git) | 🔲 Push + tag required | Xcode → Add Package → Git URL |
| npm (React Native) | 🔲 Publish required | `npm i native-webview-bridge-react-native` |

---

## Known gaps (document in proposals)

| Gap | Workaround |
|-----|------------|
| RN without `BridgeWebView` mounted | Live events still reach RN JS via `NativeBridge.on()`; web pages need `BridgeWebView` for `send` / `on` in web |
| iOS full intent query | Use `getUpiApps()` + `canOpenUrl` per scheme |
| Payment callback to web | App deep link + web route handler |
| API capture on custom HTTP clients | Wrap client or add interceptor manually |
| Biometrics / secure vault | Custom `bridge.*` handler in host app |

---

## Team ownership model (recommended)

| Layer | Owner |
|-------|-------|
| Web SDK usage & fallbacks | Frontend / Web platform |
| BridgeWebView integration | Mobile (Android/iOS) |
| OkHttp / URLSession capture | Mobile platform |
| Permissions & Info.plist | Mobile + compliance |
| UPI checkout UX | Web + payments team |
| Security review | AppSec |
| RN wrapper | Mobile platform or RN guild |
