package com.localbookkeeping.app.notification

import android.app.Notification
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localbookkeeping.app.MainActivity
import com.localbookkeeping.app.data.AppDatabase
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BookkeepingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeepAliveNotificationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var userRequestedStop = false
    private var watchdogJob: Job? = null

    private val repository by lazy {
        val database = AppDatabase.create(this)
        BookkeepingRepository(
            expenseDao = database.expenseDao(),
            debugNotificationLogDao = database.debugNotificationLogDao(),
            classificationRuleDao = database.classificationRuleDao(),
            backgroundStabilityLogDao = database.backgroundStabilityLogDao(),
            merchantCategoryLearningDao = database.merchantCategoryLearningDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureAll(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        setRunning(this, true)
        scope.launch {
            repository.addBackgroundStabilityLog(BackgroundEventType.FOREGROUND_SERVICE_START, "自动记账监听前台服务已启动")
            runHealthCheck("foregroundServiceStart")
        }
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            userRequestedStop = true
            setEnabled(this, false)
            setRunning(this, false)
            scope.launch {
                repository.addBackgroundStabilityLog(BackgroundEventType.AUTO_LISTEN_DISABLED, "用户关闭自动监听")
                repository.addBackgroundStabilityLog(BackgroundEventType.FOREGROUND_SERVICE_STOP, "自动记账监听前台服务已停止")
            }
            stopWatchdog()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        setEnabled(this, true)
        setRunning(this, true)
        scope.launch { runHealthCheck("foregroundServiceCommand") }
        return START_STICKY
    }

    override fun onDestroy() {
        stopWatchdog()
        setRunning(this, false)
        if (!userRequestedStop) {
            scope.launch {
                repository.addBackgroundStabilityLog(
                    BackgroundEventType.FOREGROUND_SERVICE_STOP,
                    "前台服务被系统停止"
                )
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MILLIS)
                runHealthCheck("watchdog")
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    private suspend fun runHealthCheck(source: String) {
        val evaluation = ListenerRecoveryManager.checkNow(
            context = this,
            repository = repository,
            source = source,
            probeWhenHealthy = true
        )
        updateNotification(evaluation.status)
    }

    private fun updateNotification(status: ListenerServiceStatus) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun buildNotification(status: ListenerServiceStatus = ListenerRecoveryState.snapshot(this).lastHealthStatus.toStatus()): Notification {
        val (title, text) = when (status) {
            ListenerServiceStatus.HEALTHY -> "自动记账监听中" to "用于提高微信/支付宝付款通知监听稳定性"
            ListenerServiceStatus.SUSPICIOUS -> "监听可能失效，点击修复" to "最近心跳异常，请打开 App 检查监听状态"
            ListenerServiceStatus.DISCONNECTED -> "监听已断开，请打开 App 修复" to "通知监听服务已断开或被系统回收"
            ListenerServiceStatus.PERMISSION_MISSING -> "通知监听权限未开启" to "请打开 App 前往通知监听权限页面"
            ListenerServiceStatus.SERVICE_UNKNOWN -> "自动记账监听中" to "正在检查通知监听服务状态"
        }
        return NotificationCompat.Builder(this, NotificationChannels.LISTENER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openBackgroundSettingsIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun openBackgroundSettingsIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_OPEN_BACKGROUND_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    companion object {
        private const val NOTIFICATION_ID = 21001
        private const val ACTION_STOP = "com.localbookkeeping.app.STOP_KEEP_ALIVE"
        const val ACTION_OPEN_BACKGROUND_SETTINGS = "com.localbookkeeping.app.OPEN_BACKGROUND_SETTINGS"
        private const val PREFS = "auto_listener"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_RUNNING = "running"
        private const val KEY_BOOT_RESTORE_PENDING = "boot_restore_pending"
        private const val WATCHDOG_INTERVAL_MILLIS = 10L * 60L * 1000L

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            setEnabled(context, true)
        }

        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveNotificationService::class.java).setAction(ACTION_STOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            setEnabled(context, false)
            setRunning(context, false)
        }

        fun isEnabled(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)

        fun isRunning(context: Context): Boolean {
            val appContext = context.applicationContext
            val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val running = manager
                ?.getRunningServices(Int.MAX_VALUE)
                ?.any {
                    it.service.packageName == appContext.packageName &&
                        it.service.className == KeepAliveNotificationService::class.java.name
                } == true
            if (!running && appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_RUNNING, false)) {
                setRunning(appContext, false)
            }
            return running
        }

        fun markBootRestorePending(context: Context, pending: Boolean) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BOOT_RESTORE_PENDING, pending)
                .apply()
        }

        fun hasBootRestorePending(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_BOOT_RESTORE_PENDING, false)

        private fun setEnabled(context: Context, enabled: Boolean) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
        }

        private fun setRunning(context: Context, running: Boolean) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_RUNNING, running)
                .apply()
        }

        private fun String.toStatus(): ListenerServiceStatus =
            runCatching { ListenerServiceStatus.valueOf(this) }.getOrDefault(ListenerServiceStatus.SERVICE_UNKNOWN)
    }
}
