# How apps install native-webview-bridge

Your package lives at:

```
native-webview-bridge/
├── android/     ← Android library (AAR module)
├── ios/         ← iOS Swift Package
├── react-native/← React Native wrapper (BridgeWebView + NativeBridge module)
├── bridge-js/   ← reference only (web SDK is auto-injected by native)
```

**Web pages install nothing.** Once the native app adds this package, `window.NativeBridge` appears automatically inside the WebView.

**Enterprise docs:** [docs/README.md](./docs/README.md) · [Executive brief](./docs/EXECUTIVE-BRIEF.md) · [Feature catalog](./docs/FEATURES.md) · [Production checklist](./docs/PRODUCTION-READINESS.md) · [Changelog](./CHANGELOG.md)

---

## Option A — Local folder (fastest for testing)

Copy or clone `native-webview-bridge` into your project repo.

---

## Android install

### Step 1 — Include the library module

**`settings.gradle`** (project root):

```gradle
include ':app'
include ':native-webview-bridge'
project(':native-webview-bridge').projectDir = new File('../native-webview-bridge/android')
```

Adjust the path if your folder layout differs.

### Step 2 — Add dependency in your app

**`app/build.gradle`**:

```gradle
dependencies {
    implementation project(':native-webview-bridge')
}
```

Sync Gradle. The library **auto-starts** via `BridgeInitProvider` — no `Application` class code needed.

### Step 3 — Use BridgeWebView in layout

**`activity_main.xml`**:

```xml
<com.bridge.BridgeWebView
    android:id="@+id/web"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**`MainActivity.kt`**:

```kotlin
import com.bridge.BridgeWebView
import com.bridge.BridgeInterceptor
import com.bridge.NativeBridge
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<BridgeWebView>(R.id.web).loadUrl("https://your-chat-app.com")

// Hardware back is auto-routed to the web page — see BACK-BUTTON.md
// NativeBridge.onBackPress(() => { ... return true to consume })

// Safe area is auto-applied — content won't draw under the status bar (see SAFE-AREA.md)
// NativeBridge.setSystemUi({ statusBarColor: '#fff', statusBarStyle: 'dark-content' })

// Pass auth to web — putData OR HTTP cookie — see COOKIES.md
// bridgeWebView.setCookie("authToken", token, "https://your-app.com")
// bridgeWebView.clearAllCookies() // logout

// Smart cache: fresh HTML, cached JS/CSS chunks — see CACHE.md
// BridgeWebView uses smart mode by default; add templates/vercel-cache.json to your web app

        // Optional: expose auth token to web
        NativeBridge.putData("authToken", yourToken)
    }
}

// In your Retrofit/OkHttp setup (once, app-wide):
val okHttp = OkHttpClient.Builder()
    .addInterceptor(BridgeInterceptor())
    .build()
```

### Step 4 — (Optional) permissions at runtime

For phone call state on Android 6+:

```kotlin
requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 1)
```

---

## iOS install

### Step 1 — Add Swift Package in Xcode

1. Open your iOS project in Xcode  
2. **File → Add Package Dependencies…**  
3. Click **Add Local…** and select the `native-webview-bridge/ios` folder  

   **OR** after pushing to GitHub:

   ```
   https://github.com/s3roy/native-bridge
   ```
   (set package path to `ios` if monorepo)

4. Add product **NativeWebViewBridge** to your app target

### Step 2 — One line in AppDelegate

**`AppDelegate.swift`**:

```swift
import NativeWebViewBridge

func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
) -> Bool {
    NativeBridge.start()
    return true
}
```

### Step 3 — Use BridgeWebView

**`ViewController.swift`**:

```swift
import NativeWebViewBridge

class ViewController: UIViewController {
    private var web: BridgeWebView!

    override func viewDidLoad() {
        super.viewDidLoad()
        web = BridgeWebView(frame: view.bounds)
        web.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(web)
        web.load(URLRequest(url: URL(string: "https://your-chat-app.com")!))

        NativeBridge.shared.putData("authToken", yourToken)
    }
}
```

---

## Web install (your chat pages)

**Nothing to npm install.** In pages loaded inside the WebView:

```js
if (window.NativeBridge?.isAvailable()) {
  const state = await NativeBridge.getAppState();
  NativeBridge.onPiP((p) => console.log("PiP:", p.isInPiP));
} else {
  // running in normal browser — use fetch directly
}
```

For TypeScript in your web repo, copy `bridge-js/native-bridge.d.ts` for types only.

---

## Option B — Publish for other teams (production)

### Android → Maven / GitHub Packages

1. Add `maven-publish` to `android/build.gradle`  
2. Run `./gradlew publish`  
3. Other apps depend on:

   ```gradle
   implementation 'com.yourcompany:native-webview-bridge:1.0.0'
   ```

### iOS → Git tag + SPM

1. Push repo to GitHub  
2. Tag `1.0.0`  
3. Other devs: Xcode → Add Package → your Git URL

---

## What happens automatically after install

| Layer | Auto on install |
|-------|-----------------|
| Web SDK injected | ✅ every page in BridgeWebView |
| App state (PiP, keyboard, battery…) | ✅ |
| API capture | Android: needs `BridgeInterceptor()` · iOS: `NativeBridge.start()` |
| Notifications | iOS auto · Android: optional FCM service |

---

## Checklist

- [ ] Android: module in `settings.gradle` + `implementation project(...)`  
- [ ] Android: replace `WebView` with `BridgeWebView`  
- [ ] Android: `addInterceptor(BridgeInterceptor())` on OkHttp  
- [ ] iOS: add Swift Package  
- [ ] iOS: `NativeBridge.start()` in AppDelegate  
- [ ] iOS: use `BridgeWebView`  
- [ ] Web: use `NativeBridge.*` in your pages (no install)  
- [ ] Load only **trusted URLs** in the WebView  

See **`PERMISSIONS.md`** for camera, microphone, location, notifications, and more — request from web with `NativeBridge.requestPermission("camera")`.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `NativeBridge is undefined` in web | Not using `BridgeWebView`, or page loaded before inject — use `nativebridgeready` event |
| API calls empty on web | Android: forgot `BridgeInterceptor()` · iOS: forgot `NativeBridge.start()` |
| Gradle sync fails | Check `projectDir` path to `android/` folder |
| iOS package not found | Point SPM to `ios/` folder, not repo root |
