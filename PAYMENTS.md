# UPI & payment apps in the WebView

The bridge exposes installed UPI / wallet apps to your web checkout so you can render icons, labels, and “Pay with PhonePe” buttons — no extra npm package on the web side.

## Web usage

```javascript
// Only installed UPI apps (PhonePe, GPay, Paytm, BHIM, …)
const upiApps = await NativeBridge.getUpiApps();
/*
  [
    { id: "phonepe", name: "PhonePe", packageName: "com.phonepe.app",
      scheme: "phonepe", category: "upi", installed: true },
    ...
  ]
*/

// Full snapshot: UPI + wallets + WhatsApp, with installed flags
const { upi, wallets, messaging, all } = await NativeBridge.getPaymentApps();

// Render checkout UI
all.filter(a => a.installed).forEach(app => {
  renderAppButton(app.name, app.id);
});
```

### Open UPI payment

```javascript
const result = await NativeBridge.openUpiPayment({
  vpa: "merchant@paytm",   // payee UPI ID
  amount: "499.00",
  name: "My Shop",
  note: "Order #1234",
  txnId: "TXN1234",
  currency: "INR",           // default
});

if (result.opened) {
  // Native UPI chooser opened (Android) or default UPI app (iOS)
} else {
  showError(result.error);
}
```

Build the URI without opening:

```javascript
const { uri } = await NativeBridge.buildUpiUri({ vpa: "shop@upi", amount: "100" });
// upi://pay?pa=shop@upi&am=100&cu=INR
```

Check / open arbitrary deep links:

```javascript
const { canOpen } = await NativeBridge.canOpenUrl("phonepe://");
await NativeBridge.openUrl("phonepe://");
```

## Example: simple checkout list

```html
<div id="upi-list"></div>
<script>
  async function renderUpiOptions() {
    const apps = await NativeBridge.getUpiApps();
    const el = document.getElementById("upi-list");
    el.innerHTML = apps.map(a =>
      `<button data-id="${a.id}">${a.name}</button>`
    ).join("");
    el.onclick = async (e) => {
      const btn = e.target.closest("button");
      if (!btn) return;
      await NativeBridge.openUpiPayment({
        vpa: "your-merchant@upi",
        amount: "100.00",
        name: "Store",
        note: btn.dataset.id,
      });
    };
  }
  if (window.NativeBridge?.isAvailable()) renderUpiOptions();
  else window.addEventListener("nativebridgeready", renderUpiOptions);
</script>
```

## Native setup (one-time)

### Android

The library manifest already declares `<queries>` for UPI intents and common package names (required on Android 11+). No extra host code.

### iOS

Add schemes to your host app **Info.plist** so `canOpenURL` can detect installed apps:

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>upi</string>
  <string>phonepe</string>
  <string>tez</string>
  <string>paytmmp</string>
  <string>bhim</string>
  <string>amazonpay</string>
  <string>mobikwik</string>
  <string>freecharge</string>
  <string>cred</string>
  <string>whatsapp</string>
</array>
```

Without these entries, iOS always reports apps as not installed even when they are on the device.

## Notes

- **Detection only** — the bridge does not process payment callbacks. Handle success/failure via your backend, webhooks, or app deep links.
- **Android** returns real `packageName`; **iOS** uses URL schemes (no package concept).
- Unknown UPI handlers on Android may still appear in `getUpiApps()` via the system intent query.
- Opening `upi://pay` shows the system/app chooser on Android; iOS opens the default handler for that URL.
