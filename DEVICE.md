# Device APIs — location, camera, and more

Full device access from your WebView: **location, camera, gallery, contacts, clipboard, share, vibrate, dial, SMS, maps, torch** — plus permissions.

All methods are on `window.NativeBridge` (auto-injected). No web npm install.

---

## Quick reference

| API | What it does |
|-----|----------------|
| `getCapabilities()` | What this device supports |
| `ensurePermission(name)` | Ask permission if needed, then use feature |
| `getCurrentLocation(opts)` | GPS coordinates |
| `takePhoto(opts)` | Native camera → base64 JPEG |
| `pickImage(opts)` | Gallery picker → base64 JPEG |
| `pickContact()` | System contact picker |
| `getClipboard()` / `setClipboard(text)` | Clipboard read/write |
| `share({ text, url, title })` | Native share sheet |
| `vibrate({ duration, pattern })` | Haptic / vibrate |
| `dial(phone)` | Open phone dialer |
| `sendSms(phone, body)` | Open SMS app |
| `openMaps({ latitude, longitude, query })` | Open maps app |
| `setTorch(true/false)` | Flashlight on/off |

Permissions: see [PERMISSIONS.md](./PERMISSIONS.md).

---

## Check what’s available

```javascript
const caps = await NativeBridge.getCapabilities();
// { location, camera, gallery, contacts, clipboard, share, vibrate, dial, sms, maps, torch, ... }
```

---

## Location

```javascript
await NativeBridge.ensurePermission("location");

const pos = await NativeBridge.getCurrentLocation({
  enableHighAccuracy: true,
  timeout: 15000,
  maximumAge: 60000,
});
// { latitude, longitude, accuracy, altitude, heading, speed, timestamp }

console.log(pos.latitude, pos.longitude);
```

---

## Camera & gallery

```javascript
await NativeBridge.ensurePermission("camera");

const photo = await NativeBridge.takePhoto({
  quality: 80,        // JPEG 1–100
  facing: "rear",     // "front" | "rear"
});
if (!photo.cancelled) {
  const img = document.createElement("img");
  img.src = "data:" + photo.mimeType + ";base64," + photo.base64;
}

// Gallery
await NativeBridge.ensurePermission("photos");
const picked = await NativeBridge.pickImage({ quality: 85 });
```

### Video / live camera in web page

```javascript
await NativeBridge.ensurePermission("camera");
await NativeBridge.ensurePermission("microphone");
const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
```

`BridgeWebView` auto-grants WebView capture when native permission is already granted.

---

## Contacts

```javascript
const contact = await NativeBridge.pickContact();
if (!contact.cancelled) {
  console.log(contact.name, contact.phone, contact.email);
}
```

iOS uses system picker (no READ_CONTACTS permission needed). Android may need `contacts` permission for some devices.

---

## Clipboard

```javascript
await NativeBridge.setClipboard("Order #12345");
const { text } = await NativeBridge.getClipboard();
```

---

## Share

```javascript
await NativeBridge.share({
  title: "Check this out",
  text: "Amazing product",
  url: "https://shop.example.com/p/1",
});
```

---

## Vibrate

```javascript
await NativeBridge.vibrate({ duration: 200 });
await NativeBridge.vibrate({ pattern: [0, 100, 50, 100] });
```

---

## Phone & SMS

```javascript
await NativeBridge.dial("+919876543210");
await NativeBridge.sendSms("+919876543210", "Hello from the app");
```

---

## Maps

```javascript
await NativeBridge.openMaps({
  latitude: 19.0760,
  longitude: 72.8777,
  query: "Mumbai",
});

await NativeBridge.openMaps({ query: "Nearest ATM" });
```

---

## Flashlight

```javascript
await NativeBridge.ensurePermission("camera");
await NativeBridge.setTorch(true);
// … later
await NativeBridge.setTorch(false);
```

---

## Full flow example (delivery app)

```javascript
async function captureDeliveryProof() {
  const locPerm = await NativeBridge.ensurePermission("location");
  const camPerm = await NativeBridge.ensurePermission("camera");
  if (locPerm.status !== "granted" || camPerm.status !== "granted") {
    alert("Location and camera are required");
    return;
  }

  const [location, photo] = await Promise.all([
    NativeBridge.getCurrentLocation({ enableHighAccuracy: true }),
    NativeBridge.takePhoto({ quality: 75 }),
  ]);

  await fetch("/api/delivery-proof", {
    method: "POST",
    body: JSON.stringify({
      lat: location.latitude,
      lng: location.longitude,
      photo: photo.base64,
    }),
  });

  NativeBridge.vibrate({ duration: 100 });
}
```

---

## iOS Info.plist (required)

```xml
<key>NSCameraUsageDescription</key>
<string>For photos and delivery proof</string>
<key>NSMicrophoneUsageDescription</key>
<string>For video calls</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>To show your location on the map</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>To attach photos</string>
```

---

## Android

Permissions merge from the library manifest. Web calls `ensurePermission` → system dialog on Android 6+.

Location services must be enabled in device settings.

---

## Platform notes

| Feature | Android | iOS |
|---------|---------|-----|
| `getCurrentLocation` | GPS / network | CoreLocation |
| `takePhoto` | Camera intent | UIImagePickerController |
| `pickImage` | Gallery intent | Photo library |
| `pickContact` | Contacts picker | CNContactPicker |
| `vibrate` | Vibrator API | Haptic feedback |
| `setTorch` | CameraManager | AVCapture torch |

Photos return **base64 JPEG** — upload to your server; not saved to gallery unless you do so server-side.

---

## Resource lifecycle (important)

Camera and gallery can stay **busy** if misused. Always:

1. Check `getDeviceResourceStatus()` before a second capture
2. Call `releaseDeviceResources()` when leaving the page
3. Call `stopMediaStream(stream)` when done with live video

Full guide: **[DEVICE-LIFECYCLE.md](./DEVICE-LIFECYCLE.md)**

```javascript
await NativeBridge.getDeviceResourceStatus();
await NativeBridge.releaseDeviceResources({ resources: ["camera", "location"] });
await NativeBridge.cancelDeviceOperation("gallery");
await NativeBridge.pickImages({ maxCount: 5, maxDimension: 1600 });
```
