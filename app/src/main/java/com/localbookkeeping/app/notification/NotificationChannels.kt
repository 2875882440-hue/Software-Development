package com.localbookkeeping.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val LISTENER_CHANNEL_ID = "bookkeeping_listener"
    const val TEST_CHANNEL_ID = "bookkeeping_test"
    const val LIMIT_CHANNEL_ID = "bookkeeping_daily_limit"

    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                LISTENER_CHANNEL_ID,
                "自动记账监听",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示自动记账监听运行状态"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                TEST_CHANNEL_ID,
                "记账测试通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于测试通知监听链路"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                LIMIT_CHANNEL_ID,
                "消费限额提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日消费超过限额时提醒"
            }
        )
    }
}
