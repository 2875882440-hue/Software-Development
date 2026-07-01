package com.localbookkeeping.app

import android.app.Application
import com.localbookkeeping.app.data.AppDatabase
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BookkeepingRepository
import com.localbookkeeping.app.notification.ListenerRecoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BookkeepingApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppVisibilityTracker)
        appScope.launch {
            repository.addBackgroundStabilityLog(BackgroundEventType.APP_START, "App启动")
            ListenerRecoveryManager.checkNow(this@BookkeepingApplication, repository, "appStart")
        }
    }

    val database by lazy { AppDatabase.create(this) }
    val repository by lazy {
        BookkeepingRepository(
            expenseDao = database.expenseDao(),
            debugNotificationLogDao = database.debugNotificationLogDao(),
            classificationRuleDao = database.classificationRuleDao(),
            backgroundStabilityLogDao = database.backgroundStabilityLogDao(),
            merchantCategoryLearningDao = database.merchantCategoryLearningDao()
        )
    }
}
