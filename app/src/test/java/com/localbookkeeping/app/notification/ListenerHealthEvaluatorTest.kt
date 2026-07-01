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
    fun suspiciousWhenHeartbeatIsStale() {
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

        assertEquals(ListenerServiceStatus.SUSPICIOUS, result.status)
        assertTrue(result.reasons.any { it.contains("心跳") })
    }

    @Test
    fun suspiciousWhenProbeFailedEvenWithFreshHeartbeat() {
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

        assertEquals(ListenerServiceStatus.SUSPICIOUS, result.status)
        assertTrue(result.reasons.any { it.contains("测试通知") })
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
                foregroundServiceRunning = true,
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
    fun serviceUnknownWhenThereIsNoStateYet() {
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

        assertEquals(ListenerServiceStatus.SUSPICIOUS, result.status)
    }
}
