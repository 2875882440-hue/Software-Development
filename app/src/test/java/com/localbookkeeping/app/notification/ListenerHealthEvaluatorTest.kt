package com.localbookkeeping.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerHealthEvaluatorTest {
    @Test
    fun healthyWhenPermissionConnectedAndHeartbeatFresh() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = true,
                lastHeartbeatAt = 10_000L,
                autoListenEnabled = true,
                foregroundServiceRunning = true,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.HEALTHY, result.status)
    }

    @Test
    fun staleWhenHeartbeatIsStale() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = true,
                lastHeartbeatAt = 1_000L,
                autoListenEnabled = false,
                foregroundServiceRunning = false,
                nowMillis = 200_000L
            )
        )

        assertEquals(ListenerServiceStatus.STALE, result.status)
        assertTrue(result.reasons.any { it.contains("心跳") })
    }

    @Test
    fun probeFailedWhenProbeFailedEvenWithFreshHeartbeat() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = true,
                lastHeartbeatAt = 10_000L,
                autoListenEnabled = true,
                foregroundServiceRunning = true,
                testNotificationFailed = true,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.PROBE_FAILED, result.status)
        assertTrue(result.reasons.any { it.contains("探测通知") })
    }

    @Test
    fun permissionGrantedButNotConnectedWhenForegroundRunsAndProbeFails() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = false,
                rawListenerConnected = false,
                lastDisconnectedAt = 0L,
                lastHeartbeatAt = 10_000L,
                autoListenEnabled = true,
                foregroundServiceRunning = true,
                testNotificationFailed = true,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.PERMISSION_GRANTED_BUT_NOT_CONNECTED, result.status)
    }

    @Test
    fun vendorBlockedWhenHuaweiHonorForegroundRunsAndProbeFails() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = false,
                rawListenerConnected = false,
                lastDisconnectedAt = 12_000L,
                lastHeartbeatAt = 10_000L,
                autoListenEnabled = true,
                foregroundServiceRunning = true,
                testNotificationFailed = true,
                isHuaweiHonorDevice = true,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.VENDOR_BLOCKED, result.status)
        assertTrue(result.reasons.any { it.contains("未实际连接") })
    }

    @Test
    fun disconnectedWhenDisconnectedCallbackWasRecorded() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = false,
                rawListenerConnected = false,
                lastDisconnectedAt = 12_000L,
                lastHeartbeatAt = 10_000L,
                autoListenEnabled = true,
                foregroundServiceRunning = false,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.DISCONNECTED, result.status)
    }

    @Test
    fun permissionMissingWhenNotificationListenerPermissionIsOff() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = false,
                listenerConnected = false,
                lastHeartbeatAt = 0L,
                autoListenEnabled = false,
                foregroundServiceRunning = false,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.PERMISSION_MISSING, result.status)
    }

    @Test
    fun staleWhenThereIsNoStateYet() {
        val result = ListenerHealthEvaluator.evaluate(
            ListenerHealthInput(
                notificationPermissionEnabled = true,
                listenerConnected = false,
                rawListenerConnected = false,
                lastDisconnectedAt = 0L,
                lastHeartbeatAt = 0L,
                autoListenEnabled = false,
                foregroundServiceRunning = false,
                nowMillis = 20_000L
            )
        )

        assertEquals(ListenerServiceStatus.STALE, result.status)
    }
}
