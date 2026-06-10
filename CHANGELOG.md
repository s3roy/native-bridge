# Changelog

All notable changes to **native-webview-bridge** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Version alignment:** Tag `vX.Y.Z` applies to `android/`, `ios/`, `react-native/`, and `bridge-js/` together.

---

## [Unreleased]

### Added
- React Native live event relay — `NativeBridge.on()`, `onAppState`, `onData`, etc. via `NativeEventEmitter`
- React Native web → native events — `NativeBridge.send` in WebView + `NativeBridge.onWebEvent()` in RN JS
- React Native `publishEvent()` — push custom events to mounted WebViews
- `NativeBridge.addEmitListener` on Android/iOS core for host integrations
- `BridgeWebView` realtime media defaults — camera/mic capture grant, `domStorageEnabled`, inline playback

### Changed
- (nothing yet)

### Fixed
- (nothing yet)

### Security
- (nothing yet)

---

## [1.0.0] - 2026-06-10

### Added

#### Core bridge
- Auto-injected web SDK (`window.NativeBridge`) for Android, iOS, and React Native WebView
- Request/response protocol with configurable timeout (default 15s)
- Event bus (`on` / `__emit`) for native → web push
- Generic RPC escape hatch (`request(method, params)`)
- TypeScript definitions (`bridge-js/native-bridge.d.ts`)

#### Data & device
- Native key-value store (`putData` / `getData` / `getAllData` / `onData`)
- Device info (`getDeviceInfo` — platform, model, OS version, bundle id)
- Custom event publishing from native (`publishEvent`)

#### Network & notifications
- HTTP API capture — Android `BridgeInterceptor` (OkHttp), iOS `BridgeURLProtocol` swizzle
- `getApiCalls` with URL/method/limit filters + `onApiCall` live stream
- Notification capture — iOS auto; Android optional FCM service or manual `recordNotification`
- `getNotifications` + `onNotification`

#### Realtime app state
- Full `getAppState` snapshot with channel-specific listeners
- Lifecycle, PiP, network, orientation, WebView visibility/focus/URL
- Keyboard height, safe area insets, battery, audio route, display, theme, locale
- Phone call state (Android); low-memory signal (Android)

#### Permissions
- `getPermissionStatus`, `requestPermission`, `getPermissions`, `openSettings`
- Supported: camera, microphone, location, notifications, photos, bluetooth, contacts
- Android transparent permission Activity; iOS WKUIDelegate for getUserMedia
- `onPermissionChange` realtime events

#### Payments (India UPI)
- `getUpiApps`, `getPaymentApps` — PhonePe, GPay, Paytm, BHIM, wallets
- `openUpiPayment`, `buildUpiUri`
- Android `<queries>` for UPI package visibility (API 30+)

#### Intents & deep links
- `canOpenUrl`, `openUrl`
- `launchIntent` — full Android Intent support; iOS URL open
- `queryIntents` — list handlers for URI / scheme probes

#### React Native
- `native-webview-bridge-react-native` package
- `BridgeWebView` component with SDK injection + message relay
- `NativeBridge` native module (same API from RN JavaScript)
- Shared `BridgeDispatcher` on Android

#### Documentation
- Enterprise docs: architecture, features, API reference, security, platform matrix, production readiness
- Integration guides: INSTALL, PERMISSIONS, PAYMENTS, INTENTS, REACT-NATIVE
- Executive brief for stakeholder / sales use

### Platform requirements
- Android: API 21+, Kotlin
- iOS: 13.0+, Swift, WKWebView
- React Native: 0.71+, react-native-webview 13+

### Known limitations
- React Native: live events (`onAppState`, etc.) delivered to WebView only; RN JS should poll or wrap EventEmitter
- iOS: `queryIntents` returns scheme probes, not full Android-style resolver list
- iOS UPI detection requires `LSApplicationQueriesSchemes` in host Info.plist
- Payment success/failure not returned by bridge — handle via backend or app deep links
- API capture not persisted across process death

---

## Release checklist (maintainers)

Before tagging `vX.Y.Z`:

- [ ] Update version in `react-native/package.json`
- [ ] Update `CHANGELOG.md` — move `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`
- [ ] Sync `BridgeScript.kt` / `BridgeScript.swift` with `bridge-js/native-bridge.js`
- [ ] Verify `native-bridge.d.ts` matches web SDK
- [ ] Run Android release build
- [ ] Run iOS archive / `swift build`
- [ ] Tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
- [ ] Publish (when ready): Maven / GitHub Packages / npm

---

## Version scheme

| Bump | When |
|------|------|
| **MAJOR** | Breaking change to `NativeBridge.*` web API or bridge method contracts |
| **MINOR** | New bridge methods, new app-state signals, backward-compatible features |
| **PATCH** | Bug fixes, docs, internal refactors with no API change |

---

[Unreleased]: https://github.com/you/native-webview-bridge/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/you/native-webview-bridge/releases/tag/v1.0.0
