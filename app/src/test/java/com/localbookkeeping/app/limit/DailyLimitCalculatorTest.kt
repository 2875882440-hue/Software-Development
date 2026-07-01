package com.localbookkeeping.app.limit

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailyLimitCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = millis(2026, 6, 24, 12, 0)

    @Test
    fun defaultLimitIsDisabled() {
        val status = DailyLimitCalculator.statusForToday(emptyList(), DailyLimitConfig(), now, zone)

        assertFalse(status.enabled)
        assertEquals(0L, status.limitCents)
        assertFalse(status.shouldAlert)
    }

    @Test
    fun savesHundredYuanConfigShape() {
        val config = DailyLimitConfig(enabled = true, limitCents = 10_000L)
        val status = DailyLimitCalculator.statusForToday(emptyList(), config, now, zone)

        assertTrue(status.enabled)
        assertEquals(10_000L, status.limitCents)
    }

    @Test
    fun eightyYuanDoesNotAlertWhenLimitIsHundred() {
        val status = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 8_000L)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(8_000L, status.spentCents)
        assertFalse(status.shouldAlert)
        assertEquals(2_000L, status.remainingCents)
    }

    @Test
    fun alertsWhenTodayExpenseExceedsLimit() {
        val status = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 11_000L)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertTrue(status.shouldAlert)
        assertEquals(1_000L, status.exceededCents)
    }

    @Test
    fun stillAlertsAfterAnotherExpenseWhenMutedIsNotSet() {
        val first = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 11_000L)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )
        val second = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 11_000L), record(2, 1_000L)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertTrue(first.shouldAlert)
        assertTrue(second.shouldAlert)
        assertEquals(12_000L, second.spentCents)
    }

    @Test
    fun incomeDoesNotCountIntoLimit() {
        val status = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 20_000L, TransactionType.INCOME)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(0L, status.spentCents)
        assertFalse(status.shouldAlert)
    }

    @Test
    fun deletedRecordsAreExcluded() {
        val status = DailyLimitCalculator.statusForToday(
            records = listOf(record(1, 11_000L, isDeleted = true)),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(0L, status.spentCents)
        assertFalse(status.shouldAlert)
    }

    @Test
    fun editedAmountRecalculatesStatus() {
        val edited = record(1, 12_000L)
        val status = DailyLimitCalculator.statusForToday(
            records = listOf(edited),
            config = DailyLimitConfig(enabled = true, limitCents = 10_000L),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(12_000L, status.spentCents)
        assertTrue(status.shouldAlert)
    }

    private fun record(
        id: Long,
        amountCents: Long,
        type: TransactionType = TransactionType.EXPENSE,
        isDeleted: Boolean = false
    ): ExpenseRecord =
        ExpenseRecord(
            id = id,
            amountCents = amountCents,
            type = type,
            status = RecordStatus.CONFIRMED,
            category = if (type == TransactionType.INCOME) "收入" else "餐饮",
            note = "",
            paidAtMillis = now,
            isDeleted = isDeleted,
            createdAtMillis = now,
            updatedAtMillis = now + id
        )

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
