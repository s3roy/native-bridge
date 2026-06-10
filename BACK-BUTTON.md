# Hardware back button → WebView

When the user presses **Android back** (or iOS swipe-back), the web page is notified so it can close modals, navigate SPA routes, or let the app exit.

**`BridgeWebView` handles this automatically** — no native code required.

---

## Web API

```javascript
// Listen for back press — return true to block app exit / history back
const off = NativeBridge.onBackPress((event) => {
  console.log(event.source);    // "hardware" | "gesture" | "api"
  console.log(event.canGoBack); // WebView has history?
  console.log(event.url);

  if (modalOpen) {
    closeModal();
    return true; // consumed — native won't go back or exit
  }
  return false; // not consumed — native goes WebView history back or exits app
});

// Passive listener (can't block)
NativeBridge.on("back.press", (event) => {
  analytics.track("back_press", event);
});

// When WebView actually navigates back in history
NativeBridge.on("back.navigation", (event) => {
  console.log("Went back to", event.url);
});

// Imperative
const { canGoBack } = await NativeBridge.canGoBackInWebView();
if (canGoBack) await NativeBridge.goBackInWebView();
```

---

## How it works (Android)

```
User presses back
       │
       ▼
BridgeWebView intercepts (auto-registered)
       │
       ▼
Web listeners (onBackPress) — return true?
       │
   yes │ no
       │  ├── WebView.canGoBack()? → webView.goBack()
       │  └── else → Activity finishes (exit screen/app)
       └── stop (consumed)
```

---

## SPA example (React / Vue router)

```javascript
let modalOpen = false;

NativeBridge.onBackPress(() => {
  if (modalOpen) {
    modalOpen = false;
    return true;
  }
  // Let native handle: WebView history or app exit
  if (window.history.length > 1) {
    router.back();
    return true;
  }
  return false;
});
```

---

## Checkout flow example

```javascript
NativeBridge.onBackPress(({ canGoBack }) => {
  if (step === "payment") {
    setStep("cart");
    return true;
  }
  if (step === "cart") {
    setStep("browse");
    return true;
  }
  return false; // exit app on last step
});
```

---

## React Native

`BridgeWebView` wires `BackHandler` automatically:

```tsx
<BridgeWebView
  source={{ uri: 'https://app.example.com' }}
  interceptBackPress={true}
/>
```

Same `NativeBridge.onBackPress` in your web app.

---

## iOS

No hardware back button. Supported:

- **Edge swipe back** in WebView → fires `back.press` with `source: "gesture"`
- **Nav bar back** → call `bridgeWebView.handleBackPress()` from Swift

```swift
@objc func backTapped() {
    bridgeWebView.handleBackPress { consumed in
        if !consumed { navigationController?.popViewController(animated: true) }
    }
}
```

---

## Disable interception

**Android:**

```kotlin
bridgeWebView.setInterceptBackPress(false)
```

**iOS:**

```swift
bridgeWebView.setInterceptBackPress(false)
```

---

## Native Activity without BridgeWebView auto-hook

```kotlin
override fun onBackPressed() {
    bridgeWebView.handleBackPress { consumed ->
        if (!consumed) super.onBackPressed()
    }
}
```

---

## Events summary

| Event / API | Fires when | Can block? |
|-------------|------------|------------|
| `onBackPress(cb)` | User presses back / gesture | Yes — return `true` |
| `on("back.press")` | Same moment | No (observe only) |
| `on("back.navigation")` | WebView went back in history | No |
| `canGoBackInWebView()` | Poll history state | — |
| `goBackInWebView()` | Programmatic history back | — |
