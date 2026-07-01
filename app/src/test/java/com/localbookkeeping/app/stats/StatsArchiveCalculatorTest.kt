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
    fun monthWeeksUseFirstToFifthWeekCards() {
        val archives = BillStatisticsCalculator.monthWeekArchives(
            records = listOf(
                record(1, 1_000L, millis(2026, 6, 2, 9, 0)),
                record(2, 2_000L, millis(2026, 6, 9, 9, 0)),
                record(3, 3_000L, millis(2026, 6, 16, 9, 0)),
                record(4, 4_000L, millis(2026, 6, 23, 9, 0)),
                record(5, 5_000L, millis(2026, 6, 30, 9, 0))
            ),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(listOf("第一周", "第二周", "第三周", "第四周", "第五周"), archives.map { it.label })
        assertEquals(1_000L, archives[0].statistics.totalExpenseCents)
        assertEquals(5_000L, archives[4].statistics.totalExpenseCents)
    }

    @Test
    fun yearMonthsCreateTwelveArchiveCards() {
        val archives = BillStatisticsCalculator.yearMonthArchives(
            records = listOf(
                record(1, 1_000L, millis(2026, 1, 2, 9, 0)),
                record(2, 6_000L, millis(2026, 6, 24, 9, 0)),
                record(3, 12_000L, millis(2026, 12, 31, 9, 0))
            ),
            nowMillis = now,
            zoneId = zone
        )

        assertEquals(12, archives.size)
        assertEquals("1月", archives.first().label)
        assertEquals("12月", archives.last().label)
        assertEquals(6_000L, archives.first { it.label == "6月" }.statistics.totalExpenseCents)
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
