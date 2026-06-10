# WebView cookies

Set, read, and clear **HTTP cookies** in the WebView from native code or from JavaScript — useful for auth sessions, logout, and passing tokens to `document.cookie`.

---

## Authentication integration

**Option A — shared store (recommended for simple tokens):**

```kotlin
NativeBridge.putData("authToken", token)
```

```javascript
const token = await NativeBridge.getData("authToken");
```

**Option B — HTTP cookie (works with existing web session code):**

```kotlin
bridgeWebView.setCookie(
    name = "authToken",
    value = token,
    url = "https://app.example.com",
)
```

```swift
bridgeWebView.setCookie(
    name: "authToken",
    value: token,
    url: "https://app.example.com"
)
```

Web page reads it normally: `document.cookie` or your framework’s cookie helper.

---

## Native API (Android)

```kotlin
// Set one cookie
bridgeWebView.setCookie("session_id", sessionId, "https://app.example.com")

// Full options via JSONObject
BridgeCookies.setCookie(webView, JSONObject()
    .put("name", "authToken")
    .put("value", token)
    .put("url", "https://app.example.com")
    .put("path", "/")
    .put("maxAge", 86400)
    .put("secure", true)
    .put("httpOnly", true)
)

// Read cookies for URL
val result = BridgeCookies.getCookies(webView, JSONObject().put("url", url))

// Remove one
BridgeCookies.removeCookie(webView, JSONObject().put("name", "authToken").put("url", url))

// Clear ALL cookies in WebView
bridgeWebView.clearAllCookies()

// Logout — cookies + cache
bridgeWebView.clearAllCookies()
bridgeWebView.clearWebViewCache() // optional: include cookies via clearWebViewCache JSONObject
```

---

## Native API (iOS)

```swift
bridgeWebView.setCookie(name: "authToken", value: token, url: "https://app.example.com")

bridgeWebView.clearAllCookies {
    print("cookies cleared")
}
```

---

## Web API

```javascript
// Set cookie (visible to document.cookie on matching domain)
await NativeBridge.setCookie({
  name: 'authToken',
  value: token,
  url: 'https://app.example.com',
  path: '/',
  maxAge: 86400,
  secure: true,
});

// Batch set
await NativeBridge.setCookies({
  cookies: [
    { name: 'a', value: '1', url: 'https://app.example.com' },
    { name: 'b', value: '2', url: 'https://app.example.com' },
  ],
});

// Read
const { cookies } = await NativeBridge.getCookies({
  url: 'https://app.example.com',
});

// Remove one
await NativeBridge.removeCookie({ name: 'authToken' });

// Clear all (logout)
await NativeBridge.clearCookies();

// Clear only for one site
await NativeBridge.clearCookies({ url: 'https://app.example.com' });
```

---

## Logout flow (web + native)

```javascript
async function logout() {
  await NativeBridge.clearCookies();
  await NativeBridge.clearWebViewCache({ disk: true, cookies: true });
  await NativeBridge.removeData('authToken');
  window.location.href = '/login';
}
```

```kotlin
fun logout() {
    bridgeWebView.clearAllCookies()
    BridgeWebViewCache.clear(bridgeWebView, JSONObject().put("cookies", true))
    NativeBridge.removeData("authToken", "native")
    bridgeWebView.loadUrl("https://app.example.com/login")
}
```

---

## putData vs cookies

| | `putData` / `getData` | HTTP cookies |
|--|----------------------|--------------|
| Web access | `NativeBridge.getData()` | `document.cookie` |
| HTTP requests | Not sent automatically | Sent with fetch/XHR to same domain |
| Cross-WebView | Yes (shared store) | Per WebView cookie jar |
| Logout | `removeData` | `clearCookies` |

Use **putData** for bridge-only tokens. Use **cookies** when your web app already expects session cookies.

---

## Notes

- `url` defaults to the WebView’s current URL if omitted.
- `httpOnly` cookies set from native are not readable from `document.cookie` in JS (by design).
- React Native: cookie APIs require the native `BridgeWebView` path on Android/iOS; pure RN WebView may need platform-specific handling.
