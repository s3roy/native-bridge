# native-webview-bridge

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)
[![GitHub](https://img.shields.io/github/stars/s3roy/native-bridge?style=social)](https://github.com/s3roy/native-bridge)

**Free and open source (MIT).** Use commercially without permission or royalties — see [LICENSE](./LICENSE). Contributions welcome: [CONTRIBUTING.md](./CONTRIBUTING.md).

A drop-in bridge so your **web pages running inside native Android/iOS WebViews**
can request, on demand:

- **API call responses** made by the native app (auto-captured)
- **Notifications** (push / local) the app received
- **Any data** the app published (auth token, user id, etc.)
- **Device info**
- **Realtime app state** (foreground/background, PiP, network, orientation, WebView visibility)
- **UPI / payment apps** — detect PhonePe, GPay, Paytm, etc. and open UPI payments from the web checkout

The native side writes **almost no code** — capture, storage, and the web SDK
injection are all built into the package. Web pages bundle **nothing**; the SDK
is auto-injected into every page.

**Documentation:** [docs/README.md](./docs/README.md) · [Overview](./docs/EXECUTIVE-BRIEF.md) · [Features](./docs/FEATURES.md) · [Install](./INSTALL.md) · [Production checklist](./docs/PRODUCTION-READINESS.md) · [Changelog](./CHANGELOG.md) · [MIT License](./LICENSE)

**Marketing site:** [website/](./website/) — professional landing page (Next.js, deploy to Vercel).

> Reality check: a true *zero-line* integration is impossible (the OS requires
> something to own the WebView and the network layer). This package gets it to
> **1 line on iOS** and **a drop-in WebView + 1 interceptor line on Android**.

---

## How it works

```
   Web page                Native (auto-capture)
 ┌───────────┐           ┌──────────────────────┐
 │NativeBridge│  request │  HTTP client hook     │ ← every API call
 │  .getApi…  ├──────────►  Notification hook    │ ← every notification
 │  .onNotif… │◄─────────┤  Key/value store      │ ← app-published data
 └───────────┘   emit    └──────────────────────┘
   (auto-injected SDK)        (singleton stores)
```

- **Web → Native:** `{ type, id, method, params }` over the platform message channel.
- **Native → Web:** `__resolve(id, result)`, `__reject(id, err)`, `__emit(event, payload)`.

---

## Web usage (identical on both platforms)

Nothing to install. Inside the WebView, `window.NativeBridge` already exists.

```js
// 1. Responses of API calls the native app made
const calls = await NativeBridge.getApiCalls({ urlContains: "/chat", limit: 20 });
console.log(calls[0].status, calls[0].responseBody);

// 2. Live stream as new calls complete
NativeBridge.onApiCall((call) => console.log("native API:", call.url, call.status));

// 3. Notifications
const notes = await NativeBridge.getNotifications();
NativeBridge.onNotification((n) => renderToast(n.title, n.body));

// 4. Any data the app published (token, user, …)
const token = await NativeBridge.getData("authToken");

// Share data between two WebViews in the same app
await NativeBridge.setData("checkoutCart", { items: [...] });
NativeBridge.onData(({ key, value, source }) => { /* WebView B reacts */ });

// 5. Device info
const device = await NativeBridge.getDeviceInfo();

// 6. Generic escape hatch (any custom native method)
const x = await NativeBridge.request("some.custom.method", { foo: 1 });

// Guard for running in a plain browser
if (!NativeBridge.isAvailable()) { /* fall back to normal fetch */ }
```

### Realtime app state (auto-pushed — no native code)

Native **pushes** updates the moment anything changes. Web can also pull a snapshot.

```js
// Current snapshot (lifecycle, PiP, network, orientation, webview)
const state = await NativeBridge.getAppState();
console.log(state.isForeground, state.isInPiP, state.network.type);

// Live — fires on ANY change (check state.changed: "pip" | "lifecycle" | …)
NativeBridge.onAppState((state) => {
  if (state.isInPiP) {
    // user is watching your WebView in Picture-in-Picture
    enterCompactUi();
  }
  if (!state.isForeground) {
    pauseVideo();
  }
});

// Or subscribe to specific channels
NativeBridge.onLifecycle(({ lifecycle, isForeground }) => { /* active | inactive | background */ });
NativeBridge.onPiP(({ isInPiP }) => { /* true when PiP active */ });
NativeBridge.onNetwork(({ connected, type }) => { /* wifi | cellular | none */ });
NativeBridge.onOrientation(({ orientation }) => { /* portrait | landscape */ });
NativeBridge.onWebViewState(({ visible, url }) => { /* webview hidden/shown */ });

// Custom realtime events from native (call state, typing, etc.)
NativeBridge.on("call.ringing", (payload) => showIncomingCall(payload));
```

**Everything tracked automatically (realtime push):**

| Signal | Web listener | Use case |
|--------|--------------|----------|
| Foreground / background | `onLifecycle` | pause video, sync |
| Picture-in-Picture | `onPiP` | compact chat UI |
| Network wifi/cellular/off | `onNetwork` | offline banner |
| Orientation | `onOrientation` | layout |
| WebView visible/focused/url | `onWebViewState` | pause when hidden |
| **Keyboard open + height** | `onKeyboard` | scroll input into view |
| **Safe area insets** | `onSafeArea` | notch / home bar padding |
| **Battery level/charging** | `onBattery` | low-battery mode |
| **Audio route (BT/headphones)** | `onAudio` | route call audio |
| Screen size / density | `onDisplay` | responsive layout |
| Dark mode | `onTheme` | theme sync |
| Locale / timezone | `onLocale` | i18n |
| **Phone call state** | `onCall` | mute chat during call |
| Low memory | `on("app.memory")` | reduce animations |

```js
// Keyboard — adjust chat input bar
NativeBridge.onKeyboard(({ visible, height }) => {
  document.body.style.paddingBottom = visible ? height + "px" : "0";
});

// Safe area — pad your header/footer
NativeBridge.onSafeArea(({ top, bottom }) => {
  document.documentElement.style.setProperty("--safe-top", top + "px");
  document.documentElement.style.setProperty("--safe-bottom", bottom + "px");
});

// Bluetooth headset connected
NativeBridge.onAudio(({ route, bluetoothConnected, deviceName }) => {
  if (route === "bluetooth") showHeadsetIcon(deviceName);
});

// Phone call in progress — pause notifications sound
NativeBridge.onCall(({ inCall, state }) => {
  if (inCall) muteChatSounds();
});
```

**Full `AppStateSnapshot`:**

```ts
{
  lifecycle, isForeground, isInPiP,
  network: { connected, type },
  orientation,
  webView: { visible, focused, url },
  keyboard: { visible, height },
  safeArea: { top, bottom, left, right },
  battery: { level, charging, lowPowerMode },
  audio: { route, bluetoothConnected, deviceName },
  display: { width, height, density/scale },
  theme: { darkMode },
  locale: { language, region, timezone },
  call: { inCall, state },
  timestamp,
  changed: "keyboard" | "pip" | "audio" | …
}
```

**Push custom events from native (optional):**

```kotlin
// Android
NativeBridge.publishEvent("call.ringing", JSONObject().put("from", "+91…"))
```

```swift
// iOS
NativeBridge.shared.publishEvent("call.ringing", ["from": "+91…"])
```

TypeScript types: `bridge-js/native-bridge.d.ts`.

### UPI & payment apps (India checkout)

Detect installed UPI apps and render them in your WebView checkout:

```js
const apps = await NativeBridge.getUpiApps();
// [{ id: "phonepe", name: "PhonePe", installed: true, … }, …]

await NativeBridge.openUpiPayment({
  vpa: "merchant@upi",
  amount: "499.00",
  name: "My Shop",
  note: "Order #123",
});
```

See **[PAYMENTS.md](./PAYMENTS.md)** for full API, iOS `Info.plist` setup, and a checkout UI example.

### Launch any intent / deep link

```js
await NativeBridge.launchIntent({
  data: "upi://pay?pa=merchant@upi&am=100&cu=INR",
  package: "com.phonepe.app",  // Android — optional
  chooser: true,
});

await NativeBridge.openUrl("tel:+919876543210");
const handlers = await NativeBridge.queryIntents({ data: "upi://pay" });
```

See **[INTENTS.md](./INTENTS.md)** for Android Intent params and share/dial examples.

### React Native

```tsx
import { BridgeWebView, NativeBridge } from 'native-webview-bridge-react-native';

await NativeBridge.start();
<BridgeWebView source={{ uri: 'https://app.example.com' }} />
```

See **[react-native/REACT-NATIVE.md](./react-native/REACT-NATIVE.md)**.

### Device APIs (location, camera, everything)

```js
await NativeBridge.ensurePermission("location");
const pos = await NativeBridge.getCurrentLocation({ enableHighAccuracy: true });

await NativeBridge.ensurePermission("camera");
const photo = await NativeBridge.takePhoto({ quality: 80 });

const contact = await NativeBridge.pickContact();
await NativeBridge.share({ text: "Hello", url: "https://..." });
```

See **[DEVICE.md](./DEVICE.md)** for full list: gallery, clipboard, vibrate, dial, SMS, maps, torch.

---

## Android integration

**1. Add the dependency** (the library auto-initializes via a ContentProvider —
no `Application` code needed).

```gradle
dependencies {
    implementation 'com.bridge:native-webview-bridge:1.0.0'
}
```

**2. Use the drop-in WebView** (replaces `WebView`):

```xml
<com.bridge.BridgeWebView
    android:id="@+id/web"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
findViewById<BridgeWebView>(R.id.web).loadUrl("https://app.example.com")
```

> Already have a `WebView`? Call `NativeBridge.attach(webView)` and re-inject in
> your `WebViewClient.onPageStarted` with `NativeBridge.inject(view)`.

**3. Capture API calls — add ONE line to your OkHttp/Retrofit client:**

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(BridgeInterceptor())
    .build()
```

That single interceptor captures every request + response for the web side.

**4. (Optional) Capture FCM push notifications** — register the provided service
in `AndroidManifest.xml`:

```xml
<service
    android:name="com.bridge.BridgeMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Already have a messaging service? Just call
`NativeBridge.recordNotification(...)` from your `onMessageReceived`.

**5. (Optional) Publish custom data:**

```kotlin
NativeBridge.putData("authToken", token)
NativeBridge.putData("userId", user.id)
```

---

## iOS integration

**1. Add the Swift Package** (`File ▸ Add Packages…`) pointing at this repo, or:

```swift
.package(url: "https://github.com/s3roy/native-bridge", from: "1.0.0")
```

**2. One line in `AppDelegate`** to enable automatic network + notification capture:

```swift
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
    NativeBridge.start()   // ← the only required line
    return true
}
```

`NativeBridge.start()` swizzles `URLSession` (captures all API calls) and the
notification-center delegate (captures notifications). No per-call code.

**3. Use the drop-in WebView:**

```swift
let web = BridgeWebView(frame: view.bounds)
web.load(URLRequest(url: URL(string: "https://app.example.com")!))
view.addSubview(web)
```

> Already have a `WKWebView`? Call `NativeBridge.shared.attach(webView)`.

**4. (Optional) Publish custom data:**

```swift
NativeBridge.shared.putData("authToken", token)
```

---

## What "on demand" means

You don't pre-declare data types. The web page asks for whatever it needs at
runtime through `getApiCalls` / `getNotifications` / `getData` / `request`, and
native answers from its always-on capture stores. Adding a new capability later
is just one more native method — the web contract doesn't change.

---

## Captured `ApiCall` shape

```ts
{
  id, method, url,
  requestHeaders, requestBody,
  status, responseHeaders, responseBody,
  durationMs, timestamp, error
}
```

`NotificationRecord`: `{ id, title, body, data, timestamp }`.

---

## Security notes (read before shipping)

- The bridge is exposed to **every page** the WebView loads. **Only load trusted
  origins**, or gate sensitive methods by checking the current URL host.
- API capture includes **headers and bodies** — they may contain tokens. Don't
  expose the WebView to third-party/remote content you don't control.
- Consider redacting `Authorization` headers in `BridgeInterceptor` /
  `BridgeURLProtocol` before storing if your web origin is not fully trusted.
- iOS uses method swizzling (`URLSession`, notification delegate). This is
  App-Store-safe (public APIs) but test against your networking stack.

---

## File map

```
native-webview-bridge/
├── bridge-js/
│   ├── native-bridge.js      # reference copy of the auto-injected SDK
│   └── native-bridge.d.ts    # TypeScript types
├── android/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml          # auto-init ContentProvider
│       └── java/com/bridge/
│           ├── NativeBridge.kt          # singleton, attach, dispatch, emit
│           ├── BridgeWebView.kt         # drop-in WebView
│           ├── BridgeInterceptor.kt     # OkHttp auto-capture (1 line to add)
│           ├── BridgeMessagingService.kt# optional FCM capture
│           ├── BridgeInitProvider.kt    # zero-config init
│           ├── BridgeScript.kt          # injected web SDK
│           └── Models.kt                # ApiCall, NotificationRecord, stores
└── ios/
    ├── Package.swift
    └── Sources/NativeWebViewBridge/
        ├── NativeBridge.swift           # singleton, attach, dispatch, emit
        ├── BridgeWebView.swift          # drop-in WKWebView
        ├── BridgeURLProtocol.swift      # URLSession auto-capture (swizzle)
        ├── NotificationCapture.swift    # notification auto-capture (swizzle)
        ├── BridgeScript.swift           # injected web SDK
        └── Models.swift                 # ApiCall, NotificationRecord, buffers
```

---

## Integration effort summary

| Platform | Required steps |
|----------|----------------|
| **Web**  | none (SDK auto-injected) |
| **iOS**  | add package + `NativeBridge.start()` + use `BridgeWebView` |
| **Android** | add dependency + use `BridgeWebView` + `addInterceptor(BridgeInterceptor())` |

Everything else (capture, storage, events, SDK injection) is automatic.

---

## License

[MIT License](./LICENSE) — Copyright (c) 2026 Souvik Roy. Free for personal, commercial, and closed-source use. Keep the copyright notice when you redistribute the software.
