package com.localbookkeeping.app.stats

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StatsArchiveCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = millis(2026, 6, 24, 12, 0)

    @Test
    fun monthWeeksOnlyKeepWeeksWithRecords() {
        val archives = BillStatisticsCalculator.monthWeekArchives(
            records = listOf(
                record(1, 1_000L, millis(2026, 6, 2, 9, 0)),
                record(2, 4_000L, millis(2026, 6, 23, 9, 0))
            ),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(listOf("第一周", "第四周"), archives.map { it.label })
        assertEquals(1_000L, archives[0].statistics.totalExpenseCents)
        assertEquals(4_000L, archives[1].statistics.totalExpenseCents)
    }

    @Test
    fun monthWeeksReturnEmptyWhenCurrentMonthHasNoRecords() {
        val archives = BillStatisticsCalculator.monthWeekArchives(
            records = listOf(record(1, 1_000L, millis(2026, 5, 2, 9, 0))),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(emptyList<String>(), archives.map { it.label })
    }

    @Test
    fun yearMonthsOnlyKeepMonthsWithRecords() {
        val archives = BillStatisticsCalculator.yearMonthArchives(
            records = listOf(
                record(1, 3_000L, millis(2026, 3, 2, 9, 0)),
                record(2, 4_000L, millis(2026, 4, 24, 9, 0)),
                record(3, 12_000L, millis(2025, 12, 31, 9, 0))
            ),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(listOf("3月", "4月"), archives.map { it.label })
        assertEquals(3_000L, archives.first { it.label == "3月" }.statistics.totalExpenseCents)
        assertEquals(4_000L, archives.first { it.label == "4月" }.statistics.totalExpenseCents)
    }

    @Test
    fun yearMonthsReturnEmptyWhenYearHasNoRecords() {
        val archives = BillStatisticsCalculator.yearMonthArchives(
            records = listOf(record(1, 1_000L, millis(2025, 12, 31, 9, 0))),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(emptyList<String>(), archives.map { it.label })
    }

    private fun record(id: Long, amountCents: Long, paidAtMillis: Long): ExpenseRecord =
        ExpenseRecord(
            id = id,
            amountCents = amountCents,
            type = TransactionType.EXPENSE,
            status = RecordStatus.CONFIRMED,
            category = "餐饮",
            note = "",
            paidAtMillis = paidAtMillis,
            createdAtMillis = paidAtMillis,
            updatedAtMillis = paidAtMillis
        )

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
