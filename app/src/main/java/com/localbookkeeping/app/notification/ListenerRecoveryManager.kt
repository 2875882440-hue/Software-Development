package com.localbookkeeping.app.notification

import android.content.Context
import com.localbookkeeping.app.data.BackgroundEventType
import com.localbookkeeping.app.data.BookkeepingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RebindAttemptStatus {
    ATTEMPTED,
    COOLDOWN,
    PERMISSION_MISSING,
    UNSUPPORTED
}

data class RebindAttemptOutcome(
    val status: RebindAttemptStatus,
    val called: Boolean,
    val connectedAfter: Boolean,
    val result: String
)

object ListenerRecoveryManager {
    suspend fun checkNow(
        context: Context,
        repository: BookkeepingRepository,
        source: String,
        forceRebind: Boolean = false,
        probeWhenHealthy: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): ListenerHealthEvaluation {
        val appContext = context.applicationContext
        val runtimeState = NotificationListenerState.current(appContext)
        val foregroundRunning = KeepAliveNotificationService.isRunning(appContext)
        var evaluation = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = NotificationListenerState.isPermissionEnabled(appContext),
                listenerConnected = runtimeState.listenerConnected,
                rawListenerConnected = runtimeState.rawListenerConnected,
                lastDisconnectedAt = runtimeState.lastDisconnectedTime,
                lastHeartbeatAt = runtimeState.lastHeartbeatTime,
                autoListenEnabled = KeepAliveNotificationService.isEnabled(appContext),
                foregroundServiceRunning = foregroundRunning,
                nowMillis = nowMillis
            )
        )
        val shouldProbe = NotificationListenerState.isPermissionEnabled(appContext) &&
            foregroundRunning &&
            (probeWhenHealthy || evaluation.status == ListenerServiceStatus.SUSPICIOUS)
        var probeFailed = false
        var probeSucceeded = false
        if (shouldProbe) {
            val probeResult = runProbeCheck(appContext, repository, source)
            if (probeResult == true) {
                probeSucceeded = true
                val refreshed = NotificationListenerState.current(appContext)
                evaluation = ListenerHealthEvaluator.evaluate(
                    ListenerHealthInput(
                        notificationPermissionEnabled = true,
                        listenerConnected = refreshed.listenerConnected,
                        rawListenerConnected = refreshed.rawListenerConnected,
                        lastDisconnectedAt = refreshed.lastDisconnectedTime,
                        lastHeartbeatAt = refreshed.lastHeartbeatTime,
                        autoListenEnabled = KeepAliveNotificationService.isEnabled(appContext),
                        foregroundServiceRunning = foregroundRunning
                    )
                )
            } else if (probeResult == false) {
                probeFailed = true
                evaluation = ListenerHealthEvaluator.evaluate(
                    ListenerHealthInput(
                        notificationPermissionEnabled = true,
                        listenerConnected = runtimeState.listenerConnected,
                        rawListenerConnected = runtimeState.rawListenerConnected,
                        lastDisconnectedAt = runtimeState.lastDisconnectedTime,
                        lastHeartbeatAt = runtimeState.lastHeartbeatTime,
                        autoListenEnabled = KeepAliveNotificationService.isEnabled(appContext),
                        foregroundServiceRunning = foregroundRunning,
                        testNotificationFailed = true
                    )
                )
            }
        }
        if (probeSucceeded && evaluation.status == ListenerServiceStatus.HEALTHY) {
            ListenerRecoveryState.clearListenerRecheckNeeded(appContext)
        }
        ListenerRecoveryState.markHealthCheck(appContext, evaluation, nowMillis)
        repository.addBackgroundStabilityLog(
            eventType = BackgroundEventType.HEALTH_CHECK,
            message = "${evaluation.status.name}:$source",
            detail = evaluation.reasons.joinToString("；"),
            createdAtMillis = nowMillis
        )
        repository.addBackgroundStabilityLog(
            eventType = eventTypeFor(evaluation.status),
            message = source,
            detail = evaluation.reasons.joinToString("；"),
            createdAtMillis = nowMillis
        )

        val heartbeatStale = runtimeState.lastHeartbeatTime <= 0L ||
            nowMillis - runtimeState.lastHeartbeatTime > ListenerHealthEvaluator.HEARTBEAT_STALE_MILLIS
        val shouldAttemptRecovery = evaluation.status == ListenerServiceStatus.DISCONNECTED ||
            (evaluation.status == ListenerServiceStatus.SUSPICIOUS && (probeFailed || heartbeatStale))
        if (shouldAttemptRecovery) {
            maybeRequestRebind(appContext, repository, evaluation, source, forceRebind, nowMillis, probeFailed)
        }
        return evaluation
    }

    suspend fun runProbeCheck(
        context: Context,
        repository: BookkeepingRepository,
        source: String,
        timeoutMillis: Long = PROBE_WAIT_MILLIS
    ): Boolean? = probeMutex.withLock {
        val sentAt = System.currentTimeMillis()
        val token = ListenerProbeNotification.send(context, sentAt) ?: return@withLock null
        repository.addBackgroundStabilityLog(
            BackgroundEventType.PROBE_NOTIFICATION_SENT,
            "probeNotificationSent",
            "source=$source,token=$token",
            sentAt
        )
        var waited = 0L
        while (waited < timeoutMillis) {
            delay(PROBE_POLL_MILLIS)
            waited += PROBE_POLL_MILLIS
            if (ListenerProbeNotification.wasCaptured(context, token)) return@withLock true
        }
        val failedAt = System.currentTimeMillis()
        ListenerProbeNotification.markFailed(context, token, failedAt)
        ListenerRecoveryState.markHealthCheck(
            context,
            ListenerHealthEvaluation(ListenerServiceStatus.SUSPICIOUS, listOf("监听权限存在，但探测通知未被捕获")),
            failedAt
        )
        repository.addBackgroundStabilityLog(
            BackgroundEventType.PROBE_FAIL,
            "probeFail",
            "source=$source,token=$token",
            failedAt
        )
        if (NotificationListenerState.isPermissionEnabled(context)) {
            repository.addBackgroundStabilityLog(
                BackgroundEventType.LISTENER_PERMISSION_GRANTED_BUT_PROBE_FAIL,
                "listenerPermissionGrantedButProbeFail",
                "source=$source",
                failedAt
            )
        }
        false
    }

    suspend fun requestRebindNow(
        context: Context,
        repository: BookkeepingRepository,
        source: String,
        forceRebind: Boolean = false,
        afterProbeFail: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): RebindAttemptOutcome {
        val evaluation = ListenerHealthEvaluation(
            ListenerServiceStatus.SUSPICIOUS,
            listOf(if (afterProbeFail) "探测通知未被捕获" else "手动恢复")
        )
        return maybeRequestRebind(
            context.applicationContext,
            repository,
            evaluation,
            source,
            forceRebind,
            nowMillis,
            afterProbeFail
        )
    }

    private suspend fun maybeRequestRebind(
        context: Context,
        repository: BookkeepingRepository,
        evaluation: ListenerHealthEvaluation,
        source: String,
        forceRebind: Boolean,
        nowMillis: Long,
        afterProbeFail: Boolean
    ): RebindAttemptOutcome {
        if (!NotificationListenerState.isPermissionEnabled(context)) {
            return RebindAttemptOutcome(RebindAttemptStatus.PERMISSION_MISSING, false, false, "permissionMissing")
        }

        val lastAttemptAt = ListenerRecoveryState.snapshot(context).lastRebindAttemptAt
        if (!forceRebind && !ListenerRecoveryPolicy.canAttemptRebind(nowMillis, lastAttemptAt)) {
            repository.addBackgroundStabilityLog(
                BackgroundEventType.REQUEST_REBIND_RESULT,
                "cooldownSkipped",
                "source=$source,status=${evaluation.status.name}",
                nowMillis
            )
            return RebindAttemptOutcome(RebindAttemptStatus.COOLDOWN, false, false, "cooldownSkipped")
        }

        ListenerRecoveryState.markRebindAttempt(context, nowMillis)
        repository.addBackgroundStabilityLog(
            BackgroundEventType.REQUEST_REBIND_ATTEMPT,
            "requestRebindAttempt",
            "source=$source,status=${evaluation.status.name},reason=${evaluation.reasons.joinToString("；")}",
            nowMillis
        )
        repository.addBackgroundStabilityLog(
            BackgroundEventType.REQUEST_REBIND,
            "requestRebindAttempt",
            "source=$source,status=${evaluation.status.name}",
            nowMillis
        )
        if (afterProbeFail) {
            repository.addBackgroundStabilityLog(
                BackgroundEventType.REQUEST_REBIND_AFTER_PROBE_FAIL,
                "requestRebindAfterProbeFail",
                "source=$source",
                nowMillis
            )
        }
        val called = NotificationListenerState.requestRebind(context)
        delay(REBIND_RESULT_WAIT_MILLIS)
        val connectedAfter = NotificationListenerState.isConnected(context)
        val success = called && connectedAfter
        val result = when {
            success -> "connectedAfterRebind"
            called -> "requestCalledButNotConnected"
            else -> "requestRebindUnsupported"
        }
        ListenerRecoveryState.markRebindResult(context, result, System.currentTimeMillis(), success)
        repository.addBackgroundStabilityLog(
            BackgroundEventType.REQUEST_REBIND_RESULT,
            result,
            "source=$source,called=$called,connectedAfter=$connectedAfter",
            System.currentTimeMillis()
        )
        return RebindAttemptOutcome(
            status = if (called) RebindAttemptStatus.ATTEMPTED else RebindAttemptStatus.UNSUPPORTED,
            called = called,
            connectedAfter = connectedAfter,
            result = result
        )
    }

    private fun eventTypeFor(status: ListenerServiceStatus): String = when (status) {
        ListenerServiceStatus.HEALTHY -> BackgroundEventType.HEALTH_HEALTHY
        ListenerServiceStatus.SUSPICIOUS -> BackgroundEventType.HEALTH_SUSPICIOUS
        ListenerServiceStatus.DISCONNECTED -> BackgroundEventType.HEALTH_DISCONNECTED
        ListenerServiceStatus.PERMISSION_MISSING -> BackgroundEventType.HEALTH_PERMISSION_MISSING
        ListenerServiceStatus.SERVICE_UNKNOWN -> BackgroundEventType.HEALTH_SERVICE_UNKNOWN
    }

    private const val REBIND_RESULT_WAIT_MILLIS = 5_000L
    private const val PROBE_WAIT_MILLIS = 4_000L
    private const val PROBE_POLL_MILLIS = 200L
    private val probeMutex = Mutex()
}
