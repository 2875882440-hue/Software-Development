package com.localbookkeeping.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

data class ListenerProbeSnapshot(
    val lastSentTime: Long,
    val lastSuccessTime: Long,
    val lastFailTime: Long
) {
    val latestProbeFailed: Boolean
        get() = lastFailTime > lastSuccessTime
}

object ListenerProbeNotification {
    const val TITLE = "监听状态检测"
    const val EXTRA_PROBE_TOKEN = "listener_probe_token"
    private const val CHANNEL_ID = "listener_probe"
    private const val NOTIFICATION_ID = 21002
    private const val PREFS = "listener_probe_state"
    private const val KEY_LAST_SENT_AT = "last_sent_at"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_LAST_FAIL_AT = "last_fail_at"
    private const val KEY_CAPTURED_TOKEN = "captured_token"
    private const val AUTO_CANCEL_DELAY_MILLIS = 8_000L

    fun send(context: Context, nowMillis: Long = System.currentTimeMillis()): Long? {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return null
        if (!manager.areNotificationsEnabled()) return null
        ensureChannel(manager)
        val token = nowMillis
        val extras = Bundle().apply { putLong(EXTRA_PROBE_TOKEN, token) }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(TITLE)
            .setContentText(TITLE)
            .setExtras(extras)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        return runCatching {
            prefs(appContext).edit()
                .putLong(KEY_LAST_SENT_AT, nowMillis)
                .remove(KEY_CAPTURED_TOKEN)
                .apply()
            manager.notify(NOTIFICATION_ID, notification)
            Handler(Looper.getMainLooper()).postDelayed(
                { manager.cancel(NOTIFICATION_ID) },
                AUTO_CANCEL_DELAY_MILLIS
            )
            token
        }.getOrNull()
    }

    fun probeToken(notificationId: Int, extras: Bundle?): Long? {
        if (notificationId != NOTIFICATION_ID) return null
        val token = extras?.getLong(EXTRA_PROBE_TOKEN, 0L) ?: 0L
        return token.takeIf { it > 0L }
    }

    fun markCaptured(context: Context, token: Long, nowMillis: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_CAPTURED_TOKEN, token)
            .putLong(KEY_LAST_SUCCESS_AT, nowMillis)
            .apply()
    }

    fun wasCaptured(context: Context, token: Long): Boolean =
        prefs(context).getLong(KEY_CAPTURED_TOKEN, 0L) == token

    fun markFailed(context: Context, token: Long, nowMillis: Long = System.currentTimeMillis()) {
        if (wasCaptured(context, token)) return
        prefs(context).edit()
            .putLong(KEY_LAST_FAIL_AT, nowMillis)
            .apply()
    }

    fun snapshot(context: Context): ListenerProbeSnapshot {
        val prefs = prefs(context)
        return ListenerProbeSnapshot(
            lastSentTime = prefs.getLong(KEY_LAST_SENT_AT, 0L),
            lastSuccessTime = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L),
            lastFailTime = prefs.getLong(KEY_LAST_FAIL_AT, 0L)
        )
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "监听状态检测", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "仅用于检查通知监听服务是否正常工作"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
