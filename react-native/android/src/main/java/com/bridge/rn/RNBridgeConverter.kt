package com.bridge.rn

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import org.json.JSONArray
import org.json.JSONObject

internal object RNBridgeConverter {

    fun toJson(map: ReadableMap?): JSONObject {
        val json = JSONObject()
        if (map == null) return json
        val iterator = map.entryIterator
        while (iterator.hasNext()) {
            val entry = iterator.next()
            json.put(entry.key, toJsonValue(entry.value))
        }
        return json
    }

    private fun toJsonValue(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is ReadableMap -> toJson(value)
        is ReadableArray -> toJsonArray(value)
        is Boolean -> value
        is Int -> value
        is Double -> value
        is String -> value
        else -> value.toString()
    }

    private fun toJsonArray(array: ReadableArray): JSONArray {
        val json = JSONArray()
        for (i in 0 until array.size()) {
            json.put(
                when (array.getType(i)) {
                    ReadableType.Null -> JSONObject.NULL
                    ReadableType.Boolean -> array.getBoolean(i)
                    ReadableType.Number -> array.getDouble(i)
                    ReadableType.String -> array.getString(i)
                    ReadableType.Map -> toJson(array.getMap(i))
                    ReadableType.Array -> toJsonArray(array.getArray(i)!!)
                    else -> JSONObject.NULL
                },
            )
        }
        return json
    }

    fun putPayload(map: WritableMap, key: String, value: Any?) {
        when (val converted = fromAny(value)) {
            null -> map.putNull(key)
            is Boolean -> map.putBoolean(key, converted)
            is Int -> map.putDouble(key, converted.toDouble())
            is Double -> map.putDouble(key, converted)
            is String -> map.putString(key, converted)
            is WritableMap -> map.putMap(key, converted)
            is WritableArray -> map.putArray(key, converted)
            else -> map.putString(key, converted.toString())
        }
    }

    fun fromAny(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> fromJsonObject(value)
        is JSONArray -> fromJsonArray(value)
        is Boolean, is Int, is Long, is Double, is String -> value
        else -> value.toString()
    }

    private fun fromJsonObject(obj: JSONObject): WritableMap {
        val map = Arguments.createMap()
        obj.keys().forEach { key ->
            when (val v = obj.get(key)) {
                JSONObject.NULL -> map.putNull(key)
                is Boolean -> map.putBoolean(key, v)
                is Int -> map.putInt(key, v)
                is Long -> map.putDouble(key, v.toDouble())
                is Double -> map.putDouble(key, v)
                is String -> map.putString(key, v)
                is JSONObject -> map.putMap(key, fromJsonObject(v))
                is JSONArray -> map.putArray(key, fromJsonArray(v))
                else -> map.putString(key, v?.toString())
            }
        }
        return map
    }

    private fun fromJsonArray(arr: JSONArray): WritableArray {
        val array = Arguments.createArray()
        for (i in 0 until arr.length()) {
            when (val v = arr.get(i)) {
                JSONObject.NULL -> array.pushNull()
                is Boolean -> array.pushBoolean(v)
                is Int -> array.pushInt(v)
                is Long -> array.pushDouble(v.toDouble())
                is Double -> array.pushDouble(v)
                is String -> array.pushString(v)
                is JSONObject -> array.pushMap(fromJsonObject(v))
                is JSONArray -> array.pushArray(fromJsonArray(v))
                else -> array.pushString(v?.toString())
            }
        }
        return array
    }
}
