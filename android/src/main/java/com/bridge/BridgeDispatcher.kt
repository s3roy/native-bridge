package com.bridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Shared request router for WebView JS bridge and React Native module. */
object BridgeDispatcher {

    fun dispatch(
        context: Context,
        method: String,
        params: JSONObject,
        webViewId: String? = null,
    ): Any? = when (method) {
        "bridge.getApiCalls" -> NativeBridge.apiCalls.query(params)
        "bridge.getNotifications" -> JSONArray(NativeBridge.notifications.all().map { it.toJson() })
        "bridge.getData" -> NativeBridge.getData(params.optString("key"))
        "bridge.getAllData" -> JSONObject(NativeBridge.getAllData())
        "bridge.setData" -> {
            val key = params.optString("key")
            NativeBridge.setData(key, params.opt("value"), webViewId)
            JSONObject().put("key", key).put("stored", true)
        }
        "bridge.removeData" -> {
            val key = params.optString("key")
            NativeBridge.removeData(key, webViewId)
            JSONObject().put("key", key).put("removed", true)
        }
        "bridge.getWebViewId" -> JSONObject().put("id", webViewId ?: "unknown")
        "bridge.getDeviceInfo" -> JSONObject(NativeBridge.getDeviceInfo())
        "bridge.getAppState" -> AppStateMonitor.currentJson()
        "bridge.getPermissionStatus" -> BridgePermissions.getStatus(
            context,
            params.optString("permission"),
        ).toJson()
        "bridge.getPermissions" -> BridgePermissions.getAllStatuses(context)
        "bridge.getUpiApps" -> BridgePaymentApps.getUpiApps(context)
        "bridge.getPaymentApps" -> BridgePaymentApps.getPaymentApps(context)
        "bridge.canOpenUrl" -> BridgeIntents.canOpenUrl(context, params.optString("url"))
        "bridge.openUrl" -> BridgeIntents.openUrl(context, params.optString("url"))
        "bridge.launchIntent" -> BridgeIntents.launchIntent(context, params)
        "bridge.queryIntents" -> BridgeIntents.queryIntents(context, params)
        "bridge.openUpiPayment" -> BridgePaymentApps.openUpiPayment(context, params)
        "bridge.buildUpiUri" -> JSONObject().put(
            "uri",
            BridgePaymentApps.buildUpiUri(params).toString(),
        )
        "bridge.openSettings" -> BridgeIntents.openAppSettings(context)
        "bridge.getCapabilities" -> BridgeDevice.getCapabilities(context)
        "bridge.getClipboard" -> BridgeDevice.getClipboard(context)
        "bridge.setClipboard" -> BridgeDevice.setClipboard(context, params.optString("text"))
        "bridge.vibrate" -> BridgeDevice.vibrate(context, params)
        "bridge.dial" -> BridgeDevice.dial(context, params.optString("phone"))
        "bridge.sendSms" -> BridgeDevice.sendSms(
            context,
            params.optString("phone"),
            params.optString("body", ""),
        )
        "bridge.openMaps" -> BridgeDevice.openMaps(context, params)
        "bridge.setTorch" -> BridgeDevice.setTorch(context, params.optBoolean("enabled", true))
        "bridge.getDeviceResourceStatus" -> BridgeDevice.getDeviceResourceStatus()
        "bridge.releaseDeviceResources" -> BridgeDevice.releaseDeviceResources(context, params)
        "bridge.cancelDeviceOperation" -> BridgeDevice.cancelDeviceOperation(context, params)
        "bridge.getSafeAreaInsets" -> AppStateMonitor.getSafeArea().toJson()
        "bridge.setSystemUi" -> BridgeSystemUi.set(context, params)
        "bridge.getSystemUi" -> BridgeSystemUi.get()
        "bridge.getNotificationSettings" -> BridgeNotifications.getSettings(context)
        "bridge.cancelNotification" -> BridgeNotifications.cancel(context, params)
        "bridge.cancelAllNotifications" -> BridgeNotifications.cancelAll(context)
        "bridge.openNotificationSettings" -> BridgeNotifications.openSettings(context)
        "bridge.getWebViewCacheInfo" -> BridgeWebViewCache.getInfo(null)
        "bridge.setWebViewCachePolicy" -> BridgeWebViewCache.setPolicy(null, params)
        else -> throw IllegalArgumentException("Unknown method: $method")
    }

    /**
     * Async methods resolve via [onResult]. Returns true if handled asynchronously.
     */
    fun dispatchAsync(
        context: Context,
        method: String,
        params: JSONObject,
        onResult: (result: Any?, error: String?) -> Unit,
    ): Boolean = when (method) {
        "bridge.requestPermission" -> {
            BridgePermissions.request(context, params.optString("permission")) { result ->
                onResult(result.toJson(), null)
            }
            true
        }
        "bridge.getCurrentLocation" -> {
            BridgeDevice.getCurrentLocation(context, params) { result, error ->
                onResult(result, error)
            }
            true
        }
        "bridge.takePhoto" -> {
            BridgeDeviceActivity.start(context, BridgeDeviceActivity.Action.TAKE_PHOTO, params) { r, e ->
                onResult(r, e)
            }
            true
        }
        "bridge.pickImage" -> {
            val action = if (params.optBoolean("multiple", false)) {
                BridgeDeviceActivity.Action.PICK_IMAGES
            } else {
                BridgeDeviceActivity.Action.PICK_IMAGE
            }
            BridgeDeviceActivity.start(context, action, params) { r, e ->
                onResult(r, e)
            }
            true
        }
        "bridge.pickImages" -> {
            BridgeDeviceActivity.start(context, BridgeDeviceActivity.Action.PICK_IMAGES, params) { r, e ->
                onResult(r, e)
            }
            true
        }
        "bridge.pickContact" -> {
            BridgeDeviceActivity.start(context, BridgeDeviceActivity.Action.PICK_CONTACT, params) { r, e ->
                onResult(r, e)
            }
            true
        }
        "bridge.share" -> {
            BridgeDeviceActivity.start(context, BridgeDeviceActivity.Action.SHARE, params) { r, e ->
                onResult(r, e)
            }
            true
        }
        else -> false
    }
}
