package com.hkmcrm.popup

import android.app.Application

class HkmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val text = error.stackTraceToString().take(1500)
                getSharedPreferences("hkm_caller", MODE_PRIVATE)
                    .edit().putString("last_crash", text).apply()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
