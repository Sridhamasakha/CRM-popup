package com.hkmcrm.popup

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ContactHit(
    val matched: Boolean,
    val name: String = "",
    val kind: String = "",
    val category: String = "",
    val lastInteraction: String = "",
    val openOpportunity: String = "",
    val assignedTo: String = "",
    val mobile: String = ""
)

object Crm {
    private const val TIMEOUT = 12000

    private fun post(context: Context, path: String, body: JSONObject): JSONObject? {
        return try {
            val url = URL(BuildConfig.BASE_URL + path)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-device-key", Prefs.deviceKey(context))
            conn.setRequestProperty("x-user-email", Prefs.userEmail(context))
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code in 200..299 && text.isNotBlank()) JSONObject(text) else null
        } catch (t: Throwable) {
            null
        }
    }

    fun lookup(context: Context, mobile: String): ContactHit? {
        val res = post(context, "/crmContactLookup", JSONObject().put("mobile", mobile)) ?: return null
        return ContactHit(
            matched = res.optBoolean("matched", false),
            name = res.optString("name"),
            kind = res.optString("kind"),
            category = res.optString("category"),
            lastInteraction = res.optString("lastInteraction"),
            openOpportunity = res.optString("openOpportunity"),
            assignedTo = res.optString("assignedTo"),
            mobile = res.optString("mobile", mobile)
        )
    }

    fun logCall(context: Context, mobile: String, direction: String, startedAtIso: String, seconds: Int): ContactHit? {
        val body = JSONObject()
            .put("mobile", mobile)
            .put("direction", direction)
            .put("startedAt", startedAtIso)
            .put("durationSeconds", seconds)
        val res = post(context, "/crmLogDeviceCall", body) ?: return null
        if (!res.optBoolean("matched", false)) return null
        return ContactHit(matched = true, name = res.optString("name"), mobile = mobile)
    }
}
