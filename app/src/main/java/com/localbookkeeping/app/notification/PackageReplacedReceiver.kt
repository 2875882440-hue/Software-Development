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

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ListenerRecoveryPolicy.shouldMarkPackageRecheck(intent.action)) return
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
            val now = System.currentTimeMillis()
            ListenerRecoveryState.markPackageReplaced(context, now)
            repository.addBackgroundStabilityLog(
                BackgroundEventType.PACKAGE_REPLACED,
                "App 已更新，等待用户检查通知监听权限",
                createdAtMillis = now
            )
            pendingResult.finish()
        }
    }
}
