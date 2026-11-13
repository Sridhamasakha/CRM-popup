package com.hkmcrm.popup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Notifier {
    private const val CHANNEL = "hkm_caller_popup"
    const val FOREGROUND_ID = 41

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "HKM caller popup", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "Shows CRM details when a known donor or lead calls"
                ch.enableVibration(true)
                mgr.createNotificationChannel(ch)
            }
        }
    }

    private fun base(context: Context): Notification.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, CHANNEL)
        else
            Notification.Builder(context)
        return builder.setSmallIcon(android.R.drawable.sym_action_call)
    }

    fun foreground(context: Context): Notification {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return base(context)
            .setContentTitle("HKM CRM caller watch")
            .setContentText("Watching calls for donors and leads")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    fun popup(context: Context, hit: ContactHit, incoming: Boolean) {
        ensureChannel(context)
        val lines = mutableListOf<String>()
        if (hit.kind.isNotBlank()) lines.add(hit.kind)
        if (hit.category.isNotBlank()) lines.add(hit.category)
        if (hit.lastInteraction.isNotBlank()) lines.add("Last: ${hit.lastInteraction}")
        if (hit.openOpportunity.isNotBlank()) lines.add("Open: ${hit.openOpportunity}")
        if (hit.assignedTo.isNotBlank()) lines.add("Owner: ${hit.assignedTo}")

        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val callBack = PendingIntent.getActivity(
            context, 2, Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + hit.mobile)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val n = base(context)
            .setContentTitle((if (incoming) "Incoming · " else "Calling · ") + (hit.name.ifBlank { hit.mobile }))
            .setContentText(lines.joinToString(" · "))
            .setStyle(Notification.BigTextStyle().bigText(lines.joinToString("\n")))
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_call, "Call", callBack)
            .build()

        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.notify(hit.mobile.hashCode(), n)
    }
}
