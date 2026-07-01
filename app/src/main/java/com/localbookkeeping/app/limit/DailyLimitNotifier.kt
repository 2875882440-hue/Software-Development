package com.localbookkeeping.app.limit

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.localbookkeeping.app.formatMoney
import com.localbookkeeping.app.notification.NotificationChannels

object DailyLimitNotifier {
    private const val NOTIFICATION_ID = 23001

    fun notifyExceeded(context: Context, limitCents: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.LIMIT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今日已达限额")
            .setContentText("今日支出已超过 ${formatMoney(limitCents)}，请注意控制消费。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }
}
