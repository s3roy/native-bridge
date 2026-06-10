# React Native integration

Use **native-webview-bridge** in React Native with almost no native glue code.

## Install

```bash
npm install native-webview-bridge-react-native react-native-webview
# or yarn add ...
cd ios && pod install
```

### Android — link the core library

The RN module depends on the Android bridge library. Add both modules in **`settings.gradle`**:

```gradle
include ':native-webview-bridge'
project(':native-webview-bridge').projectDir = new File(
  rootProject.projectDir,
  '../node_modules/native-webview-bridge-react-native/../android'  // adjust path if monorepo
)

// Autolinking adds native-webview-bridge-react-native automatically.
// If using a local monorepo folder instead of npm:
project(':native-webview-bridge').projectDir = new File(rootProject.projectDir, '../native-webview-bridge/android')
```

**Monorepo layout** (this repo):

```gradle
include ':native-webview-bridge'
project(':native-webview-bridge').projectDir = new File(rootProject.projectDir, '../native-webview-bridge/android')
```

Sync Gradle. The bridge **auto-starts** on Android via `BridgeInitProvider`.

### iOS

Autolinking installs the pod. Call `NativeBridge.start()` once (see below). Add UPI / deep-link schemes to **Info.plist** if you use payment apps — see [PAYMENTS.md](../PAYMENTS.md).

---

## Usage

### 1. WebView with auto-injected `window.NativeBridge`

Your web app needs **zero npm packages** — the SDK is injected automatically.

```tsx
import { BridgeWebView, NativeBridge } from 'native-webview-bridge-react-native';
import { useEffect } from 'react';

export default function App() {
  useEffect(() => {
    NativeBridge.start(); // iOS: enable capture hooks
    NativeBridge.putData('authToken', 'abc123');
  }, []);

  return (
    <BridgeWebView
      source={{ uri: 'https://your-app.com/checkout' }}
      style={{ flex: 1 }}
    />
  );
}
```

Inside the loaded page:

```javascript
const apps = await NativeBridge.getUpiApps();
await NativeBridge.launchIntent({ data: 'upi://pay?pa=shop@upi&am=100' });
```

### 2. Call bridge APIs from React Native JS

Same API without a WebView:

```tsx
import { NativeBridge } from 'native-webview-bridge-react-native';

async function payWithUpi() {
  const apps = await NativeBridge.getUpiApps();
  console.log('Installed:', apps);

  await NativeBridge.openUpiPayment({
    vpa: 'merchant@paytm',
    amount: '499.00',
    name: 'My Shop',
  });
}

async function openMaps() {
  await NativeBridge.launchIntent({
    data: 'geo:19.0760,72.8777?q=Mumbai',
    chooser: true,
  });
}

async function dialSupport() {
  await NativeBridge.openUrl('tel:+1800123456');
}
```

### 3. Generic native method escape hatch

```tsx
const state = await NativeBridge.request('bridge.getAppState', {});
const handlers = await NativeBridge.request('bridge.queryIntents', {
  data: 'upi://pay',
});
```

---

## Live events (native ↔ web ↔ RN)

### Native → web + RN JS

Native pushes (`app.state`, `data`, `notification`, custom events) are relayed to:

1. **Mounted `BridgeWebView` instances** — injected as `window.NativeBridge.__emit`
2. **React Native JS** — via `NativeBridge.on()` and convenience helpers

```tsx
import { NativeBridge } from 'native-webview-bridge-react-native';
import { useEffect } from 'react';

useEffect(() => {
  const off = NativeBridge.onAppState((state) => {
    console.log('lifecycle', state);
  });
  return off;
}, []);

// Custom event from native Kotlin/Swift:
NativeBridge.publishEvent('order.updated', { id: 42 });
```

Inside the WebView page:

```javascript
NativeBridge.on('order.updated', (payload) => updateCart(payload));
```

### Web → RN JS (`NativeBridge.send`)

Fire-and-forget from web pages to your RN app:

```javascript
// Inside BridgeWebView page
NativeBridge.send('navigation.request', { screen: 'Settings' });
NativeBridge.send('analytics.track', { name: 'checkout_complete' });
```

```tsx
useEffect(() => {
  return NativeBridge.onWebEvent((event, payload) => {
    if (event === 'navigation.request') navigate(payload.screen);
  });
}, []);
```

---

## Realtime media in WebView

`BridgeWebView` enables camera and microphone capture for web pages that use standard browser media APIs.

**Defaults (no extra props required):**

- Android — `onPermissionRequest` grants capture when native camera/mic permissions are granted
- iOS — inline media playback + capture grant after permissions
- `domStorageEnabled` on Android (needed by many realtime media stacks)

**Typical flow in your web page:**

```javascript
await NativeBridge.ensurePermission('camera');
await NativeBridge.ensurePermission('microphone');
const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });

NativeBridge.onLifecycle(({ isForeground }) => {
  if (!isForeground) NativeBridge.stopMediaStream(stream);
});
NativeBridge.onAudio((route) => {
  /* react to speaker / headset / bluetooth route changes */
});
```

Disable auto media handling:

```tsx
<BridgeWebView mediaCapture={false} source={{ uri: 'https://…' }} />
```

Add **NSCameraUsageDescription** and **NSMicrophoneUsageDescription** to iOS Info.plist.

---

## What works in RN

| Feature | WebView (`window.NativeBridge`) | RN JS (`NativeBridge.*`) |
|---------|--------------------------------|---------------------------|
| UPI / payment apps | ✅ | ✅ |
| Launch intent / open URL | ✅ | ✅ |
| App state, permissions | ✅ | ✅ |
| API capture, notifications | ✅ (auto) | ✅ (read via `getApiCalls`) |
| Live events (`on`, `onAppState`, …) | ✅ | ✅ |
| Web → RN events (`send` / `onWebEvent`) | ✅ | ✅ |
| `publishEvent` (RN/native → web) | ✅ | ✅ |
| Realtime media (`getUserMedia`) | ✅ | — (web APIs in WebView) |

---

## TypeScript

Web types live in `bridge-js/native-bridge.d.ts`. Inside WebView pages, `window.NativeBridge` is typed globally when you reference that file in your web project.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `NativeWebViewBridge` is null | Rebuild app after install; check autolinking |
| Android `getUpiApps()` empty | Ensure `<queries>` merged from bridge manifest (Android 11+) |
| iOS always `installed: false` | Add schemes to `LSApplicationQueriesSchemes` |
| WebView `NativeBridge` undefined | Use `BridgeWebView`, not plain `WebView` |
| Gradle `:native-webview-bridge` not found | Add library to `settings.gradle` (see above) |
