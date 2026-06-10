package com.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Detects installed UPI / wallet apps and opens UPI payment intents from the web bridge.
 */
object BridgePaymentApps {

    /** Known payment apps — checked even if intent query misses them. */
    private val KNOWN_APPS = listOf(
        PaymentAppDef("phonepe", "PhonePe", "com.phonepe.app", "phonepe", "upi"),
        PaymentAppDef("gpay", "Google Pay", "com.google.android.apps.nbu.paisa.user", "tez", "upi"),
        PaymentAppDef("paytm", "Paytm", "net.one97.paytm", "paytmmp", "upi"),
        PaymentAppDef("bhim", "BHIM UPI", "in.org.npci.upiapp", "bhim", "upi"),
        PaymentAppDef("amazonpay", "Amazon Pay", "in.amazon.mShop.android.shopping", "amazonpay", "wallet"),
        PaymentAppDef("mobikwik", "MobiKwik", "com.mobikwik_new", "mobikwik", "wallet"),
        PaymentAppDef("freecharge", "Freecharge", "com.freecharge.android", "freecharge", "wallet"),
        PaymentAppDef("cred", "CRED", "com.dreamplug.androidapp", "cred", "wallet"),
        PaymentAppDef("whatsapp", "WhatsApp", "com.whatsapp", "whatsapp", "messaging"),
        PaymentAppDef("whatsapp_business", "WhatsApp Business", "com.whatsapp.w4b", "whatsapp", "messaging"),
    )

    private data class PaymentAppDef(
        val id: String,
        val name: String,
        val packageName: String,
        val scheme: String,
        val category: String,
    )

    /** All installed UPI-capable apps (from intent query + known list). */
    fun getUpiApps(context: Context): JSONArray {
        val seen = mutableSetOf<String>()
        val result = JSONArray()

        // Apps that handle upi://pay
        queryUpiIntentHandlers(context).forEach { app ->
            if (seen.add(app.getString("packageName"))) result.put(app)
        }

        // Known apps by package name
        KNOWN_APPS.filter { it.category == "upi" }.forEach { def ->
            if (isPackageInstalled(context, def.packageName) && seen.add(def.packageName)) {
                result.put(def.toJson(context, true))
            }
        }

        return result
    }

    /** UPI + wallets + messaging apps useful for payments. */
    fun getPaymentApps(context: Context): JSONObject {
        val upi = getUpiApps(context)
        val wallets = JSONArray()
        val messaging = JSONArray()
        val all = JSONArray()

        KNOWN_APPS.forEach { def ->
            val installed = isPackageInstalled(context, def.packageName)
            val json = def.toJson(context, installed)
            all.put(json)
            if (installed) {
                when (def.category) {
                    "wallet" -> wallets.put(json)
                    "messaging" -> messaging.put(json)
                }
            }
        }

        // Merge UPI into all without duplicates
        val seen = mutableSetOf<String>()
        for (i in 0 until all.length()) seen.add(all.getJSONObject(i).getString("id"))
        for (i in 0 until upi.length()) {
            val app = upi.getJSONObject(i)
            if (seen.add(app.optString("id", app.getString("packageName")))) {
                all.put(app)
            }
        }

        return JSONObject().apply {
            put("upi", upi)
            put("wallets", wallets)
            put("messaging", messaging)
            put("all", all)
            put("platform", "android")
        }
    }

    fun canOpenUrl(context: Context, url: String): JSONObject =
        BridgeIntents.canOpenUrl(context, url)

    fun openUrl(context: Context, url: String): JSONObject =
        BridgeIntents.openUrl(context, url)

    /**
     * Open UPI payment chooser.
     * params: vpa/pa, amount/am, name/pn, note/tn, txnId/tr, currency/cu (default INR)
     */
    fun openUpiPayment(context: Context, params: JSONObject): JSONObject {
        val vpa = params.optString("vpa", params.optString("pa", ""))
        if (vpa.isEmpty()) {
            return JSONObject().put("opened", false).put("error", "vpa (payee address) is required")
        }
        val uri = buildUpiUri(params)
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Pay with UPI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            JSONObject()
                .put("opened", true)
                .put("uri", uri.toString())
                .put("availableApps", getUpiApps(context))
        } catch (e: Exception) {
            JSONObject().put("opened", false).put("error", e.message).put("uri", uri.toString())
        }
    }

    fun buildUpiUri(params: JSONObject): Uri {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", params.optString("vpa", params.optString("pa")))
            .appendQueryParameter("pn", params.optString("name", params.optString("pn", "")))
            .appendQueryParameter("am", params.optString("amount", params.optString("am", "")))
            .appendQueryParameter("cu", params.optString("currency", params.optString("cu", "INR")))
            .appendQueryParameter("tn", params.optString("note", params.optString("tn", "")))
            .appendQueryParameter("tr", params.optString("txnId", params.optString("tr", "")))
        return builder.build()
    }

    private fun queryUpiIntentHandlers(context: Context): List<JSONObject> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        @Suppress("DEPRECATION")
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val activities = context.packageManager.queryIntentActivities(intent, flags)
        return activities.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(context.packageManager).toString()
            val known = KNOWN_APPS.find { it.packageName == pkg }
            JSONObject().apply {
                put("id", known?.id ?: pkg)
                put("name", known?.name ?: label)
                put("packageName", pkg)
                put("scheme", known?.scheme ?: "upi")
                put("category", known?.category ?: "upi")
                put("installed", true)
            }
        }.distinctBy { it.getString("packageName") }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun PaymentAppDef.toJson(context: Context, installed: Boolean): JSONObject {
        val label = if (installed) {
            try {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(packageName, 0),
                ).toString()
            } catch (_: Exception) {
                name
            }
        } else name
        return JSONObject().apply {
            put("id", id)
            put("name", label)
            put("packageName", packageName)
            put("scheme", scheme)
            put("category", category)
            put("installed", installed)
        }
    }
}
