package com.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import org.json.JSONArray

object BridgePermissions {

    fun androidPermissions(webName: String): Array<String> = when (webName) {
        BridgePermissionNames.CAMERA -> arrayOf(android.Manifest.permission.CAMERA)
        BridgePermissionNames.MICROPHONE -> arrayOf(android.Manifest.permission.RECORD_AUDIO)
        BridgePermissionNames.LOCATION -> arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        BridgePermissionNames.LOCATION_COARSE ->
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        BridgePermissionNames.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else emptyArray()
        BridgePermissionNames.PHOTOS -> when {
            Build.VERSION.SDK_INT >= 33 -> arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        BridgePermissionNames.BLUETOOTH -> if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else emptyArray()
        BridgePermissionNames.CONTACTS -> arrayOf(android.Manifest.permission.READ_CONTACTS)
        else -> emptyArray()
    }

    fun getStatus(context: Context, webName: String): PermissionResult {
        val perms = androidPermissions(webName)
        if (perms.isEmpty()) {
            if (webName == BridgePermissionNames.NOTIFICATIONS && Build.VERSION.SDK_INT < 33) {
                return PermissionResult(webName, "granted", false)
            }
            return PermissionResult(webName, "unavailable", false)
        }
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) return PermissionResult(webName, "granted", false)

        val prefs = context.getSharedPreferences("bridge_perms", Context.MODE_PRIVATE)
        val requested = prefs.getBoolean("req_$webName", false)
        return PermissionResult(
            permission = webName,
            status = if (requested) "blocked" else "denied",
            canAskAgain = !requested,
        )
    }

    fun getAllStatuses(context: Context): JSONArray {
        val arr = JSONArray()
        BridgePermissionNames.ALL.forEach { arr.put(getStatus(context, it).toJson()) }
        return arr
    }

    fun request(context: Context, webName: String, callback: (PermissionResult) -> Unit) {
        val status = getStatus(context, webName)
        if (status.status == "granted") {
            callback(status)
            return
        }
        context.getSharedPreferences("bridge_perms", Context.MODE_PRIVATE)
            .edit().putBoolean("req_$webName", true).apply()
        BridgePermissionActivity.start(context.applicationContext, webName, callback)
    }

    fun openAppSettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    } catch (_: Exception) {
        false
    }

    fun isGranted(context: Context, webName: String): Boolean =
        getStatus(context, webName).status == "granted"
}
