package com.bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Transparent activity that shows the native permission dialog for web requests. */
class BridgePermissionActivity : Activity() {

    companion object {
        private var pendingCallback: ((PermissionResult) -> Unit)? = null
        private const val EXTRA_PERMISSION = "bridge.permission"
        private const val REQUEST_CODE = 8801

        fun start(context: Context, webName: String, callback: (PermissionResult) -> Unit) {
            pendingCallback = callback
            context.startActivity(
                Intent(context, BridgePermissionActivity::class.java).apply {
                    putExtra(EXTRA_PERMISSION, webName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    private lateinit var webName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webName = intent.getStringExtra(EXTRA_PERMISSION) ?: run {
            finishWith(PermissionResult("", "unavailable", false))
            return
        }
        val perms = BridgePermissions.androidPermissions(webName)
        if (perms.isEmpty()) {
            val status = if (webName == BridgePermissionNames.NOTIFICATIONS) "granted" else "unavailable"
            finishWith(PermissionResult(webName, status, false))
            return
        }
        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            finishWith(PermissionResult(webName, "granted", false))
            return
        }
        ActivityCompat.requestPermissions(this, perms, REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != REQUEST_CODE) return
        val granted = grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        finishWith(
            if (granted) {
                PermissionResult(webName, "granted", false)
            } else {
                val blocked = permissions.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
                PermissionResult(
                    webName,
                    if (blocked) "blocked" else "denied",
                    canAskAgain = !blocked,
                )
            },
        )
    }

    private fun finishWith(result: PermissionResult) {
        pendingCallback?.invoke(result)
        pendingCallback = null
        NativeBridge.emit("permission.change", result.toJson())
        finish()
        overridePendingTransition(0, 0)
    }
}
