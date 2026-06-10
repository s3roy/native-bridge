# WebView cache & Vercel bandwidth

Control what the WebView caches — **fresh HTML on every launch**, **cached hashed JS/CSS chunks** — so users get updates without re-downloading megabytes of static assets. Lowers **Vercel bandwidth bills**.

---

## Default: `smart` mode

`BridgeWebView` ships with **smart** cache policy:

| Resource | Behavior |
|----------|----------|
| HTML / SPA routes | `Cache-Control: no-cache` — always revalidated |
| `/_next/static/*` chunks | Cached (immutable hashes) |
| `.js`, `.css`, fonts, images | Cached per server headers |

```
WebView opens
     │
     ▼
HTML document ──► network (fresh)
     │
     ▼
app-[hash].js ──► disk cache (skip download)
chunk-[hash].js ──► disk cache
```

---

## Web API

```javascript
// Default is already smart — optional override
await NativeBridge.setWebViewCachePolicy({
  mode: 'smart',       // smart | noCache | default | cacheOnly
  clearOnLaunch: false // true wipes cache when policy applied
});

const info = await NativeBridge.getWebViewCacheInfo();
console.log(info.mode); // "smart"

// Hard refresh current page (HTML only in smart mode)
await NativeBridge.reloadWebView({ bypassCache: true });

// Nuclear — wipe all WebView cache (+ optional cookies)
await NativeBridge.clearWebViewCache({ disk: true, cookies: false });

// Never cache anything (dev / debugging)
await NativeBridge.setWebViewCachePolicy({ mode: 'noCache' });
```

---

## Native load (fresh document)

```kotlin
// Android — loadUrl already uses smart headers
bridgeWebView.loadUrl("https://app.example.com")
bridgeWebView.loadUrlFresh("https://app.example.com") // explicit
```

```swift
// iOS
bridgeWebView.loadFresh(URL(string: "https://app.example.com")!)
```

---

## Vercel: cache static chunks (lower bills)

Add this to your **web app's** `vercel.json` (not the native app):

```json
{
  "headers": [
    {
      "source": "/_next/static/(.*)",
      "headers": [
        { "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }
      ]
    },
    {
      "source": "/assets/(.*)",
      "headers": [
        { "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }
      ]
    },
    {
      "source": "/(.*\\.(js|css|woff2|png|jpg|svg|ico|webp))",
      "headers": [
        { "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }
      ]
    },
    {
      "source": "/((?!_next/static).*)",
      "headers": [
        { "key": "Cache-Control", "value": "no-cache, no-store, must-revalidate" }
      ]
    }
  ]
}
```

Copy the full template: [`templates/vercel-cache.json`](templates/vercel-cache.json)

### Next.js `next.config.ts`

```typescript
const nextConfig = {
  async headers() {
    return [
      {
        source: "/_next/static/:path*",
        headers: [
          { key: "Cache-Control", value: "public, max-age=31536000, immutable" },
        ],
      },
    ];
  },
};
```

The NativeBridge marketing site (`website/`) already includes these headers.

---

## React Native

```tsx
<BridgeWebView
  source={{ uri: 'https://app.example.com' }}
  cachePolicy="smart"   // default
/>

// noCache for dev
<BridgeWebView cachePolicy="noCache" incognito />
```

Web pages inside still call `NativeBridge.setWebViewCachePolicy()` / `reloadWebView()`.

---

## Modes reference

| Mode | HTML | Static chunks | Use case |
|------|------|---------------|----------|
| `smart` | Fresh | Cached | **Production (default)** |
| `noCache` | Fresh | Fresh | Debugging |
| `default` | Browser default | Browser default | Legacy behavior |
| `cacheOnly` | Cache only | Cache only | Offline kiosk |

---

## Cost impact

Without smart caching, every WebView open re-downloads all JS chunks → high Vercel **Fast Data Transfer** usage.

With **smart + immutable headers**:
- First visit: full download
- Repeat visits: HTML only (~few KB) + cache hits for MB of chunks
- Typical savings: **70–95%** bandwidth on return visits
