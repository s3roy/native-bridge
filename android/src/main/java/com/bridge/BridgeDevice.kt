package com.bridge

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object BridgeDevice {

    fun getCapabilities(context: Context): JSONObject = JSONObject().apply {
        put("platform", "android")
        put("location", true)
        put("camera", context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY))
        put("gallery", true)
        put("contacts", true)
        put("clipboard", true)
        put("share", true)
        put("vibrate", hasVibrator(context))
        put("dial", true)
        put("sms", true)
        put("maps", true)
        put("torch", context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH))
        put("getUserMedia", true)
    }

    fun getClipboard(context: Context): JSONObject {
        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(context).toString()
        } else ""
        return JSONObject().put("text", text)
    }

    fun setClipboard(context: Context, text: String): JSONObject {
        val mgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        mgr.setPrimaryClip(ClipData.newPlainText("bridge", text))
        return JSONObject().put("set", true)
    }

    fun vibrate(context: Context, params: JSONObject): JSONObject {
        if (!hasVibrator(context)) {
            return JSONObject().put("vibrated", false).put("error", "No vibrator")
        }
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (params.has("pattern")) {
            val arr = params.getJSONArray("pattern")
            val pattern = LongArray(arr.length()) { arr.getLong(it) }
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } else {
            val duration = params.optLong("duration", 200L)
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
        return JSONObject().put("vibrated", true)
    }

    fun dial(context: Context, phone: String): JSONObject =
        BridgeIntents.launchIntent(
            context,
            JSONObject()
                .put("action", Intent.ACTION_DIAL)
                .put("data", "tel:$phone"),
        )

    fun sendSms(context: Context, phone: String, body: String): JSONObject {
        val uri = Uri.parse("smsto:$phone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            JSONObject().put("opened", true)
        } catch (e: Exception) {
            JSONObject().put("opened", false).put("error", e.message)
        }
    }

    fun openMaps(context: Context, params: JSONObject): JSONObject {
        val lat = params.optDouble("latitude", Double.NaN)
        val lng = params.optDouble("longitude", Double.NaN)
        val query = params.optString("query", "")
        val uri = when {
            !lat.isNaN() && !lng.isNaN() ->
                Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(query)})")
            query.isNotEmpty() ->
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            else -> return JSONObject().put("opened", false).put("error", "latitude/longitude or query required")
        }
        return BridgeIntents.launchIntent(
            context,
            JSONObject().put("action", Intent.ACTION_VIEW).put("data", uri.toString()),
        )
    }

    fun setTorch(context: Context, enabled: Boolean): JSONObject {
        if (BridgeDeviceSession.isBusy(BridgeDeviceSession.Resource.CAMERA)) {
            return JSONObject().put("enabled", false).put("error", "Camera is in use")
        }
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return JSONObject()
                .put("enabled", false).put("error", "No camera")
            cameraManager.setTorchMode(cameraId, enabled)
            JSONObject().put("enabled", enabled)
        } catch (e: Exception) {
            JSONObject().put("enabled", false).put("error", e.message)
        }
    }

    fun releaseTorch(context: Context) {
        try {
            setTorch(context, false)
        } catch (_: Exception) {
        }
    }

    fun getDeviceResourceStatus(): JSONObject = BridgeDeviceSession.status()

    fun releaseDeviceResources(context: Context, params: JSONObject): JSONObject {
        val resources = params.optJSONArray("resources")
        if (resources == null || resources.length() == 0) {
            BridgeDeviceSession.releaseAll(context)
        } else {
            for (i in 0 until resources.length()) {
                val name = resources.optString(i)
                val resource = when (name) {
                    "camera" -> BridgeDeviceSession.Resource.CAMERA
                    "gallery" -> BridgeDeviceSession.Resource.GALLERY
                    "contact" -> BridgeDeviceSession.Resource.CONTACT
                    "location" -> BridgeDeviceSession.Resource.LOCATION
                    else -> null
                }
                resource?.let { BridgeDeviceSession.cancel(it, context) }
            }
        }
        return JSONObject().put("released", true).put("status", BridgeDeviceSession.status())
    }

    fun cancelDeviceOperation(context: Context, params: JSONObject): JSONObject {
        val type = params.optString("type", params.optString("resource", ""))
        val resource = when (type) {
            "camera" -> BridgeDeviceSession.Resource.CAMERA
            "gallery" -> BridgeDeviceSession.Resource.GALLERY
            "contact" -> BridgeDeviceSession.Resource.CONTACT
            "location" -> BridgeDeviceSession.Resource.LOCATION
            else -> null
        }
        if (resource == null) {
            return JSONObject().put("cancelled", false).put("error", "Unknown resource: $type")
        }
        BridgeDeviceSession.cancel(resource, context)
        return JSONObject().put("cancelled", true).put("status", BridgeDeviceSession.status())
    }

    fun getCurrentLocation(context: Context, params: JSONObject, callback: (JSONObject?, String?) -> Unit) {
        if (!BridgeDeviceSession.tryOccupy(BridgeDeviceSession.Resource.LOCATION)) {
            callback(null, "Location request already in progress")
            return
        }
        val fine = BridgePermissions.isGranted(context, BridgePermissionNames.LOCATION)
        val coarse = BridgePermissions.isGranted(context, BridgePermissionNames.LOCATION_COARSE)
        if (!fine && !coarse) {
            BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
            callback(null, "Location permission not granted")
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val highAccuracy = params.optBoolean("enableHighAccuracy", true)
        val providers = mutableListOf<String>()
        if (highAccuracy && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }
        if (providers.isEmpty()) {
            BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
            callback(null, "Location providers disabled")
            return
        }
        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p)
                if (loc != null && (best == null || loc.time > best!!.time)) best = loc
            } catch (_: SecurityException) {
            }
        }
        val maxAge = params.optLong("maximumAge", 60_000L)
        if (best != null && System.currentTimeMillis() - best.time <= maxAge) {
            BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
            callback(locationToJson(best), null)
            return
        }
        val timeout = params.optLong("timeout", 15_000L)
        val handler = Handler(Looper.getMainLooper())
        var resolved = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (resolved) return
                resolved = true
                stopLocation(lm, this, handler)
                BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
                callback(locationToJson(location), null)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        BridgeDeviceSession.registerLocationCancel {
            if (!resolved) {
                resolved = true
                stopLocation(lm, listener, handler)
                BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
                callback(JSONObject().put("cancelled", true), null)
            }
        }
        val timeoutRunnable = Runnable {
            if (!resolved) {
                resolved = true
                stopLocation(lm, listener, handler)
                BridgeDeviceSession.clearLocationCancel()
                BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
                if (best != null) callback(locationToJson(best), null)
                else callback(null, "Location timeout")
            }
        }
        handler.postDelayed(timeoutRunnable, timeout)
        try {
            lm.requestLocationUpdates(providers[0], 0L, 0f, listener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
            callback(null, e.message)
        } catch (e: Exception) {
            BridgeDeviceSession.release(BridgeDeviceSession.Resource.LOCATION)
            callback(null, e.message)
        }
    }

    private fun stopLocation(lm: LocationManager, listener: LocationListener, handler: Handler) {
        try { lm.removeUpdates(listener) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
        BridgeDeviceSession.clearLocationCancel()
    }

    fun encodeImageUri(context: Context, uri: Uri, quality: Int, maxDim: Int = 1920): JSONObject? {
        val bitmap = decodeSampledBitmap(context, uri, maxDim) ?: return null
        val oriented = applyExifOrientation(context, uri, bitmap)
        if (oriented !== bitmap) bitmap.recycle()
        val scaled = scaleBitmap(oriented, maxDim)
        if (scaled !== oriented) oriented.recycle()
        val b64 = bitmapToBase64(scaled, quality)
        val result = JSONObject()
            .put("base64", b64)
            .put("mimeType", "image/jpeg")
            .put("width", scaled.width)
            .put("height", scaled.height)
        scaled.recycle()
        return result
    }

    fun encodeBitmap(bitmap: Bitmap?, quality: Int, maxDim: Int = 1920): JSONObject? {
        if (bitmap == null) return null
        val scaled = scaleBitmap(bitmap, maxDim)
        val recycleScaled = scaled !== bitmap
        val b64 = bitmapToBase64(scaled, quality)
        val result = JSONObject()
            .put("base64", b64)
            .put("mimeType", "image/jpeg")
            .put("width", scaled.width)
            .put("height", scaled.height)
        if (recycleScaled) scaled.recycle()
        return result
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDim)
        opts.inJustDecodeBounds = false
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun calculateInSampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        var halfW = w / 2
        var halfH = h / 2
        while (halfW / sample >= maxDim && halfH / sample >= maxDim) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return bitmap
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val rotation = when (
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> return bitmap
                }
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (_: Exception) {
            bitmap
        }
    }

    fun parseContactUri(context: Context, uri: Uri): JSONObject {
        val cr = context.contentResolver
        val cursor = cr.query(uri, null, null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val id = if (idIdx >= 0) c.getString(idIdx) else ""
                val name = if (nameIdx >= 0) c.getString(nameIdx) else ""
                var phone = ""
                var email = ""
                val phoneCursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(id),
                    null,
                )
                phoneCursor?.use { p ->
                    if (p.moveToFirst()) {
                        phone = p.getString(p.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }
                val emailCursor = cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
                    arrayOf(id),
                    null,
                )
                emailCursor?.use { e ->
                    if (e.moveToFirst()) {
                        email = e.getString(e.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                    }
                }
                return JSONObject()
                    .put("name", name)
                    .put("phone", phone)
                    .put("email", email)
                    .put("id", id)
            }
        }
        return JSONObject().put("cancelled", true)
    }

    private fun locationToJson(loc: Location) = JSONObject().apply {
        put("latitude", loc.latitude)
        put("longitude", loc.longitude)
        put("accuracy", loc.accuracy.toDouble())
        put("altitude", loc.altitude)
        put("heading", loc.bearing.toDouble())
        put("speed", loc.speed.toDouble())
        put("timestamp", loc.time)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun hasVibrator(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.hasVibrator()
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).hasVibrator()
        }
    }
}
