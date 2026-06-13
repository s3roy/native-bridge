# NativeBridge demo apps

Three sample apps that load the **Bridge Playground** inside `BridgeWebView` and log native events (`WEBVIEW_LOADED`, `send`, etc.).

## Before you run

Start the docs/playground dev server from the repo root:

```bash
cd native-webview-bridge
npm run dev
# → http://localhost:3001/playground
```

### Default playground URLs

| Target | URL |
|--------|-----|
| **Android emulator** | `http://10.0.2.2:3001/playground` |
| **iOS Simulator** | `http://localhost:3001/playground` |
| **Physical device** | `http://YOUR_LAN_IP:3001/playground` |
| **Deployed** | `https://native-webview-bridge.vercel.app/playground` |

---

## Android (`android-demo/`)

Kotlin app using `com.bridge.BridgeWebView`.

```bash
# Open in Android Studio
open -a "Android Studio" examples/android-demo

# Or command line (after Android SDK installed)
cd examples/android-demo
./gradlew :app:installDebug
```

Features:
- Editable URL bar
- Native event log at the bottom
- `NativeBridge.setOnWebViewLoaded` + `setWebEventListener`
- Cleartext HTTP enabled for local dev

---

## iOS (`ios-demo/`)

Swift UIKit app using `BridgeWebView`.

### Option A — XcodeGen

```bash
brew install xcodegen
cd examples/ios-demo
./setup-xcode.sh
open NativeBridgeDemo.xcodeproj
```

### Option B — Manual Xcode

1. **File → New → Project → App** (UIKit, Swift), name `NativeBridgeDemo`
2. **File → Add Package Dependencies → Add Local** → select `native-webview-bridge/ios`
3. Copy `NativeBridgeDemo/AppDelegate.swift` and `ViewController.swift` into the project
4. Merge `Info.plist` keys (ATS, camera, mic, location)
5. Run on Simulator

---

## React Native (`NativeBridgeDemo/`)

```bash
cd examples/NativeBridgeDemo
npm install

# Terminal 1 — Metro
npm start

# Terminal 2 — Android
npm run android

# Or iOS (macOS)
cd ios && pod install && cd ..
npm run ios
```

Uses local packages:
- `file:../../react-native` — RN wrapper
- `file:../../../android` — core Android library (via `settings.gradle`)

`App.tsx` renders `BridgeWebView` + `NativeBridge.onWebViewLoaded` log panel.

---

## What to verify in the playground

1. Status shows **Connected** (not “Not in WebView”)
2. **Run all read-only** returns device info, safe area, permissions
3. **WEBVIEW_LOADED** appears in native log (dom + complete)
4. **notifyWebViewLoaded** / custom **send** shows in native log
5. Live events (`app.state`, `app.safeArea`, etc.) stream in playground log

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Blank WebView / connection refused | Dev server running? Correct URL for emulator vs device? |
| Android emulator can’t reach localhost | Use `10.0.2.2`, not `localhost` |
| iOS Simulator HTTP blocked | ATS disabled in demo `Info.plist` |
| RN Gradle: `:native-webview-bridge` not found | Check `android/settings.gradle` path to `../../../android` |
| `NativeBridge is undefined` | Must use `BridgeWebView`, not raw `WebView` |
