package com.bridge

import org.json.JSONObject

/** Result of a permission check or request. */
data class PermissionResult(
    val permission: String,
    /** granted | denied | blocked | unavailable */
    val status: String,
    val canAskAgain: Boolean,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("permission", permission)
        put("status", status)
        put("canAskAgain", canAskAgain)
    }
}

/** Supported permission names (same on web, Android, iOS). */
object BridgePermissionNames {
    const val CAMERA = "camera"
    const val MICROPHONE = "microphone"
    const val LOCATION = "location"
    const val LOCATION_COARSE = "locationCoarse"
    const val NOTIFICATIONS = "notifications"
    const val PHOTOS = "photos"
    const val BLUETOOTH = "bluetooth"
    const val CONTACTS = "contacts"

    val ALL = listOf(
        CAMERA, MICROPHONE, LOCATION, LOCATION_COARSE,
        NOTIFICATIONS, PHOTOS, BLUETOOTH, CONTACTS,
    )
}
