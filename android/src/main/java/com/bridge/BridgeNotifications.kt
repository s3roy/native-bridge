package com.bridge

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

/** Android notification shade / app notifications — read and control from web. */
object BridgeNotifications {

    fun getSettings(context: Context): JSONObject {
        val nmCompat = NotificationManagerCompat.from(context)
        val enabled = nmCompat.areNotificationsEnabled()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm.activeNotifications.map { n ->
                JSONObject()
                    .put("id", n.id)
                    .put("tag", n.tag ?: JSONObject.NULL)
                    .put("package", n.packageName)
            }
        } else {
            emptyList()
        }
        return JSONObject()
            .put("enabled", enabled)
            .put("activeCount", active.size)
            .put("active", JSONArray(active))
    }

    fun cancel(context: Context, params: JSONObject): JSONObject {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = params.optInt("id", -1)
        val tag = params.optString("tag", null)
        if (id >= 0) {
            if (tag != null) nm.cancel(tag, id) else nm.cancel(id)
        }
        return JSONObject().put("cancelled", id >= 0)
    }

    fun cancelAll(context: Context): JSONObject {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
        return JSONObject().put("cancelledAll", true)
    }

    fun openSettings(context: Context): JSONObject {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            context.startActivity(intent)
            JSONObject().put("opened", true)
        } catch (_: Exception) {
            JSONObject().put("opened", false)
        }
    }
}
