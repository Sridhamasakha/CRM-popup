package com.hkmcrm.popup

import android.content.Context

object Prefs {
    private const val FILE = "hkm_caller"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DEVICE_KEY = "device_key"
    private const val KEY_USER = "user_email"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_ENABLED, true)
    fun setEnabled(ctx: Context, value: Boolean) = sp(ctx).edit().putBoolean(KEY_ENABLED, value).apply()

    fun deviceKey(ctx: Context): String = sp(ctx).getString(KEY_DEVICE_KEY, "") ?: ""
    fun setDeviceKey(ctx: Context, value: String) = sp(ctx).edit().putString(KEY_DEVICE_KEY, value.trim()).apply()

    fun userEmail(ctx: Context): String = sp(ctx).getString(KEY_USER, "") ?: ""
    fun setUserEmail(ctx: Context, value: String) = sp(ctx).edit().putString(KEY_USER, value.trim()).apply()
}
