package com.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Launch Android intents and query handlers — used by the web bridge and React Native.
 */
object BridgeIntents {

    fun canOpenUrl(context: Context, url: String): JSONObject {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val can = intent.resolveActivity(context.packageManager) != null
        return JSONObject().apply {
            put("url", url)
            put("canOpen", can)
        }
    }

    fun openUrl(context: Context, url: String): JSONObject =
        launchIntent(
            context,
            JSONObject()
                .put("action", Intent.ACTION_VIEW)
                .put("data", url),
        )

    /**
     * params: action, data, type, package, chooser, chooserTitle, flags, extras
     */
    fun launchIntent(context: Context, params: JSONObject): JSONObject {
        val action = params.optString("action", Intent.ACTION_VIEW)
        val intent = Intent(action)

        params.optString("data").takeIf { it.isNotEmpty() }?.let { intent.data = Uri.parse(it) }
        params.optString("type").takeIf { it.isNotEmpty() }?.let { intent.type = it }
        params.optString("package").takeIf { it.isNotEmpty() }?.let { intent.setPackage(it) }
        params.optString("category").takeIf { it.isNotEmpty() }?.let { intent.addCategory(it) }

        params.optJSONObject("extras")?.let { extras ->
            extras.keys().forEach { key ->
                when (val v = extras.get(key)) {
                    is String -> intent.putExtra(key, v)
                    is Int -> intent.putExtra(key, v)
                    is Long -> intent.putExtra(key, v)
                    is Boolean -> intent.putExtra(key, v)
                    is Double -> intent.putExtra(key, v)
                    else -> intent.putExtra(key, v?.toString())
                }
            }
        }

        if (params.has("flags")) {
            intent.flags = params.optInt("flags")
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            val toLaunch = if (params.optBoolean("chooser", false)) {
                Intent.createChooser(
                    intent,
                    params.optString("chooserTitle", "Choose app"),
                ).apply {
                    if (!params.has("flags")) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                intent
            }
            context.startActivity(toLaunch)
            JSONObject()
                .put("opened", true)
                .put("action", action)
                .put("data", params.optString("data", null))
        } catch (e: Exception) {
            JSONObject()
                .put("opened", false)
                .put("action", action)
                .put("error", e.message)
        }
    }

    /** List apps that can handle an intent (e.g. all UPI handlers). */
    fun queryIntents(context: Context, params: JSONObject): JSONArray {
        val action = params.optString("action", Intent.ACTION_VIEW)
        val intent = Intent(action)
        params.optString("data").takeIf { it.isNotEmpty() }?.let { intent.data = Uri.parse(it) }
        params.optString("type").takeIf { it.isNotEmpty() }?.let { intent.type = it }

        @Suppress("DEPRECATION")
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val activities = context.packageManager.queryIntentActivities(intent, flags)
        val result = JSONArray()
        activities
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(context.packageManager).toString()
                JSONObject().apply {
                    put("packageName", pkg)
                    put("name", label)
                    put("activity", info.activityInfo.name)
                }
            }
            .distinctBy { it.getString("packageName") }
            .forEach { result.put(it) }
        return result
    }

    fun openAppSettings(context: Context): JSONObject {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject().put("opened", true)
        } catch (e: Exception) {
            JSONObject().put("opened", false).put("error", e.message)
        }
    }
}
