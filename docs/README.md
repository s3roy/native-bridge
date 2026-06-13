# Native WebView Bridge — Documentation

**License:** [MIT](../LICENSE) — free for commercial and personal use. [Contributing](../CONTRIBUTING.md) · [GitHub](https://github.com/s3roy/native-webview-bridge)

Technical documentation for teams adopting **native-webview-bridge** in production mobile applications.

---

## Document index

| Document | Audience | Purpose |
|----------|----------|---------|
| [FEATURES.md](./FEATURES.md) | Engineering · Product | Complete feature catalog |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Architects · Senior engineers | System design, message protocol, component boundaries |
| [API-REFERENCE.md](./API-REFERENCE.md) | Web · Mobile engineers | Full Web SDK and native bridge method reference |
| [SECURITY.md](./SECURITY.md) | Security · AppSec | Threat model, data handling, hardening checklist |
| [PRODUCTION-READINESS.md](./PRODUCTION-READINESS.md) | DevOps · Release managers | Go-live checklist, monitoring, versioning |
| [PLATFORM-MATRIX.md](./PLATFORM-MATRIX.md) | Tech leads | Android · iOS · React Native · Web comparison |
| [EXECUTIVE-BRIEF.md](./EXECUTIVE-BRIEF.md) | Anyone new to the project | One-page open-source project overview |

## Integration guides (by stack)

| Guide | Location |
|-------|----------|
| Android & iOS native install | [../INSTALL.md](../INSTALL.md) |
| React Native | [../react-native/REACT-NATIVE.md](../react-native/REACT-NATIVE.md) |
| Permissions | [../PERMISSIONS.md](../PERMISSIONS.md) |
| UPI / payments (India) | [../PAYMENTS.md](../PAYMENTS.md) |
| Intents / deep links | [../INTENTS.md](../INTENTS.md) |
| Multi-WebView data sharing | [../DATA-SHARE.md](../DATA-SHARE.md) |
| Device APIs (location, camera, …) | [../DEVICE.md](../DEVICE.md) |
| Camera/gallery lifecycle & edge cases | [../DEVICE-LIFECYCLE.md](../DEVICE-LIFECYCLE.md) |
| Hardware back button → web | [../BACK-BUTTON.md](../BACK-BUTTON.md) |
| Safe area, status bar, notifications | [../SAFE-AREA.md](../SAFE-AREA.md) |
| WebView cache & Vercel bandwidth | [../CACHE.md](../CACHE.md) |
| TypeScript definitions | [../bridge-js/native-bridge.d.ts](../bridge-js/native-bridge.d.ts) |
| Changelog | [../CHANGELOG.md](../CHANGELOG.md) |
| Demo apps | [../examples/README.md](../examples/README.md) |
| Playground (web) | [../website/app/playground](../website/app/playground) |

---

## Summary

**native-webview-bridge** connects **web content inside native WebViews** to **native device and app capabilities** without requiring web teams to ship a separate JavaScript bundle. **Everything is MIT open source** — no paid tiers or feature gates.

### Value proposition

| Stakeholder | Benefit |
|-------------|---------|
| **Web team** | Zero npm dependency; `window.NativeBridge` is auto-injected |
| **Android team** | Drop-in `BridgeWebView` + one OkHttp interceptor line |
| **iOS team** | Swift Package + `NativeBridge.start()` + `BridgeWebView` |
| **React Native team** | `BridgeWebView` component + `NativeBridge` module |
| **Platform / architecture** | Single message protocol across Android, iOS, and RN |

### Supported host platforms

- Native Android (Kotlin, API 21+)
- Native iOS (Swift, iOS 13+)
- React Native (0.71+, via wrapper package)

### Design principles

1. **Web-first API** — one JavaScript surface (`NativeBridge`) on all platforms
2. **Minimal host integration** — capture and injection are built into the library
3. **Request/response + events** — pull snapshots or subscribe to realtime pushes
4. **Explicit security boundary** — web can only invoke declared bridge methods
5. **Type-safe web contracts** — TypeScript definitions shipped with the package

---

## Package structure

```
native-webview-bridge/
├── android/          Android library (AAR)
├── ios/              Swift Package
├── react-native/     RN wrapper — BridgeWebView + native module
├── bridge-js/        Web SDK reference + TypeScript types (injected by native)
├── examples/         Demo apps (Android, iOS, React Native)
├── website/          Docs site + interactive playground
└── docs/             Documentation (this folder)
```

---

## Versioning

Tag **`vX.Y.Z`** across `android/`, `ios/`, `react-native/`, and `bridge-js/` together. See [CHANGELOG.md](../CHANGELOG.md).

---

## Suggested adoption path

```
Week 1   POC — BridgeWebView + playground + getDeviceInfo + getAppState
Week 2   Auth bridge — putData / getData for tokens
Week 3   Permissions + UPI checkout (if India)
Week 4   Security review (SECURITY.md) + production checklist
```

For go-live, use [PRODUCTION-READINESS.md](./PRODUCTION-READINESS.md).
