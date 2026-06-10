package com.bridge

import org.json.JSONObject

/**
 * Tracks camera, gallery, location, and other device resources so they are not
 * double-opened and are always released after use or cancel.
 */
object BridgeDeviceSession {

    enum class Resource {
        CAMERA, GALLERY, CONTACT, LOCATION, SHARE,
    }

    private val lock = Any()
    private val busy = mutableSetOf<Resource>()
    private var locationCancel: (() -> Unit)? = null

    fun tryOccupy(resource: Resource): Boolean = synchronized(lock) {
        if (busy.contains(resource)) return false
        busy.add(resource)
        true
    }

    fun release(resource: Resource) = synchronized(lock) {
        busy.remove(resource)
    }

    fun releaseAll(context: android.content.Context?) {
        synchronized(lock) {
            locationCancel?.invoke()
            locationCancel = null
            busy.clear()
        }
        context?.let { BridgeDevice.releaseTorch(it) }
    }

    fun cancel(resource: Resource, context: android.content.Context? = null) {
        synchronized(lock) {
            when (resource) {
                Resource.LOCATION -> {
                    locationCancel?.invoke()
                    locationCancel = null
                }
                else -> {}
            }
            busy.remove(resource)
        }
        if (resource == Resource.CAMERA) {
            context?.let { BridgeDevice.releaseTorch(it) }
        }
    }

    fun registerLocationCancel(cancel: () -> Unit) {
        synchronized(lock) { locationCancel = cancel }
    }

    fun clearLocationCancel() {
        synchronized(lock) { locationCancel = null }
    }

    fun isBusy(resource: Resource): Boolean = synchronized(lock) { busy.contains(resource) }

    fun status(): JSONObject = synchronized(lock) {
        JSONObject().apply {
            put("camera", if (busy.contains(Resource.CAMERA)) "busy" else "idle")
            put("gallery", if (busy.contains(Resource.GALLERY)) "busy" else "idle")
            put("contact", if (busy.contains(Resource.CONTACT)) "busy" else "idle")
            put("location", if (busy.contains(Resource.LOCATION)) "busy" else "idle")
        }
    }
}
