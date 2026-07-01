package com.localbookkeeping.app.data

data class BackgroundDiagnosticsReport(
    val appStartCount: Int,
    val listenerConnectedCount: Int,
    val listenerDisconnectedCount: Int,
    val requestRebindCount: Int,
    val foregroundServiceStartCount: Int,
    val foregroundServiceStopCount: Int,
    val healthCheckCount: Int,
    val healthHealthyCount: Int,
    val healthSuspiciousCount: Int,
    val healthDisconnectedCount: Int,
    val requestRebindAttemptCount: Int,
    val requestRebindConnectedCount: Int,
    val requestRebindFailedCount: Int,
    val packageReplacedCount: Int,
    val probeNotificationSentCount: Int,
    val probeSuccessCount: Int,
    val probeFailCount: Int,
    val autoListenRestoreCount: Int,
    val foregroundServiceMissingCount: Int,
    val listenerPermissionGrantedButProbeFailCount: Int,
    val requestRebindAfterProbeFailCount: Int,
    val paymentCapturedCount: Int,
    val paymentParseSuccessCount: Int,
    val paymentParseFailCount: Int,
    val failureReasons: Map<String, Int>
)

object BackgroundDiagnosticsCalculator {
    fun calculate(logs: List<BackgroundStabilityLog>, sinceMillis: Long): BackgroundDiagnosticsReport {
        val recent = logs.filter { it.createdAtMillis >= sinceMillis }
        val failures = recent
            .filter { it.eventType == BackgroundEventType.PAYMENT_PARSE_FAIL }
            .groupingBy { it.message.ifBlank { "未知原因" } }
            .eachCount()
        return BackgroundDiagnosticsReport(
            appStartCount = recent.count { it.eventType == BackgroundEventType.APP_START },
            listenerConnectedCount = recent.count { it.eventType == BackgroundEventType.LISTENER_CONNECTED },
            listenerDisconnectedCount = recent.count { it.eventType == BackgroundEventType.LISTENER_DISCONNECTED },
            requestRebindCount = recent.count { it.eventType == BackgroundEventType.REQUEST_REBIND },
            foregroundServiceStartCount = recent.count { it.eventType == BackgroundEventType.FOREGROUND_SERVICE_START },
            foregroundServiceStopCount = recent.count { it.eventType == BackgroundEventType.FOREGROUND_SERVICE_STOP },
            healthCheckCount = recent.count { it.eventType == BackgroundEventType.HEALTH_CHECK },
            healthHealthyCount = recent.count { it.eventType == BackgroundEventType.HEALTH_HEALTHY },
            healthSuspiciousCount = recent.count { it.eventType == BackgroundEventType.HEALTH_SUSPICIOUS },
            healthDisconnectedCount = recent.count { it.eventType == BackgroundEventType.HEALTH_DISCONNECTED },
            requestRebindAttemptCount = recent.count { it.eventType == BackgroundEventType.REQUEST_REBIND_ATTEMPT },
            requestRebindConnectedCount = recent.count {
                it.eventType == BackgroundEventType.REQUEST_REBIND_RESULT && it.message == "connectedAfterRebind"
            },
            requestRebindFailedCount = recent.count {
                it.eventType == BackgroundEventType.REQUEST_REBIND_RESULT &&
                    it.message != "connectedAfterRebind" &&
                    it.message != "cooldownSkipped"
            },
            packageReplacedCount = recent.count { it.eventType == BackgroundEventType.PACKAGE_REPLACED },
            probeNotificationSentCount = recent.count { it.eventType == BackgroundEventType.PROBE_NOTIFICATION_SENT },
            probeSuccessCount = recent.count { it.eventType == BackgroundEventType.PROBE_SUCCESS },
            probeFailCount = recent.count { it.eventType == BackgroundEventType.PROBE_FAIL },
            autoListenRestoreCount = recent.count { it.eventType == BackgroundEventType.AUTO_LISTEN_RESTORE },
            foregroundServiceMissingCount = recent.count { it.eventType == BackgroundEventType.FOREGROUND_SERVICE_MISSING },
            listenerPermissionGrantedButProbeFailCount = recent.count {
                it.eventType == BackgroundEventType.LISTENER_PERMISSION_GRANTED_BUT_PROBE_FAIL
            },
            requestRebindAfterProbeFailCount = recent.count {
                it.eventType == BackgroundEventType.REQUEST_REBIND_AFTER_PROBE_FAIL
            },
            paymentCapturedCount = recent.count { it.eventType == BackgroundEventType.PAYMENT_NOTIFICATION_CAPTURED },
            paymentParseSuccessCount = recent.count { it.eventType == BackgroundEventType.PAYMENT_PARSE_SUCCESS },
            paymentParseFailCount = recent.count { it.eventType == BackgroundEventType.PAYMENT_PARSE_FAIL },
            failureReasons = failures
        )
    }
}
