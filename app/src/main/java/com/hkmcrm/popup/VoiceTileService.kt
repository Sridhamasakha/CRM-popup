package com.hkmcrm.popup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.TileService

class VoiceTileService : TileService() {
    override fun onClick() {
        super.onClick()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.APP_URL))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this, 0, intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: Throwable) {
        }
    }
}
