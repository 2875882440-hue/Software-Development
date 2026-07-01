package com.localbookkeeping.app.notification

data class ListenerHealthInput(
    val notificationPermissionEnabled: Boolean,
    val listenerConnected: Boolean,
    val rawListenerConnected: Boolean = listenerConnected,
    val lastDisconnectedAt: Long = 0L,
    val lastHeartbeatAt: Long,
    val autoListenEnabled: Boolean,
    val foregroundServiceRunning: Boolean,
    val testNotificationFailed: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis()
)

data class ListenerHealthEvaluation(
    val status: ListenerServiceStatus,
    val reasons: List<String>
)

enum class ListenerServiceStatus {
    HEALTHY,
    SUSPICIOUS,
    DISCONNECTED,
    PERMISSION_MISSING,
    SERVICE_UNKNOWN
}

object ListenerHealthEvaluator {
    const val HEARTBEAT_STALE_MILLIS = 90_000L

    fun evaluate(input: ListenerHealthInput): ListenerHealthEvaluation {
        if (!input.notificationPermissionEnabled) {
            return ListenerHealthEvaluation(
                status = ListenerServiceStatus.PERMISSION_MISSING,
                reasons = listOf("通知监听权限未授权")
            )
        }

        if (!input.listenerConnected && input.lastDisconnectedAt > 0L) {
            return ListenerHealthEvaluation(
                status = ListenerServiceStatus.DISCONNECTED,
                reasons = listOf("onListenerDisconnected 已触发")
            )
        }

        val reasons = mutableListOf<String>()
        if (input.lastHeartbeatAt <= 0L || input.nowMillis - input.lastHeartbeatAt > HEARTBEAT_STALE_MILLIS) {
            reasons += "最近心跳超时"
        }
        if (input.autoListenEnabled && !input.foregroundServiceRunning) {
            reasons += "自动监听已开启但前台服务未运行"
        }
        if (input.testNotificationFailed) {
            reasons += "测试通知无法捕获"
        }

        return when {
            input.listenerConnected && reasons.isEmpty() ->
                ListenerHealthEvaluation(ListenerServiceStatus.HEALTHY, listOf("监听服务健康"))
            reasons.isNotEmpty() ->
                ListenerHealthEvaluation(ListenerServiceStatus.SUSPICIOUS, reasons)
            input.rawListenerConnected ->
                ListenerHealthEvaluation(ListenerServiceStatus.SERVICE_UNKNOWN, listOf("状态无法判断"))
            else ->
                ListenerHealthEvaluation(ListenerServiceStatus.SERVICE_UNKNOWN, listOf("暂无监听服务状态记录"))
        }
    }
}
