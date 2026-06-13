# Native WebView Bridge — Documentation

**License:** [MIT](../LICENSE) — free for commercial and personal use. [Contributing](../CONTRIBUTING.md) · [GitHub](https://github.com/s3roy/native-bridge)

Technical documentation for engineering teams adopting **native-webview-bridge** in production mobile applications.

---

## Document index

| Document | Audience | Purpose |
|----------|----------|---------|
| [FEATURES.md](./FEATURES.md) | Product · Engineering · Sales | Complete feature catalog and platform parity matrix |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Architects · Senior engineers | System design, message protocol, component boundaries |
| [API-REFERENCE.md](./API-REFERENCE.md) | Web · Mobile engineers | Full Web SDK and native bridge method reference |
| [SECURITY.md](./SECURITY.md) | Security · Compliance · AppSec | Threat model, data handling, hardening checklist |
| [PRODUCTION-READINESS.md](./PRODUCTION-READINESS.md) | DevOps · Release managers | Go-live checklist, monitoring, versioning |
| [PLATFORM-MATRIX.md](./PLATFORM-MATRIX.md) | Tech leads | Android · iOS · React Native · Web comparison |
| [EXECUTIVE-BRIEF.md](./EXECUTIVE-BRIEF.md) | Sales · Sponsors · Leadership | One-page product overview for decks and RFPs |

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

---

## Executive summary

**native-webview-bridge** is a cross-platform SDK that connects **web content inside native WebViews** to **native device and app capabilities** without requiring web teams to ship a separate JavaScript bundle.

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
├── android/          Android library (AAR) — capture, WebView, permissions, payments
├── ios/              Swift Package — capture, WebView, permissions, payments
├── react-native/     RN wrapper — BridgeWebView + native module
├── bridge-js/        Web SDK reference + TypeScript types (injected by native)
└── docs/             Enterprise documentation (this folder)
```

---

## Versioning & support model (recommended for MNC rollout)

| Tier | Scope | Suggested SLA |
|------|-------|---------------|
| **L1 — Web SDK** | `NativeBridge.*` API stability, TypeScript types | Semver; breaking changes major version only |
| **L2 — Native integration** | `BridgeWebView`, install steps | Documented in INSTALL.md per release |
| **L3 — Platform parity** | Feature matrix in PLATFORM-MATRIX.md | Gaps documented; roadmap for new OS APIs |

**Recommended release tagging:** `v1.x.y` aligned across `android/`, `ios/`, and `react-native/`.

---

## Quick adoption path (enterprise)

```
Week 1   POC — BridgeWebView + getDeviceInfo + getAppState
Week 2   Auth bridge — putData / getData for tokens
Week 3   Permissions + UPI checkout (if India)
Week 4   Security review (SECURITY.md) + production checklist
```

For a full go-live gate, use [PRODUCTION-READINESS.md](./PRODUCTION-READINESS.md).
