package com.hkmcrm.popup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CallWatchService : Service() {

    private val pool = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var listener: PhoneStateListener? = null
    private var calling = false
    private var startedAt = 0L
    private var seenNumber = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(Notifier.FOREGROUND_ID, Notifier.foreground(this))
        } catch (t: Throwable) {
            stopSelf()
            return
        }
        try {
            register()
        } catch (t: Throwable) {
            // keep the service alive even if the telephony hook is unavailable
        }
    }

    @Suppress("DEPRECATION")
    private fun register() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val l = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        calling = true
                        startedAt = System.currentTimeMillis()
                        seenNumber = incomingNumber ?: ""
                        if (seenNumber.isNotBlank()) safe { incomingLookup(seenNumber, true) }
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (!calling) {
                            calling = true
                            startedAt = System.currentTimeMillis()
                            seenNumber = incomingNumber ?: ""
                        }
                        val hint = incomingNumber ?: ""
                        if (hint.isNotBlank()) safe { incomingLookup(hint, false) }
                        main.postDelayed({ outboundLookup() }, 1200)
                        main.postDelayed({ outboundLookup() }, 4000)
                        main.postDelayed({ outboundLookup() }, 8000)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (calling) {
                            calling = false
                            val beganAt = startedAt
                            main.postDelayed({ finishCall(beganAt) }, 1500)
                        }
                    }
                }
            }
        }
        listener = l
        tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun safe(block: () -> Unit) {
        pool.execute { try { block() } catch (_: Throwable) {} }
    }

    private fun outboundLookup() {
        if (!calling) return
        if (!Prefs.isEnabled(this)) return
        val entry = lastCallEntry() ?: return
        if (entry.third != CallLog.Calls.OUTGOING_TYPE) return
        val number = entry.first
        if (number.isBlank()) return
        safe { incomingLookup(number, false) }
    }

    private fun incomingLookup(number: String, incoming: Boolean) {
        if (!Prefs.isEnabled(this)) return
        val hit = Crm.lookup(this, number) ?: return
        if (!hit.matched) return
        main.post { Notifier.popup(this, hit, incoming) }
    }

    private fun finishCall(beganAt: Long) {
        try {
            val entry = lastCallEntry() ?: return
            val number = entry.first
            val seconds = entry.second
            val outgoing = entry.third == CallLog.Calls.OUTGOING_TYPE
            if (number.isBlank()) return
            if (seconds <= 0) return
            if (!Prefs.isEnabled(this)) return
            val iso = isoTime(if (beganAt > 0) beganAt else System.currentTimeMillis() - seconds * 1000L)
            val hit = Crm.logCall(this, number, if (outgoing) "outbound" else "inbound", iso, seconds)
            if (hit != null) {
                main.post { Notifier.popup(this, hit.copy(name = hit.name + " · call logged"), !outgoing) }
            }
        } catch (_: Throwable) {
        }
    }

    private fun lastCallEntry(): Triple<String, Int, Int>? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                null, null, CallLog.Calls.DATE + " DESC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val ageMs = System.currentTimeMillis() - cursor.getLong(3)
                if (ageMs > 180000) return null
                Triple(cursor.getString(0) ?: "", cursor.getInt(1), cursor.getInt(2))
            } else null
        } catch (t: Throwable) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun isoTime(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(ms))

    override fun onDestroy() {
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            listener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (_: Throwable) {
        }
        pool.shutdownNow()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
}
