# Native WebView Bridge — Project overview

**One-page summary of the open-source project.**

| | |
|---|---|
| **Product** | native-webview-bridge (NativeBridge) |
| **License** | [MIT](../LICENSE) — free for personal and commercial use |
| **Category** | Cross-platform WebView ↔ Native SDK |
| **Platforms** | Android · iOS · React Native |
| **Web impact** | Zero npm install — SDK auto-injected |
| **Repository** | [github.com/s3roy/native-bridge](https://github.com/s3roy/native-bridge) |

---

## Problem

Apps increasingly ship **web UI inside native WebViews** (checkout, chat, dashboards). That often means:

- Duplicate integration work on Android, iOS, and React Native
- Custom JS bridges maintained per team
- Web teams bundling platform-specific SDKs
- Slow rollout of device features (permissions, payments, app state)

---

## Solution

**native-webview-bridge** exposes native capabilities to web pages through one JavaScript API: `window.NativeBridge`.

```
┌──────────────┐     single API      ┌─────────────────────┐
│  Web pages   │ ◄──────────────────► │  Native app (A/iOS) │
│  (no bundle) │   request + events   │  BridgeWebView      │
└──────────────┘                      └─────────────────────┘
```

**Integration footprint:** drop-in `BridgeWebView` + optional OkHttp interceptor (Android) or `NativeBridge.start()` (iOS).

---

## What's included (everything, MIT)

| Domain | What web can do |
|--------|-----------------|
| **Core bridge** | RPC, events, multi-WebView data store, `WEBVIEW_LOADED` |
| **App state** | Foreground/background, PiP, network, keyboard, battery, theme — realtime |
| **Permissions** | Camera, mic, location, notifications from web UI |
| **Payments (India)** | UPI app detection, payment intents, URI builder |
| **Device** | Location, camera, gallery, clipboard, share, torch, maps |
| **Observability** | Optional native HTTP and notification capture |
| **React Native** | `BridgeWebView` + `NativeBridge` module — same web API |

No feature tiers. See [FEATURES.md](./FEATURES.md) and [PLATFORM-MATRIX.md](./PLATFORM-MATRIX.md).

---

## Platform support

| | Android | iOS | React Native |
|--|:-------:|:---:|:------------:|
| Drop-in WebView | ✅ | ✅ | ✅ |
| Auto-injected web SDK | ✅ | ✅ | ✅ |
| UPI / payments | ✅ | ✅ | ✅ |
| Full intent launch | ✅ | URL-based | ✅ |
| API capture | ✅¹ | ✅² | ✅ |

¹ OkHttp interceptor · ² `NativeBridge.start()` in AppDelegate

---

## Security & production

- Fixed API surface — web cannot execute arbitrary native code
- Load **trusted origins only** in the WebView
- [SECURITY.md](./SECURITY.md) · [PRODUCTION-READINESS.md](./PRODUCTION-READINESS.md)

---

## Get started

1. Clone [github.com/s3roy/native-bridge](https://github.com/s3roy/native-bridge)
2. Follow [INSTALL.md](../INSTALL.md) for your platform
3. Open the [playground](https://native-webview-bridge.vercel.app/playground) in `BridgeWebView` to test APIs
4. Read [docs/README.md](./README.md) for the full index

**Contributing:** [CONTRIBUTING.md](../CONTRIBUTING.md)

---

*native-webview-bridge · MIT open source*
