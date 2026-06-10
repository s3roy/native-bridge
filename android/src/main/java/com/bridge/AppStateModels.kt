package com.bridge

import org.json.JSONObject

data class NetworkState(
    val connected: Boolean,
    val type: String,
) {
    fun toJson() = JSONObject().apply {
        put("connected", connected)
        put("type", type)
    }
}

data class WebViewState(
    val visible: Boolean,
    val focused: Boolean,
    val url: String?,
) {
    fun toJson() = JSONObject().apply {
        put("visible", visible)
        put("focused", focused)
        put("url", url ?: JSONObject.NULL)
    }
}

data class KeyboardState(
    val visible: Boolean,
    val height: Int,
) {
    fun toJson() = JSONObject().apply {
        put("visible", visible)
        put("height", height)
    }
}

data class SafeAreaInsets(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int,
) {
    fun toJson() = JSONObject().apply {
        put("top", top)
        put("bottom", bottom)
        put("left", left)
        put("right", right)
    }
}

data class BatteryState(
    val level: Float,
    val charging: Boolean,
    val lowPowerMode: Boolean,
) {
    fun toJson() = JSONObject().apply {
        put("level", level.toDouble())
        put("charging", charging)
        put("lowPowerMode", lowPowerMode)
    }
}

data class AudioRouteState(
    /** speaker | earpiece | bluetooth | wired | unknown */
    val route: String,
    val bluetoothConnected: Boolean,
    val deviceName: String?,
) {
    fun toJson() = JSONObject().apply {
        put("route", route)
        put("bluetoothConnected", bluetoothConnected)
        put("deviceName", deviceName ?: JSONObject.NULL)
    }
}

data class DisplayState(
    val width: Int,
    val height: Int,
    val density: Float,
    val fontScale: Float,
) {
    fun toJson() = JSONObject().apply {
        put("width", width)
        put("height", height)
        put("density", density.toDouble())
        put("fontScale", fontScale.toDouble())
    }
}

data class ThemeState(
    val darkMode: Boolean,
) {
    fun toJson() = JSONObject().apply {
        put("darkMode", darkMode)
    }
}

data class LocaleState(
    val language: String,
    val region: String,
    val timezone: String,
) {
    fun toJson() = JSONObject().apply {
        put("language", language)
        put("region", region)
        put("timezone", timezone)
    }
}

data class CallState(
    val inCall: Boolean,
    /** idle | ringing | active | unknown */
    val state: String,
) {
    fun toJson() = JSONObject().apply {
        put("inCall", inCall)
        put("state", state)
    }
}

/** Live snapshot of native app + device context. Pushed to web on every change. */
data class AppStateSnapshot(
    val lifecycle: String,
    val isForeground: Boolean,
    val isInPiP: Boolean,
    val network: NetworkState,
    val orientation: String,
    val webView: WebViewState?,
    val keyboard: KeyboardState,
    val safeArea: SafeAreaInsets,
    val battery: BatteryState,
    val audio: AudioRouteState,
    val display: DisplayState,
    val theme: ThemeState,
    val locale: LocaleState,
    val call: CallState,
    val timestamp: Long = System.currentTimeMillis(),
    val changed: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("lifecycle", lifecycle)
        put("isForeground", isForeground)
        put("isInPiP", isInPiP)
        put("network", network.toJson())
        put("orientation", orientation)
        put("webView", webView?.toJson() ?: JSONObject.NULL)
        put("keyboard", keyboard.toJson())
        put("safeArea", safeArea.toJson())
        put("battery", battery.toJson())
        put("audio", audio.toJson())
        put("display", display.toJson())
        put("theme", theme.toJson())
        put("locale", locale.toJson())
        put("call", call.toJson())
        put("timestamp", timestamp)
        put("changed", changed ?: JSONObject.NULL)
    }
}
