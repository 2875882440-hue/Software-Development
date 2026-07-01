package com.localbookkeeping.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundDiagnosticsCalculatorTest {
    @Test
    fun countsRequestRebindAndFailureReasons() {
        val logs = listOf(
            log(BackgroundEventType.APP_START, "", 1_000),
            log(BackgroundEventType.REQUEST_REBIND, "手动重绑", 2_000),
            log(BackgroundEventType.REQUEST_REBIND_RESULT, "重绑失败", 3_000),
            log(BackgroundEventType.LISTENER_CONNECTED, "", 3_500),
            log(BackgroundEventType.LISTENER_DISCONNECTED, "", 4_000),
            log(BackgroundEventType.FOREGROUND_SERVICE_START, "", 5_000),
            log(BackgroundEventType.FOREGROUND_SERVICE_STOP, "", 5_500),
            log(BackgroundEventType.HEALTH_CHECK, "HEALTHY:watchdog", 5_600),
            log(BackgroundEventType.HEALTH_HEALTHY, "", 5_700),
            log(BackgroundEventType.HEALTH_SUSPICIOUS, "", 5_800),
            log(BackgroundEventType.HEALTH_DISCONNECTED, "", 5_900),
            log(BackgroundEventType.REQUEST_REBIND_ATTEMPT, "", 5_950),
            log(BackgroundEventType.REQUEST_REBIND_RESULT, "connectedAfterRebind", 5_960),
            log(BackgroundEventType.REQUEST_REBIND_RESULT, "requestCalledButNotConnected", 5_970),
            log(BackgroundEventType.PACKAGE_REPLACED, "", 5_980),
            log(BackgroundEventType.PROBE_NOTIFICATION_SENT, "", 5_981),
            log(BackgroundEventType.PROBE_SUCCESS, "", 5_982),
            log(BackgroundEventType.PROBE_FAIL, "", 5_983),
            log(BackgroundEventType.AUTO_LISTEN_RESTORE, "", 5_984),
            log(BackgroundEventType.FOREGROUND_SERVICE_MISSING, "", 5_985),
            log(BackgroundEventType.LISTENER_PERMISSION_GRANTED_BUT_PROBE_FAIL, "", 5_986),
            log(BackgroundEventType.REQUEST_REBIND_AFTER_PROBE_FAIL, "", 5_987),
            log(BackgroundEventType.PAYMENT_NOTIFICATION_CAPTURED, "", 6_000),
            log(BackgroundEventType.PAYMENT_PARSE_SUCCESS, "", 7_000),
            log(BackgroundEventType.PAYMENT_PARSE_FAIL, "未识别金额", 8_000),
            log(BackgroundEventType.PAYMENT_PARSE_FAIL, "未识别金额", 9_000),
            log(BackgroundEventType.REQUEST_REBIND, "旧数据", 100)
        )

        val report = BackgroundDiagnosticsCalculator.calculate(logs, sinceMillis = 1_000)

        assertEquals(1, report.appStartCount)
        assertEquals(1, report.listenerConnectedCount)
        assertEquals(1, report.listenerDisconnectedCount)
        assertEquals(1, report.requestRebindCount)
        assertEquals(1, report.foregroundServiceStartCount)
        assertEquals(1, report.foregroundServiceStopCount)
        assertEquals(1, report.healthCheckCount)
        assertEquals(1, report.healthHealthyCount)
        assertEquals(1, report.healthSuspiciousCount)
        assertEquals(1, report.healthDisconnectedCount)
        assertEquals(1, report.requestRebindAttemptCount)
        assertEquals(1, report.requestRebindConnectedCount)
        assertEquals(2, report.requestRebindFailedCount)
        assertEquals(1, report.packageReplacedCount)
        assertEquals(1, report.probeNotificationSentCount)
        assertEquals(1, report.probeSuccessCount)
        assertEquals(1, report.probeFailCount)
        assertEquals(1, report.autoListenRestoreCount)
        assertEquals(1, report.foregroundServiceMissingCount)
        assertEquals(1, report.listenerPermissionGrantedButProbeFailCount)
        assertEquals(1, report.requestRebindAfterProbeFailCount)
        assertEquals(1, report.paymentCapturedCount)
        assertEquals(1, report.paymentParseSuccessCount)
        assertEquals(2, report.paymentParseFailCount)
        assertEquals(2, report.failureReasons["未识别金额"])
    }

    private fun log(eventType: String, message: String, time: Long): BackgroundStabilityLog =
        BackgroundStabilityLog(
            eventType = eventType,
            message = message,
            detail = "",
            createdAtMillis = time
        )
}
