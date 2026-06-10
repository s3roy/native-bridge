# Permissions — request from WebView

Web pages call `NativeBridge.requestPermission(...)` → native shows the **system dialog** → result returns to JavaScript.

No native code required beyond installing the package + Info.plist / manifest entries.

---

## Web API

```js
// Check current status
const { status, canAskAgain } = await NativeBridge.getPermissionStatus("camera");
// status: "granted" | "denied" | "blocked" | "unavailable"

// Request — shows native system dialog
const result = await NativeBridge.requestPermission("camera");
if (result.status === "granted") startCamera();

// All permissions at once
const all = await NativeBridge.getPermissions();

// User permanently denied — open app settings
if (result.status === "blocked") {
  await NativeBridge.openSettings();
}

// Live updates
NativeBridge.onPermissionChange(({ permission, status }) => {
  console.log(permission, status);
});

// Constants
NativeBridge.Permissions.CAMERA       // "camera"
NativeBridge.Permissions.MICROPHONE   // "microphone"
NativeBridge.Permissions.LOCATION
NativeBridge.Permissions.NOTIFICATIONS
NativeBridge.Permissions.PHOTOS
NativeBridge.Permissions.BLUETOOTH
NativeBridge.Permissions.CONTACTS
```

---

## Supported permissions

| Web name | Android | iOS |
|----------|---------|-----|
| `camera` | CAMERA | Camera |
| `microphone` | RECORD_AUDIO | Microphone |
| `location` | FINE + COARSE | When In Use |
| `locationCoarse` | COARSE only | When In Use |
| `notifications` | POST_NOTIFICATIONS (33+) | Push notifications |
| `photos` | READ_MEDIA_IMAGES | Photo library |
| `bluetooth` | BLUETOOTH_CONNECT (31+) | Auto-granted |
| `contacts` | READ_CONTACTS | unavailable on iOS bridge |

---

## Status values

| Status | Meaning |
|--------|---------|
| `granted` | User allowed — use the feature |
| `denied` | Not granted yet — can ask again |
| `blocked` | User denied permanently — call `openSettings()` |
| `unavailable` | Not supported on this OS / not declared |

---

## Chat app example

```js
async function startVideoCall() {
  let mic = await NativeBridge.getPermissionStatus("microphone");
  if (mic.status !== "granted") {
    mic = await NativeBridge.requestPermission("microphone");
  }
  let cam = await NativeBridge.getPermissionStatus("camera");
  if (cam.status !== "granted") {
    cam = await NativeBridge.requestPermission("camera");
  }
  if (mic.status === "granted" && cam.status === "granted") {
    navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  } else if (mic.status === "blocked" || cam.status === "blocked") {
    showMessage("Enable camera & mic in Settings");
    await NativeBridge.openSettings();
  }
}
```

---

## getUserMedia in WebView

If your web code uses `navigator.mediaDevices.getUserMedia()`:

1. First request via bridge: `await NativeBridge.requestPermission("camera")`
2. Then call `getUserMedia` — `BridgeWebView` auto-grants WebView media capture when native permission is already granted.

---

## iOS — add to host app Info.plist

```xml
<key>NSCameraUsageDescription</key>
<string>Needed for video calls</string>
<key>NSMicrophoneUsageDescription</key>
<string>Needed for voice messages and calls</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>Needed to share your location</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Needed to send photos</string>
```

Without these strings iOS **crashes** when requesting permission.

---

## Android — auto-merged permissions

The library declares permissions in its manifest; they merge into your app automatically. On Android 6+ the **runtime dialog** is shown when web calls `requestPermission`.

Optional: request `READ_PHONE_STATE` at runtime if you use call-state detection.

---

## Flow diagram

```
Web page                          Native
   │                                │
   │  requestPermission("camera")   │
   ├──────────────────────────────►│ System permission dialog
   │                                │
   │  { status: "granted" }         │
   ◄──────────────────────────────┤
   │                                │
   │  getUserMedia({ video:true })  │
   ├──────────────────────────────►│ BridgeWebView grants capture
```
