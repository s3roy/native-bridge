package com.bridge.rn

import com.bridge.BridgeDispatcher
import com.bridge.NativeBridge
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONObject

class NativeWebViewBridgeModule(
    private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {

    private val emitRelay: (String, Any?) -> Unit = { event, payload ->
        sendBridgeEvent(event, payload)
    }

    init {
        NativeBridge.addEmitListener(emitRelay)
    }

    override fun getName(): String = "NativeWebViewBridge"

    override fun onCatalystInstanceDestroy() {
        NativeBridge.removeEmitListener(emitRelay)
        super.onCatalystInstanceDestroy()
    }

    /** Android library auto-starts via BridgeInitProvider — no-op for API parity with iOS. */
    @ReactMethod
    fun start(promise: Promise) {
        promise.resolve(null)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for NativeEventEmitter on RN 0.65+
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for NativeEventEmitter on RN 0.65+
    }

    @ReactMethod
    fun putData(key: String, value: String, promise: Promise) {
        NativeBridge.putData(key, value)
        promise.resolve(null)
    }

  @ReactMethod
    fun publishEvent(event: String, params: ReadableMap?, promise: Promise) {
        val payload = params?.let { RNBridgeConverter.toJson(it) } ?: JSONObject.NULL
        NativeBridge.publishEvent(event, payload)
        promise.resolve(null)
    }

    @ReactMethod
    fun dispatch(method: String, params: ReadableMap?, promise: Promise) {
        val json = RNBridgeConverter.toJson(params ?: Arguments.createMap())
        val ctx = reactApplicationContext

        if (BridgeDispatcher.dispatchAsync(ctx, method, json) { result, error ->
                if (error != null) promise.reject("BRIDGE_ERROR", error)
                else promise.resolve(RNBridgeConverter.fromAny(result))
            }
        ) {
            return
        }

        try {
            val result = BridgeDispatcher.dispatch(ctx, method, json)
            promise.resolve(RNBridgeConverter.fromAny(result))
        } catch (e: Exception) {
            promise.reject("BRIDGE_ERROR", e.message, e)
        }
    }

    private fun sendBridgeEvent(event: String, payload: Any?) {
        val params = Arguments.createMap().apply {
            putString("event", event)
            RNBridgeConverter.putPayload(this, "payload", payload)
        }
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(EVENT_NAME, params)
    }

    companion object {
        const val EVENT_NAME = "NativeBridgeEvent"
    }
}
