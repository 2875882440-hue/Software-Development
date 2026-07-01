package com.localbookkeeping.app.stats

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BillStatisticsCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = millis(2026, 6, 12, 12, 0)

    @Test
    fun calculatesTodayStats() {
        val stats = BillStatisticsCalculator.calculate(
            records = sampleRecords(),
            range = BillStatisticsCalculator.rangeFor(StatsRangeType.TODAY, now, zone),
            zoneId = zone
        )

        assertEquals(1280L, stats.totalExpenseCents)
        assertEquals(1, stats.expenseCount)
        assertEquals(0L, stats.totalIncomeCents)
        assertNotNull(stats.latestRecord)
    }

    @Test
    fun calculatesWeekStatsStartingMonday() {
        val range = BillStatisticsCalculator.rangeFor(StatsRangeType.WEEK, now, zone)
        val stats = BillStatisticsCalculator.calculate(sampleRecords(), range, zoneId = zone)

        assertEquals(millis(2026, 6, 8, 0, 0), range.fromMillis)
        assertEquals(millis(2026, 6, 15, 0, 0), range.toMillis)
        assertEquals(6280L, stats.totalExpenseCents)
        assertEquals(20000L, stats.totalIncomeCents)
    }

    @Test
    fun calculatesMonthStats() {
        val range = BillStatisticsCalculator.rangeFor(StatsRangeType.MONTH, now, zone)
        val stats = BillStatisticsCalculator.calculate(sampleRecords(), range, zoneId = zone)

        assertEquals(millis(2026, 6, 1, 0, 0), range.fromMillis)
        assertEquals(millis(2026, 7, 1, 0, 0), range.toMillis)
        assertEquals(9280L, stats.totalExpenseCents)
    }

    @Test
    fun summarizesCategoryAndSource() {
        val stats = BillStatisticsCalculator.calculate(
            records = sampleRecords(),
            range = BillStatisticsCalculator.rangeFor(StatsRangeType.MONTH, now, zone),
            zoneId = zone
        )

        assertEquals(6280L, stats.categoryItems.first { it.label == "餐饮" }.amountCents)
        assertEquals(3000L, stats.categoryItems.first { it.label == "购物" }.amountCents)
        assertEquals(6280L, stats.sourceItems.first { it.label == "微信" }.amountCents)
        assertEquals(3000L, stats.sourceItems.first { it.label == "支付宝" }.amountCents)
    }

    @Test
    fun updatesTodayStatsAfterRecordAmountChanges() {
        val changed = sampleRecords().map {
            if (it.id == 1L) it.copy(amountCents = 2580L, updatedAtMillis = now) else it
        }
        val stats = BillStatisticsCalculator.calculate(
            records = changed,
            range = BillStatisticsCalculator.rangeFor(StatsRangeType.TODAY, now, zone),
            zoneId = zone
        )

        assertEquals(2580L, stats.totalExpenseCents)
        assertEquals(1, stats.expenseCount)
    }

    @Test
    fun excludesSoftDeletedRecordsFromStats() {
        val deleted = sampleRecords().map {
            if (it.id == 1L) it.copy(isDeleted = true, deletedAtMillis = now, updatedAtMillis = now) else it
        }
        val stats = BillStatisticsCalculator.calculate(
            records = deleted,
            range = BillStatisticsCalculator.rangeFor(StatsRangeType.TODAY, now, zone),
            zoneId = zone
        )

        assertEquals(0L, stats.totalExpenseCents)
        assertEquals(0, stats.expenseCount)
    }

    private fun sampleRecords(): List<ExpenseRecord> = listOf(
        record(1, 1280, TransactionType.EXPENSE, "餐饮", "微信", millis(2026, 6, 12, 8, 0), "com.tencent.mm"),
        record(2, 5000, TransactionType.EXPENSE, "餐饮", "微信", millis(2026, 6, 10, 9, 0), "com.tencent.mm"),
        record(3, 20000, TransactionType.INCOME, "工资", "手动", millis(2026, 6, 9, 9, 0)),
        record(4, 3000, TransactionType.EXPENSE, "购物", "支付宝", millis(2026, 6, 2, 9, 0), "com.eg.android.AlipayGphone"),
        record(5, 9900, TransactionType.EXPENSE, "娱乐", "微信", millis(2026, 5, 31, 9, 0), "com.tencent.mm")
    )

    private fun record(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        sourceApp: String,
        paidAtMillis: Long,
        packageName: String = ""
    ): ExpenseRecord =
        ExpenseRecord(
            id = id,
            amountCents = amountCents,
            type = type,
            status = RecordStatus.CONFIRMED,
            category = category,
            note = category,
            paidAtMillis = paidAtMillis,
            sourceApp = sourceApp,
            notificationPackageName = packageName,
            createdAtMillis = paidAtMillis,
            updatedAtMillis = paidAtMillis
        )

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
