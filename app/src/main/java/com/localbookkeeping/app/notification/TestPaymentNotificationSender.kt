package com.localbookkeeping.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object TestPaymentNotificationSender {
    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun areAppNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun send(context: Context): Boolean {
        if (!canPostNotifications(context) || !areAppNotificationsEnabled(context)) return false
        NotificationChannels.ensureAll(context)
        val notification = NotificationCompat.Builder(context, NotificationChannels.TEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("测试支付通知")
            .setContentText("测试支付通知 ¥0.01")
            .setStyle(NotificationCompat.BigTextStyle().bigText("测试支付通知 ¥0.01"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        return true
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(TEST_NOTIFICATION_ID)
    }

    private const val TEST_NOTIFICATION_ID = 22001
}
