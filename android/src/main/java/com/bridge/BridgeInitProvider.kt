package com.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build

/**
 * Zero-config auto-initialization. Android instantiates ContentProviders before
 * Application.onCreate, so the bridge sets itself up with no host-app code. This
 * is the same trick libraries like Firebase / WorkManager use.
 */
class BridgeInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return true
        val app = ctx.applicationContext
        if (app is android.app.Application) {
            AppStateMonitor.start(app)
        }
        NativeBridge.setDeviceInfoProvider {
            mapOf(
                "platform" to "android",
                "model" to Build.MODEL,
                "manufacturer" to Build.MANUFACTURER,
                "osVersion" to Build.VERSION.RELEASE,
                "sdkInt" to Build.VERSION.SDK_INT,
                "appPackage" to ctx.packageName,
            )
        }
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?,
    ): Int = 0
}
