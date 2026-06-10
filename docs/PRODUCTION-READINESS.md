# Production readiness checklist

Enterprise go-live gate for **native-webview-bridge** deployments.

---

## Phase 1 — Integration (dev)

### Android
- [ ] `native-webview-bridge` module in `settings.gradle`
- [ ] `implementation project(':native-webview-bridge')` in app module
- [ ] `BridgeWebView` replaces `WebView` in production screens
- [ ] `BridgeInterceptor()` added to production OkHttp client (if API capture required)
- [ ] Gradle sync clean; release build succeeds
- [ ] Merged manifest reviewed (permissions from library)

### iOS
- [ ] Swift Package added (`ios/` folder or Git URL)
- [ ] `NativeBridge.start()` in `AppDelegate` / `@main` App
- [ ] `BridgeWebView` used in production view controllers
- [ ] `Info.plist` usage descriptions for all requested permissions
- [ ] `LSApplicationQueriesSchemes` for UPI (if India payments)
- [ ] Release archive succeeds

### React Native
- [ ] `native-webview-bridge-react-native` + `react-native-webview` installed
- [ ] Android core library linked in `settings.gradle`
- [ ] `pod install` completed
- [ ] `NativeBridge.start()` called at app launch
- [ ] `BridgeWebView` used instead of plain `WebView`

### Web
- [ ] `NativeBridge.isAvailable()` guard for browser fallback
- [ ] `nativebridgeready` listener for early init
- [ ] `native-bridge.d.ts` referenced for TypeScript (optional)
- [ ] No assumption bridge exists in desktop browser

---

## Phase 2 — Functional QA

| Test case | Expected |
|-----------|----------|
| Cold start → load web URL | `NativeBridge` defined within 1s |
| `getDeviceInfo()` | Returns platform, model, OS version |
| `getAppState()` | Valid lifecycle + network |
| Background app | `onLifecycle` fires `background` |
| Resume app | `onLifecycle` fires `active` |
| Rotate device | `onOrientation` fires |
| Open keyboard in web form | `onKeyboard` height > 0 |
| `putData` from native | `getData` + `onData` on web |
| Request camera (if used) | System dialog; `granted` or `denied` |
| `getUpiApps()` (India) | Lists installed apps on real device |
| `openUpiPayment()` | External UPI app opens |
| Logout | Native clears `putData` keys |

---

## Phase 3 — Security & compliance

See [SECURITY.md](./SECURITY.md).

- [ ] WebView URL allowlist enforced
- [ ] HTTPS only for remote content
- [ ] Tokens not logged in API capture
- [ ] Privacy policy updated (device signals, permissions)
- [ ] AppSec sign-off on bridge surface area
- [ ] Pen test includes WebView XSS → bridge abuse scenario

---

## Phase 4 — Performance

| Metric | Target |
|--------|--------|
| SDK inject time | < 50ms (negligible vs page load) |
| `getAppState()` latency | < 100ms |
| Memory (API ring buffer) | Bounded ~200 entries |
| App state push frequency | Debounced per channel |

- [ ] API capture ring buffer size acceptable for app memory budget
- [ ] No bridge calls on every scroll/frame (use events instead)

---

## Phase 5 — Observability

### Recommended logging (host app)

```kotlin
// Android — wrap critical bridge failures
BridgeWebView + custom WebViewClient logging navigation errors
```

```swift
// iOS — WKNavigationDelegate error logging
```

### Web-side

```javascript
NativeBridge.request("bridge.getAppState").catch((e) => {
  analytics.track("bridge_error", { method: "getAppState", message: e.message });
});
```

- [ ] Bridge errors tracked in analytics (non-PII)
- [ ] Crash reporting includes WebView version (Android System WebView)

---

## Phase 6 — Release management

- [ ] Version pinned across Android AAR, iOS SPM tag, RN npm version
- [ ] CHANGELOG entry for bridge version bump
- [ ] Rollback plan: web fallback when `!isAvailable()`
- [ ] Staged rollout (5% → 25% → 100%)
- [ ] Feature flags for UPI / permissions if regional

---

## Phase 7 — Documentation handoff

Deliver to client / internal teams:

| Document | Purpose |
|----------|---------|
| [docs/README.md](./README.md) | Documentation index |
| [FEATURES.md](./FEATURES.md) | Feature catalog for stakeholders |
| [API-REFERENCE.md](./API-REFERENCE.md) | Engineer reference |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Architecture review |
| [SECURITY.md](./SECURITY.md) | AppSec sign-off |
| [PLATFORM-MATRIX.md](./PLATFORM-MATRIX.md) | Parity & scoping |
| [INSTALL.md](../INSTALL.md) | Integration steps |
| `native-bridge.d.ts` | Web TypeScript types |

---

## Sign-off template

```
Project: _______________________
Bridge version: _______________
Environment: Production

Android lead:  _______________  Date: _______
iOS lead:      _______________  Date: _______
Web lead:      _______________  Date: _______
AppSec:        _______________  Date: _______
QA:            _______________  Date: _______
Release mgr:   _______________  Date: _______
```

---

## Rollback procedure

1. Web: all bridge calls already guarded by `isAvailable()` — browser path works
2. Native: revert to standard `WebView` / `WKWebView` (loses bridge, app still loads web)
3. No server-side dependency — rollback is client-only
