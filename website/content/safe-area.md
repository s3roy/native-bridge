# Safe area & system UI

`BridgeWebView` **automatically pads content** below the status bar / notch and above the navigation bar — your web page no longer draws under the notification bar.

---

## Automatic (default)

Use `BridgeWebView` in XML or code. No web changes required for basic safety.

```xml
<com.bridge.BridgeWebView
    android:id="@+id/web"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```swift
let web = BridgeWebView()
```

Padding is on by default (`applySafeAreaPadding = true`).

---

## Web API

```javascript
// Current insets (px) — also in getAppState().safeArea
const { top, bottom, left, right } = await NativeBridge.getSafeArea();

// Live updates (rotation, keyboard, immersive mode)
NativeBridge.onSafeArea(({ top, bottom }) => {
  document.querySelector('header').style.paddingTop = top + 'px';
});

// CSS variables injected automatically on every page:
// --native-safe-area-top, --sab, --sat, etc.
// Use in your stylesheet:
// header { padding-top: var(--native-safe-area-top, 0px); }
```

### Toggle native padding

```javascript
// Edge-to-edge web design — you handle insets in CSS only
await NativeBridge.setApplySafeAreaPadding(false);
```

---

## Android system UI (status bar, nav bar, immersive)

```javascript
await NativeBridge.setSystemUi({
  statusBarColor: '#1a1a2e',
  statusBarStyle: 'light-content',   // white icons
  navigationBarColor: '#000000',
  navigationBarStyle: 'dark-content',
  statusBarHidden: false,
  navigationBarHidden: false,
  layoutFullscreen: false,           // true = edge-to-edge behind bars
  immersive: false,                  // hide bars, swipe to reveal
  keepScreenOn: true,
});

const ui = await NativeBridge.getSystemUi();
```

| Option | Effect |
|--------|--------|
| `statusBarColor` | Hex or `"transparent"` |
| `statusBarStyle` | `"light-content"` / `"dark-content"` |
| `navigationBarColor` | Android nav bar color |
| `immersive` | Full-screen, transient system bars |
| `layoutFullscreen` | Draw behind status bar (pair with CSS safe area) |
| `keepScreenOn` | Prevent screen sleep |

---

## Android notifications (system shade)

```javascript
const { enabled, activeCount, active } = await NativeBridge.getNotificationSettings();

// Cancel one notification shown in the shade
await NativeBridge.cancelNotification({ id: 42, tag: 'order' });

// Clear all + badge
await NativeBridge.cancelAllNotifications();

// Open app's notification settings screen
await NativeBridge.openNotificationSettings();
```

---

## iOS notes

| Feature | Support |
|---------|---------|
| Safe area padding | ✅ Auto on `BridgeWebView` |
| CSS variables | ✅ Injected |
| Notification settings | ✅ `getNotificationSettings` (async) |
| Cancel delivered notifications | ✅ |
| Open settings | ✅ |
| Status bar style | ⚠️ Limited — host `UIViewController` may need `setNeedsStatusBarAppearanceUpdate` |
| `keepScreenOn` | ✅ `UIApplication.isIdleTimerDisabled` |

---

## CSS example

```css
.app-header {
  padding-top: var(--native-safe-area-top, env(safe-area-inset-top, 0px));
  padding-left: var(--native-safe-area-left, env(safe-area-inset-left, 0px));
  padding-right: var(--native-safe-area-right, env(safe-area-inset-right, 0px));
}
.app-footer {
  padding-bottom: var(--native-safe-area-bottom, env(safe-area-inset-bottom, 0px));
}
```

---

## Native toggle (optional)

```kotlin
bridgeWebView.setApplySafeAreaPadding(false) // edge-to-edge
```

```swift
bridgeWebView.setApplySafeAreaPadding(false)
```

---

## React Native

Web pages inside `BridgeWebView` use the same `NativeBridge.*` APIs.

For the RN shell, also use `react-native-safe-area-context` + `StatusBar` around the WebView if needed.
