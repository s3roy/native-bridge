# Device resource lifecycle — camera, gallery, location

How to **occupy, release, and avoid bugs** when using camera, gallery upload, and location from the WebView.

---

## Resource status

Check before opening camera/gallery again:

```javascript
const status = await NativeBridge.getDeviceResourceStatus();
// { camera: "idle"|"busy", gallery: "idle"|"busy", contact: "idle"|"busy", location: "idle"|"busy" }

if (status.camera === "busy") {
  await NativeBridge.cancelDeviceOperation("camera");
}
```

---

## Occupy → use → release pattern

```javascript
async function captureAndUpload() {
  await NativeBridge.ensurePermission("camera");

  const photo = await NativeBridge.takePhoto({ quality: 80, maxDimension: 1920 });
  if (photo.cancelled) return;

  await uploadToServer(photo.base64);

  // Camera is auto-released when picker closes.
  // Release torch/location if you used them:
  await NativeBridge.releaseDeviceResources({ resources: ["location"] });
}
```

### Auto-release (built-in)

| API | Released when |
|-----|----------------|
| `takePhoto` | User takes photo or cancels picker |
| `pickImage` / `pickImages` | User picks or cancels |
| `pickContact` | User picks or cancels |
| `getCurrentLocation` | Fix received, timeout, or cancel |
| Temp photo files (Android) | Deleted after encode |

### Manual release (call when needed)

```javascript
// Release everything (location listeners, torch, busy flags)
await NativeBridge.releaseDeviceResources();

// Or specific resources
await NativeBridge.releaseDeviceResources({
  resources: ["camera", "gallery", "location"],
});

// Cancel in-flight operation
await NativeBridge.cancelDeviceOperation("camera");
```

---

## getUserMedia (live camera in web)

Always **stop tracks** when done or page hides:

```javascript
let mediaStream;

async function startVideo() {
  await NativeBridge.ensurePermission("camera");
  await NativeBridge.ensurePermission("microphone");
  mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  videoEl.srcObject = mediaStream;
}

async function stopVideo() {
  await NativeBridge.stopMediaStream(mediaStream);
  mediaStream = null;
  videoEl.srcObject = null;
}

// On app background — release camera for other apps
NativeBridge.onLifecycle(async ({ isForeground }) => {
  if (!isForeground && mediaStream) {
    await stopVideo();
  }
});
```

---

## Gallery upload (multiple images)

```javascript
await NativeBridge.ensurePermission("photos");

const result = await NativeBridge.pickImages({
  maxCount: 5,
  quality: 75,
  maxDimension: 1600,
});
// { images: [{ base64, mimeType, width, height }, ...], count: 5 }

for (const img of result.images) {
  await uploadImage(img.base64);
}
```

Single image with `multiple: true` alias:

```javascript
await NativeBridge.pickImage({ multiple: true, maxCount: 3 });
```

---

## Edge cases handled

| Issue | Fix |
|-------|-----|
| Double `takePhoto()` while camera open | Second call errors: `camera is busy` |
| Activity destroyed mid-capture (Android) | Returns `{ cancelled: true, reason: "destroyed" }` |
| Large gallery images OOM | `maxDimension` + subsampling on Android |
| Photo wrong orientation | EXIF rotation applied (Android API 24+) |
| Location listener leak | Removed on result, timeout, cancel, or `releaseDeviceResources` |
| Torch left on | `releaseDeviceResources()` turns off torch |
| Concurrent location requests | Second call errors: `Location request already in progress` |
| Temp camera file left on disk | Deleted in `cleanupPhotoFile()` |
| iOS picker dismissed | Session released; `cancelled: true` |

---

## Recommended page cleanup

```javascript
window.addEventListener("beforeunload", () => {
  NativeBridge.releaseDeviceResources();
});

NativeBridge.onLifecycle(({ isForeground }) => {
  if (!isForeground) {
    NativeBridge.releaseDeviceResources({ resources: ["location"] });
  }
});
```

---

## Error handling

```javascript
try {
  const photo = await NativeBridge.takePhoto();
} catch (e) {
  if (e.message.includes("busy")) {
    await NativeBridge.cancelDeviceOperation("camera");
    // retry once
  } else if (e.message.includes("permission")) {
    await NativeBridge.openSettings();
  }
}
```

---

See [DEVICE.md](./DEVICE.md) for full API list.
