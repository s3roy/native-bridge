# Share data between multiple WebViews

Use a **single app-wide store** so WebView A can write data and WebView B reads it instantly — no native glue code.

Both WebViews must use **`BridgeWebView`** (they auto-attach to the same `NativeBridge` singleton).

---

## How it works

```
┌─────────────────┐         ┌──────────────────────┐         ┌─────────────────┐
│  WebView A      │ setData │  NativeBridge store  │ onData  │  WebView B      │
│  (checkout)     ├────────►│  (in-memory, shared) ├────────►│  (summary)      │
│  wv_1           │         │                      │ getData │  wv_2           │
└─────────────────┘         └──────────────────────┘         └─────────────────┘
```

- **Write:** `setData(key, value)` from any WebView or `putData` from native
- **Read:** `getData(key)` or `getAllData()` from any WebView
- **Live updates:** `onData` fires on **every** attached WebView when data changes
- **Source:** payload includes `source` (`wv_1`, `wv_2`, or `native`)

---

## WebView A — write data

```javascript
// Save cart before navigating to second WebView
await NativeBridge.setData("checkoutCart", {
  items: [{ id: "sku-1", qty: 2 }],
  total: 998,
});

// Optional: know which WebView you are
const { id } = await NativeBridge.getWebViewId();
console.log("I am", id); // wv_1
```

---

## WebView B — read data (pull)

```javascript
// On page load — read what WebView A stored
const cart = await NativeBridge.getData("checkoutCart");
if (cart) renderSummary(cart);

// Or read everything
const all = await NativeBridge.getAllData();
```

---

## WebView B — read data (push / realtime)

```javascript
NativeBridge.onData(({ key, value, source, removed }) => {
  if (key !== "checkoutCart") return;
  if (removed) {
    clearSummary();
    return;
  }
  console.log("Updated by", source); // wv_1 or native
  renderSummary(value);
});
```

---

## Native app — also can write

```kotlin
// Android — visible to all WebViews immediately
NativeBridge.putData("authToken", token)
```

```swift
// iOS
NativeBridge.shared.putData("authToken", token)
```

Web pages see `source: "native"` in `onData`.

---

## Clear data

```javascript
await NativeBridge.removeData("checkoutCart");
// All WebViews get onData({ key, removed: true, value: null })
```

---

## Two WebViews in native layout

**Android** — two `BridgeWebView` in the same activity (or different activities in same process):

```xml
<com.bridge.BridgeWebView android:id="@+id/webCheckout" ... />
<com.bridge.BridgeWebView android:id="@+id/webSummary" ... />
```

```kotlin
findViewById<BridgeWebView>(R.id.webCheckout).loadUrl("https://app.example.com/checkout")
findViewById<BridgeWebView>(R.id.webSummary).loadUrl("https://app.example.com/summary")
```

**iOS** — two `BridgeWebView` instances:

```swift
let checkout = BridgeWebView(frame: topRect)
let summary = BridgeWebView(frame: bottomRect)
view.addSubview(checkout)
view.addSubview(summary)
checkout.load(URLRequest(url: checkoutURL))
summary.load(URLRequest(url: summaryURL))
```

No extra wiring — sharing works automatically.

---

## React Native — two BridgeWebViews

```tsx
<View style={{ flex: 1 }}>
  <BridgeWebView source={{ uri: 'https://app.example.com/a' }} style={{ flex: 1 }} />
  <BridgeWebView source={{ uri: 'https://app.example.com/b' }} style={{ flex: 1 }} />
</View>
```

Same `setData` / `getData` / `onData` API inside each web page.

---

## Tips

| Topic | Guidance |
|-------|----------|
| **Key naming** | Use clear keys: `checkoutCart`, `selectedAddress` |
| **JSON values** | Objects and arrays work; they are stored as JSON |
| **Persistence** | In-memory only — re-`setData` on cold start if needed |
| **Ignore self** | `onData` fires on writer too; filter with `source` if needed |
| **Security** | Only load trusted URLs; any WebView can read shared keys |

---

## Ignore updates from self

```javascript
let myId;
NativeBridge.getWebViewId().then((r) => { myId = r.id; });

NativeBridge.onData(({ key, value, source }) => {
  if (source === myId) return; // skip own writes
  if (key === "checkoutCart") renderSummary(value);
});
```
