package com.bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Transparent activity for camera, gallery, contact picker, and share sheet. */
class BridgeDeviceActivity : Activity() {

    enum class Action { TAKE_PHOTO, PICK_IMAGE, PICK_IMAGES, PICK_CONTACT, SHARE }

    companion object {
        private const val EXTRA_ACTION = "bridge.device.action"
        private const val EXTRA_PARAMS = "bridge.device.params"
        private const val REQ_CAMERA = 9901
        private const val REQ_GALLERY = 9902
        private const val REQ_GALLERY_MULTI = 9904
        private const val REQ_CONTACT = 9903

        private fun Action.toResource(): BridgeDeviceSession.Resource = when (this) {
            Action.TAKE_PHOTO -> BridgeDeviceSession.Resource.CAMERA
            Action.PICK_IMAGE, Action.PICK_IMAGES -> BridgeDeviceSession.Resource.GALLERY
            Action.PICK_CONTACT -> BridgeDeviceSession.Resource.CONTACT
            Action.SHARE -> BridgeDeviceSession.Resource.SHARE
        }

        fun start(
            context: Context,
            action: Action,
            params: JSONObject,
            callback: (JSONObject?, String?) -> Unit,
        ) {
            val resource = action.toResource()
            if (!BridgeDeviceSession.tryOccupy(resource)) {
                callback(null, "${resource.name.lowercase()} is busy")
                return
            }
            BridgeDeviceActivityCallback.register(resource, callback)
            context.startActivity(
                Intent(context, BridgeDeviceActivity::class.java).apply {
                    putExtra(EXTRA_ACTION, action.name)
                    putExtra(EXTRA_PARAMS, params.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }

        private fun Action.toResource() = when (this) {
            Action.TAKE_PHOTO -> BridgeDeviceSession.Resource.CAMERA
            Action.PICK_IMAGE, Action.PICK_IMAGES -> BridgeDeviceSession.Resource.GALLERY
            Action.PICK_CONTACT -> BridgeDeviceSession.Resource.CONTACT
            Action.SHARE -> BridgeDeviceSession.Resource.SHARE
        }
    }

    private lateinit var action: Action
    private lateinit var resource: BridgeDeviceSession.Resource
    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private var quality = 80
    private var maxDimension = 1920
    private var maxCount = 10
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionName = intent.getStringExtra(EXTRA_ACTION) ?: run {
            complete(null, "Invalid action"); return
        }
        action = Action.valueOf(actionName)
        resource = action.toResource()
        val params = try {
            JSONObject(intent.getStringExtra(EXTRA_PARAMS) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        quality = params.optInt("quality", 80).coerceIn(1, 100)
        maxDimension = params.optInt("maxDimension", 1920).coerceAtLeast(256)
        maxCount = params.optInt("maxCount", 10).coerceIn(1, 20)

        if (savedInstanceState != null) {
            complete(JSONObject().put("cancelled", true).put("reason", "activity_recreated"), null)
            return
        }

        when (action) {
            Action.TAKE_PHOTO -> launchCamera()
            Action.PICK_IMAGE -> launchGallery(multiple = false)
            Action.PICK_IMAGES -> launchGallery(multiple = true)
            Action.PICK_CONTACT -> launchContact()
            Action.SHARE -> launchShare(params)
        }
    }

    private fun launchCamera() {
        if (!BridgePermissions.isGranted(this, BridgePermissionNames.CAMERA)) {
            complete(null, "Camera permission not granted"); return
        }
        val handlers = packageManager.queryIntentActivities(Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0)
        if (handlers.isEmpty()) {
            complete(null, "No camera app available"); return
        }
        val file = File(cacheDir, "bridge/photo_${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs()
        }
        photoFile = file
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.bridge.fileprovider",
            file,
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        handlers.forEach { ri ->
            grantUriPermission(
                ri.activityInfo.packageName,
                photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_CAMERA)
        } catch (e: Exception) {
            complete(null, e.message ?: "Failed to open camera")
        }
    }

    private fun launchGallery(multiple: Boolean) {
        val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            if (multiple && Build.VERSION.SDK_INT >= 18) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent.createChooser(pick, if (multiple) "Select images" else "Select image"),
                if (multiple) REQ_GALLERY_MULTI else REQ_GALLERY,
            )
        } catch (e: Exception) {
            complete(null, e.message ?: "Failed to open gallery")
        }
    }

    private fun launchContact() {
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                REQ_CONTACT,
            )
        } catch (e: Exception) {
            complete(null, e.message ?: "Failed to open contacts")
        }
    }

    private fun launchShare(params: JSONObject) {
        val text = params.optString("text", "")
        val url = params.optString("url", "")
        val title = params.optString("title", "Share")
        val body = listOf(text, url).filter { it.isNotEmpty() }.joinToString("\n")
        try {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(share, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            complete(JSONObject().put("shared", true), null)
        } catch (e: Exception) {
            complete(null, e.message)
        }
        finish()
        overridePendingTransition(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (finished) return
        if (resultCode != RESULT_OK) {
            cleanupPhotoFile()
            complete(JSONObject().put("cancelled", true), null)
            finish()
            overridePendingTransition(0, 0)
            return
        }
        when (requestCode) {
            REQ_CAMERA -> {
                val result = when {
                    photoFile?.exists() == true && (photoFile?.length() ?: 0) > 0 && photoUri != null ->
                        BridgeDevice.encodeImageUri(this, photoUri!!, quality, maxDimension)
                    else -> {
                        @Suppress("DEPRECATION")
                        BridgeDevice.encodeBitmap(data?.extras?.get("data") as? Bitmap, quality, maxDimension)
                    }
                }
                cleanupPhotoFile()
                complete(result, if (result == null) "Failed to capture photo" else null)
            }
            REQ_GALLERY -> {
                val uri = data?.data
                if (uri == null) complete(JSONObject().put("cancelled", true), null)
                else {
                    val result = BridgeDevice.encodeImageUri(this, uri, quality, maxDimension)
                    complete(result, if (result == null) "Failed to read image" else null)
                }
            }
            REQ_GALLERY_MULTI -> {
                val uris = mutableListOf<Uri>()
                data?.data?.let { uris.add(it) }
                data?.clipData?.let { clip ->
                    for (i in 0 until minOf(clip.itemCount, maxCount)) uris.add(clip.getItemAt(i).uri)
                }
                if (uris.isEmpty()) {
                    complete(JSONObject().put("cancelled", true), null)
                } else {
                    val images = JSONArray()
                    uris.forEach { uri ->
                        BridgeDevice.encodeImageUri(this, uri, quality, maxDimension)?.let { images.put(it) }
                    }
                    complete(
                        JSONObject().put("images", images).put("count", images.length()),
                        if (images.length() == 0) "Failed to read images" else null,
                    )
                }
            }
            REQ_CONTACT -> {
                val uri = data?.data
                if (uri == null) complete(JSONObject().put("cancelled", true), null)
                else complete(BridgeDevice.parseContactUri(this, uri), null)
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        if (!finished) {
            cleanupPhotoFile()
            complete(JSONObject().put("cancelled", true).put("reason", "destroyed"), null)
        }
        super.onDestroy()
    }

    private fun cleanupPhotoFile() {
        try { photoFile?.delete() } catch (_: Exception) { }
        photoFile = null
        photoUri = null
    }

    private fun complete(result: JSONObject?, error: String?) {
        if (finished) return
        finished = true
        BridgeDeviceActivityCallback.deliver(resource, result, error)
    }

    private fun Action.toResource() = when (this) {
        Action.TAKE_PHOTO -> BridgeDeviceSession.Resource.CAMERA
        Action.PICK_IMAGE, Action.PICK_IMAGES -> BridgeDeviceSession.Resource.GALLERY
        Action.PICK_CONTACT -> BridgeDeviceSession.Resource.CONTACT
        Action.SHARE -> BridgeDeviceSession.Resource.SHARE
    }
}

internal object BridgeDeviceActivityCallback {
    private val lock = Any()
    private val callbacks = mutableMapOf<BridgeDeviceSession.Resource, (JSONObject?, String?) -> Unit>()

    fun register(resource: BridgeDeviceSession.Resource, cb: (JSONObject?, String?) -> Unit) {
        synchronized(lock) { callbacks[resource] = cb }
    }

    fun deliver(resource: BridgeDeviceSession.Resource, result: JSONObject?, error: String?) {
        val cb = synchronized(lock) { callbacks.remove(resource) }
        BridgeDeviceSession.release(resource)
        cb?.invoke(result, error)
    }
}
