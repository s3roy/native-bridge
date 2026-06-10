# Launching intents & deep links

Open any Android intent or iOS URL from your WebView (or React Native JS) — UPI, maps, dial, share, third-party apps.

## Web (inside WebView)

```javascript
// Open any URL / deep link
await NativeBridge.openUrl("https://maps.google.com/?q=Mumbai");
await NativeBridge.openUrl("tel:+919876543210");
await NativeBridge.openUrl("mailto:support@shop.com");

// Check before showing a button
const { canOpen } = await NativeBridge.canOpenUrl("phonepe://");

// Android — full Intent control
await NativeBridge.launchIntent({
  action: "android.intent.action.VIEW",
  data: "upi://pay?pa=merchant@upi&am=100&cu=INR",
  package: "com.phonepe.app",   // optional — force PhonePe
  chooser: true,                // optional — system picker
  chooserTitle: "Pay with",
});

// Share text (Android)
await NativeBridge.launchIntent({
  action: "android.intent.action.SEND",
  type: "text/plain",
  extras: { "android.intent.extra.TEXT": "Check out this product!" },
  chooser: true,
});

// List apps that handle an intent (e.g. all UPI handlers)
const handlers = await NativeBridge.queryIntents({
  action: "android.intent.action.VIEW",
  data: "upi://pay",
});
// Android: [{ packageName, name, activity }, …]
// iOS: [{ scheme, name, canOpen }, …]
```

### iOS notes

iOS has no generic Intent API. `launchIntent` opens URLs via `UIApplication.shared.open`. Add schemes to **Info.plist** → `LSApplicationQueriesSchemes` for `canOpenUrl` / detection to work.

```javascript
// iOS — same API, maps to URL open
await NativeBridge.launchIntent({ url: "phonepe://upi/pay?..." });
```

## React Native (from JS, outside WebView)

```tsx
import { NativeBridge } from 'native-webview-bridge-react-native';

// Call once at app start (iOS — enables capture hooks)
await NativeBridge.start();

// Launch intent from RN screen (not only WebView)
await NativeBridge.launchIntent({
  data: 'upi://pay?pa=shop@upi&am=499&cu=INR',
  chooser: true,
});

const apps = await NativeBridge.getUpiApps();
```

See **[REACT-NATIVE.md](./REACT-NATIVE.md)** for full RN setup.

## Android `launchIntent` params

| Param | Description |
|-------|-------------|
| `action` | Intent action (default `android.intent.action.VIEW`) |
| `data` / `url` | URI string |
| `type` | MIME type (for SEND) |
| `package` | Target app package |
| `category` | Intent category |
| `chooser` | Show system chooser |
| `chooserTitle` | Chooser dialog title |
| `flags` | Intent flags (default adds `NEW_TASK`) |
| `extras` | Key/value map for intent extras |

## UPI shortcut

For payments, prefer `openUpiPayment()` — see [PAYMENTS.md](./PAYMENTS.md).

To open a specific UPI app:

```javascript
await NativeBridge.launchIntent({
  data: (await NativeBridge.buildUpiUri({ vpa: "shop@upi", amount: "100" })).uri,
  package: "com.phonepe.app",
});
```
