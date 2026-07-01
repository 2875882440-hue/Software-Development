package com.localbookkeeping.app.limit

import android.content.Context
import java.time.Instant
import java.time.ZoneId

object DailyLimitManager {
    private const val PREFS = "daily_limit"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LIMIT_CENTS = "limit_cents"
    private const val KEY_MUTED_DATE = "muted_date"

    fun load(context: Context): DailyLimitConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DailyLimitConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            limitCents = prefs.getLong(KEY_LIMIT_CENTS, 0L),
            mutedDate = prefs.getString(KEY_MUTED_DATE, "").orEmpty()
        )
    }

    fun save(context: Context, enabled: Boolean, limitCents: Long) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putLong(KEY_LIMIT_CENTS, limitCents.coerceAtLeast(0L))
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun muteToday(context: Context, nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()) {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toString()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MUTED_DATE, today)
            .apply()
    }
}
