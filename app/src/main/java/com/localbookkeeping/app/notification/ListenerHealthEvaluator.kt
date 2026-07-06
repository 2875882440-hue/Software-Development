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
    val isHuaweiHonorDevice: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis()
)

data class ListenerHealthEvaluation(
    val status: ListenerServiceStatus,
    val reasons: List<String>
)

enum class ListenerServiceStatus {
    HEALTHY,
    STALE,
    SUSPICIOUS,
    DISCONNECTED,
    PERMISSION_MISSING,
    PERMISSION_GRANTED_BUT_NOT_CONNECTED,
    FOREGROUND_RUNNING_BUT_LISTENER_DEAD,
    VENDOR_BLOCKED,
    PROBE_FAILED,
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

        if (input.notificationPermissionEnabled &&
            input.foregroundServiceRunning &&
            !input.listenerConnected &&
            input.testNotificationFailed &&
            input.isHuaweiHonorDevice
        ) {
            return ListenerHealthEvaluation(
                status = ListenerServiceStatus.VENDOR_BLOCKED,
                reasons = listOf(
                    "权限已授权，但监听服务未实际连接",
                    "疑似 HarmonyOS 后台策略拦截",
                    "请重新授权通知监听权限"
                )
            )
        }

        if (input.notificationPermissionEnabled &&
            input.foregroundServiceRunning &&
            !input.listenerConnected &&
            input.testNotificationFailed
        ) {
            return ListenerHealthEvaluation(
                status = ListenerServiceStatus.PERMISSION_GRANTED_BUT_NOT_CONNECTED,
                reasons = listOf("权限已授权，但监听服务未实际连接")
            )
        }

        if (input.notificationPermissionEnabled &&
            input.foregroundServiceRunning &&
            !input.listenerConnected &&
            input.lastDisconnectedAt > 0L
        ) {
            return ListenerHealthEvaluation(
                status = ListenerServiceStatus.FOREGROUND_RUNNING_BUT_LISTENER_DEAD,
                reasons = listOf("前台服务运行中，但监听服务未连接")
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
            reasons += "长时间没有监听心跳"
        }
        if (input.autoListenEnabled && !input.foregroundServiceRunning) {
            reasons += "自动监听已开启但前台服务未运行"
        }
        if (input.testNotificationFailed) {
            reasons += "探测通知失败"
        }

        return when {
            input.listenerConnected && reasons.isEmpty() ->
                ListenerHealthEvaluation(ListenerServiceStatus.HEALTHY, listOf("监听服务健康"))
            input.testNotificationFailed ->
                ListenerHealthEvaluation(ListenerServiceStatus.PROBE_FAILED, reasons)
            reasons.size == 1 && reasons.first().contains("监听心跳") ->
                ListenerHealthEvaluation(ListenerServiceStatus.STALE, reasons)
            reasons.isNotEmpty() ->
                ListenerHealthEvaluation(ListenerServiceStatus.SUSPICIOUS, reasons)
            input.rawListenerConnected ->
                ListenerHealthEvaluation(ListenerServiceStatus.SERVICE_UNKNOWN, listOf("状态无法判断"))
            else ->
                ListenerHealthEvaluation(ListenerServiceStatus.SERVICE_UNKNOWN, listOf("暂无监听服务状态记录"))
        }
    }
}
