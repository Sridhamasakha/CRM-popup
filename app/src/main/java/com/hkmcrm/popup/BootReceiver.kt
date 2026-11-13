package com.hkmcrm.popup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isEnabled(context)) return
        val svc = Intent(context, CallWatchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc) else context.startService(svc)
    }
}
