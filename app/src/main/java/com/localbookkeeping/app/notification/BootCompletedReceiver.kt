package com.localbookkeeping.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localbookkeeping.app.data.AppDatabase
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BookkeepingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = AppDatabase.create(context)
            val repository = BookkeepingRepository(
                expenseDao = database.expenseDao(),
                debugNotificationLogDao = database.debugNotificationLogDao(),
                classificationRuleDao = database.classificationRuleDao(),
                backgroundStabilityLogDao = database.backgroundStabilityLogDao(),
                merchantCategoryLearningDao = database.merchantCategoryLearningDao()
            )
            repository.addBackgroundStabilityLog(BackgroundEventType.BOOT_COMPLETED, "系统开机完成")
            if (KeepAliveNotificationService.isEnabled(context)) {
                KeepAliveNotificationService.markBootRestorePending(context, true)
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.BOOT_COMPLETED,
                    "用户之前开启过自动监听，等待打开 App 后确认恢复"
                )
            }
            pendingResult.finish()
        }
    }
}
