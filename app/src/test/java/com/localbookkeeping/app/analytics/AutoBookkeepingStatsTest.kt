package com.localbookkeeping.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AutoBookkeepingStatsTest {
    private val today = LocalDate.of(2026, 7, 16)

    @Test
    fun aggregatesLast14DaysBySource() {
        val storage = InMemoryCounterStorage()
        val store = AutoBookkeepingStatsStore(storage)

        repeat(3) {
            store.record(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, AutoBookkeepingSource.WECHAT, today)
        }
        repeat(2) {
            store.record(AutoBookkeepingEvent.PAYMENT_RELATED, AutoBookkeepingSource.WECHAT, today)
            store.record(AutoBookkeepingEvent.PENDING_CREATED, AutoBookkeepingSource.WECHAT, today)
        }
        store.record(AutoBookkeepingEvent.USER_CONFIRMED, AutoBookkeepingSource.WECHAT, today)
        store.record(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, AutoBookkeepingSource.ALIPAY, today.minusDays(13))
        store.record(AutoBookkeepingEvent.PAYMENT_RELATED, AutoBookkeepingSource.ALIPAY, today.minusDays(13))
        store.record(AutoBookkeepingEvent.AMOUNT_PARSE_FAILED, AutoBookkeepingSource.ALIPAY, today.minusDays(13))
        store.record(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, AutoBookkeepingSource.OTHER, today.minusDays(14))

        val snapshot = store.snapshot(windowDays = 14, today = today)
        val wechat = snapshot.bySource.single { it.source == AutoBookkeepingSource.WECHAT }.counters
        val alipay = snapshot.bySource.single { it.source == AutoBookkeepingSource.ALIPAY }.counters

        assertEquals(today.minusDays(13), snapshot.fromDate)
        assertEquals(4, snapshot.total.notificationReceived)
        assertEquals(3, snapshot.total.paymentRelated)
        assertEquals(2, snapshot.total.pendingCreated)
        assertEquals(67, snapshot.total.generationSuccessRatePercent)
        assertEquals(50, snapshot.total.notificationConversionRatePercent)
        assertEquals(1, snapshot.total.amountParseFailed)
        assertEquals(1, snapshot.total.userConfirmed)
        assertEquals(100, wechat.generationSuccessRatePercent)
        assertEquals(0, alipay.pendingCreated)
    }

    @Test
    fun prunesEntriesOlderThan30Days() {
        val storage = InMemoryCounterStorage()
        val store = AutoBookkeepingStatsStore(storage)

        store.record(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, AutoBookkeepingSource.WECHAT, today.minusDays(30))
        store.record(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, AutoBookkeepingSource.WECHAT, today.minusDays(29))
        store.record(AutoBookkeepingEvent.PENDING_CREATED, AutoBookkeepingSource.WECHAT, today)

        assertFalse(storage.values.containsKey(AutoBookkeepingStatsStore.keyFor(
            today.minusDays(30),
            AutoBookkeepingSource.WECHAT,
            AutoBookkeepingEvent.NOTIFICATION_RECEIVED
        )))
        assertTrue(storage.values.containsKey(AutoBookkeepingStatsStore.keyFor(
            today.minusDays(29),
            AutoBookkeepingSource.WECHAT,
            AutoBookkeepingEvent.NOTIFICATION_RECEIVED
        )))
    }

    @Test
    fun returnsNullRateWhenDenominatorIsZero() {
        val counters = AutoBookkeepingCounters(pendingCreated = 1)

        assertNull(counters.generationSuccessRatePercent)
        assertNull(counters.notificationConversionRatePercent)
    }

    @Test
    fun persistedKeysContainOnlyDateSourceAndEvent() {
        val key = AutoBookkeepingStatsStore.keyFor(
            date = today,
            source = AutoBookkeepingSource.ALIPAY,
            event = AutoBookkeepingEvent.USER_AMOUNT_EDITED
        )

        assertEquals("v1|2026-07-16|ALIPAY|USER_AMOUNT_EDITED", key)
        assertFalse(key.contains("微信支付"))
        assertFalse(key.contains("88.80"))
        assertFalse(key.contains("商户"))
        assertEquals(today, AutoBookkeepingStatsStore.parseKey(key)?.date)
    }

    @Test
    fun userActionsOnlyTrackAutomaticallyCreatedNotificationRecords() {
        assertTrue(isAutoNotificationRecordEligible(
            sourceType = "notification",
            packageName = "com.tencent.mm",
            notificationFingerprint = "real-fingerprint"
        ))
        assertFalse(isAutoNotificationRecordEligible(
            sourceType = "screenshot",
            packageName = "wechat_screenshot",
            notificationFingerprint = ""
        ))
        assertFalse(isAutoNotificationRecordEligible(
            sourceType = "notification",
            packageName = "com.tencent.mm",
            notificationFingerprint = "manual-debug-log-12-1000"
        ))
        assertFalse(isAutoNotificationRecordEligible(
            sourceType = "notification",
            packageName = "",
            notificationFingerprint = ""
        ))
    }

    private class InMemoryCounterStorage : AutoBookkeepingCounterStorage {
        val values = linkedMapOf<String, Int>()

        override fun readAll(): Map<String, Int> = values.toMap()

        override fun increment(key: String) {
            values[key] = values.getOrDefault(key, 0) + 1
        }

        override fun remove(keys: Set<String>) {
            keys.forEach(values::remove)
        }
    }
}
