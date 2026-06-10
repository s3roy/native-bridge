# Security & compliance

Guidance for AppSec, compliance, and production hardening when adopting **native-webview-bridge** in enterprise mobile applications.

---

## 1. Threat model summary

| Asset | Risk | Mitigation |
|-------|------|------------|
| Auth tokens in `putData` | Web JS reads sensitive data | Only load trusted URLs; use short-lived tokens |
| Captured API bodies | PII in ring buffer | Filter in interceptor; limit retention; disable in prod if unused |
| `launchIntent` / `openUrl` | Open malicious deep links | URL allowlist in web app; validate VPA server-side |
| Permission requests | User consent fatigue | Request in context; explain before `requestPermission` |
| WebView XSS | Bridge abuse if page compromised | CSP, sanitize input, trusted origins only |
| JS bridge surface | Method enumeration | Fixed dispatcher; no arbitrary native execution |

---

## 2. WebView content trust

**Rule:** Only load HTTPS origins you control. The bridge grants web code access to native capabilities.

### Recommended

```kotlin
// Android — restrict WebView to your domain
webView.webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val host = request.url.host ?: return true
        return !host.endsWith("yourcompany.com")
    }
}
```

```swift
// iOS — WKNavigationDelegate decidePolicyFor
func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
             decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
    guard let host = navigationAction.request.url?.host,
          host.hasSuffix("yourcompany.com") else {
        decisionHandler(.cancel); return
    }
    decisionHandler(.allow)
}
```

---

## 3. Sensitive data handling

### `putData` / `getData`

| Do | Don't |
|----|-------|
| Pass opaque session references | Pass long-lived refresh tokens without rotation |
| Clear on logout: `putData("authToken", null)` | Store PAN, Aadhaar, full card numbers |
| Use server-side session validation | Treat web-read token as sole auth proof |

### API capture

Captured traffic includes **request/response bodies**. For GDPR / PCI:

- Disable capture in production if not required
- Redact headers (`Authorization`, cookies) in custom interceptor wrapper
- Document in privacy policy if analytics uses captured data

---

## 4. Permissions

- Request permissions **in context** (user tapped "Take photo", not on app launch)
- iOS: every permission needs `Info.plist` usage string — App Store rejection if missing
- Android: runtime permissions merged from library manifest — review merged manifest in release build
- `openSettings` sends user to system settings — use only when `status === "blocked"`

---

## 5. Payments (UPI)

| Control | Owner |
|---------|-------|
| Validate amount & merchant VPA server-side | Backend |
| Generate `txnId` server-side | Backend |
| Payment success/failure | PSP webhook / app deep link — **not** bridge callback |
| Display installed UPI apps | Bridge (detection only) |

The bridge **opens** external payment apps. It does **not** process or store payment credentials.

---

## 6. Intent / URL launching

Validate before calling from web:

```javascript
const ALLOWED = /^https:\/\/|^upi:\/\/|^tel:|^mailto:/;
if (!ALLOWED.test(url)) return;

const { canOpen } = await NativeBridge.canOpenUrl(url);
if (canOpen) await NativeBridge.openUrl(url);
```

On Android, `launchIntent` with `package` restricts target app — prefer for known payment flows.

---

## 7. Network capture security

| Platform | Mechanism | Note |
|----------|-----------|------|
| Android | OkHttp `BridgeInterceptor` | Only captures traffic through that client |
| iOS | `URLSession` swizzle via `NativeBridge.start()` | Captures default session traffic |

**Production option:** omit `BridgeInterceptor()` and skip `NativeBridge.start()` if capture not needed — reduces attack surface and memory use.

---

## 8. Compliance mapping (indicative)

| Regulation | Consideration |
|------------|---------------|
| **GDPR** | Captured API/notifications may contain PII — data minimization, retention limits |
| **PCI DSS** | Do not pass card data through bridge; UPI delegates to external apps |
| **RBI / UPI** | Merchant VPA verification is backend responsibility |
| **App Store / Play** | Permission strings, payment disclaimers, privacy nutrition labels |

*Not legal advice — engage compliance team for your jurisdiction.*

---

## 9. Security checklist (pre-production)

- [ ] WebView loads only allowlisted domains
- [ ] CSP headers on web origin
- [ ] Auth tokens are short-lived; cleared on logout
- [ ] API capture disabled or redacted in production (if not required)
- [ ] Permission prompts have in-app rationale
- [ ] iOS `Info.plist` usage descriptions complete
- [ ] iOS `LSApplicationQueriesSchemes` minimal set for UPI
- [ ] Android merged manifest reviewed
- [ ] UPI amounts validated server-side
- [ ] Pen test scope includes WebView + bridge abuse scenarios
- [ ] Privacy policy mentions device signals if `getAppState` used for tracking

---

## 10. Incident response

If WebView content is compromised (XSS on your domain):

1. Bridge exposes all methods available to that origin — treat as native API access from attacker
2. Rotate sessions server-side immediately
3. Force app update if WebView only loads bundled HTML; patch web origin if remote
4. Review `launchIntent` / `openUrl` usage for open-redirect patterns
