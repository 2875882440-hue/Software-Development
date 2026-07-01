package com.localbookkeeping.app.stats

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class StatsRangeType {
    TODAY,
    WEEK,
    MONTH,
    CUSTOM
}

enum class BillGroupType {
    DAY,
    WEEK,
    MONTH
}

data class TimeRange(
    val fromMillis: Long,
    val toMillis: Long,
    val label: String
)

data class SummaryItem(
    val label: String,
    val amountCents: Long,
    val count: Int,
    val percent: Double
)

data class BillGroup(
    val label: String,
    val records: List<ExpenseRecord>,
    val expenseCents: Long,
    val incomeCents: Long
)

data class BillStatistics(
    val range: TimeRange,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val balanceCents: Long,
    val expenseCount: Int,
    val incomeCount: Int,
    val largestExpense: ExpenseRecord?,
    val latestRecord: ExpenseRecord?,
    val categoryItems: List<SummaryItem>,
    val sourceItems: List<SummaryItem>,
    val groups: List<BillGroup>
)

data class ArchivePeriod(
    val label: String,
    val range: TimeRange,
    val statistics: BillStatistics
)

object BillStatisticsCalculator {
    fun rangeFor(
        type: StatsRangeType,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        customStartMillis: Long? = null,
        customEndMillis: Long? = null
    ): TimeRange {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate = when (type) {
            StatsRangeType.TODAY -> today
            StatsRangeType.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            StatsRangeType.MONTH -> today.withDayOfMonth(1)
            StatsRangeType.CUSTOM -> customStartMillis?.let { millisToDate(it, zoneId) } ?: today
        }
        val endDateExclusive = when (type) {
            StatsRangeType.TODAY -> startDate.plusDays(1)
            StatsRangeType.WEEK -> startDate.plusDays(7)
            StatsRangeType.MONTH -> startDate.plusMonths(1)
            StatsRangeType.CUSTOM -> customEndMillis?.let { millisToDate(it, zoneId).plusDays(1) } ?: startDate.plusDays(1)
        }
        val label = when (type) {
            StatsRangeType.TODAY -> "今日"
            StatsRangeType.WEEK -> "本周"
            StatsRangeType.MONTH -> "本月"
            StatsRangeType.CUSTOM -> "${startDate} 至 ${endDateExclusive.minusDays(1)}"
        }
        return TimeRange(
            fromMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            toMillis = endDateExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            label = label
        )
    }

    fun calculate(
        records: List<ExpenseRecord>,
        range: TimeRange,
        groupType: BillGroupType = BillGroupType.DAY,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BillStatistics {
        val confirmed = records
            .filter { it.status.name == "CONFIRMED" }
            .filterNot { it.isDeleted }
            .filter { it.paidAtMillis >= range.fromMillis && it.paidAtMillis < range.toMillis }
            .sortedByDescending { it.paidAtMillis }
        val expenses = confirmed.filter { it.type == TransactionType.EXPENSE }
        val incomes = confirmed.filter { it.type == TransactionType.INCOME }
        val totalExpense = expenses.sumOf { it.amountCents }
        val totalIncome = incomes.sumOf { it.amountCents }
        return BillStatistics(
            range = range,
            totalIncomeCents = totalIncome,
            totalExpenseCents = totalExpense,
            balanceCents = totalIncome - totalExpense,
            expenseCount = expenses.size,
            incomeCount = incomes.size,
            largestExpense = expenses.maxByOrNull { it.amountCents },
            latestRecord = confirmed.firstOrNull(),
            categoryItems = summarize(
                records = expenses,
                totalCents = totalExpense,
                labelOf = { normalizeCategory(it.category) }
            ),
            sourceItems = summarize(
                records = confirmed,
                totalCents = confirmed.sumOf { it.amountCents },
                labelOf = { normalizeSource(it) }
            ),
            groups = groupRecords(confirmed, groupType, zoneId)
        )
    }

    fun monthWeekArchives(
        records: List<ExpenseRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ArchivePeriod> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val monthStart = today.withDayOfMonth(1)
        val monthEndExclusive = monthStart.plusMonths(1)
        return (0..4).mapNotNull { index ->
            val start = monthStart.plusDays(index * 7L)
            if (!start.isBefore(monthEndExclusive)) return@mapNotNull null
            val endExclusive = minOf(start.plusDays(7), monthEndExclusive)
            val range = TimeRange(
                fromMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                toMillis = endExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                label = weekLabel(index + 1)
            )
            ArchivePeriod(
                label = weekLabel(index + 1),
                range = range,
                statistics = calculate(records, range, BillGroupType.DAY, zoneId)
            )
        }
    }

    fun yearMonthArchives(
        records: List<ExpenseRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ArchivePeriod> {
        val year = Instant.ofEpochMilli(nowMillis).atZone(zoneId).year
        return (1..12).map { month ->
            val start = LocalDate.of(year, month, 1)
            val endExclusive = start.plusMonths(1)
            val range = TimeRange(
                fromMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                toMillis = endExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                label = "${month}月"
            )
            ArchivePeriod(
                label = "${month}月",
                range = range,
                statistics = calculate(records, range, BillGroupType.DAY, zoneId)
            )
        }
    }

    private fun weekLabel(index: Int): String = when (index) {
        1 -> "第一周"
        2 -> "第二周"
        3 -> "第三周"
        4 -> "第四周"
        else -> "第五周"
    }

    fun normalizeCategory(category: String): String = when (category.trim()) {
        "餐饮" -> "餐饮"
        "交通" -> "交通"
        "购物" -> "购物"
        "娱乐" -> "娱乐"
        "生活" -> "生活"
        "医疗" -> "医疗"
        "住房" -> "住房"
        "待分类", "未分类", "" -> "未分类"
        else -> "其他"
    }

    fun normalizeSource(record: ExpenseRecord): String {
        val source = record.sourceApp.trim()
        val packageName = record.notificationPackageName
        val sourceType = record.sourceType
        return when {
            source.contains("微信") || packageName == "com.tencent.mm" -> "微信"
            source.contains("支付宝") || packageName == "com.eg.android.AlipayGphone" -> "支付宝"
            sourceType == "screenshot" || source.contains("截图") -> "截图识别"
            source == "手动" || source.contains("补录") -> "手动补录"
            sourceType == "notification" || packageName.isNotBlank() -> "通知识别"
            else -> "其他"
        }
    }

    private fun summarize(
        records: List<ExpenseRecord>,
        totalCents: Long,
        labelOf: (ExpenseRecord) -> String
    ): List<SummaryItem> =
        records.groupBy(labelOf)
            .map { (label, group) ->
                val amount = group.sumOf { it.amountCents }
                SummaryItem(
                    label = label,
                    amountCents = amount,
                    count = group.size,
                    percent = if (totalCents > 0) amount.toDouble() / totalCents else 0.0
                )
            }
            .sortedByDescending { it.amountCents }

    private fun groupRecords(
        records: List<ExpenseRecord>,
        groupType: BillGroupType,
        zoneId: ZoneId
    ): List<BillGroup> =
        records.groupBy { record ->
            val date = millisToDate(record.paidAtMillis, zoneId)
            when (groupType) {
                BillGroupType.DAY -> date.toString()
                BillGroupType.WEEK -> {
                    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    "${monday} 周"
                }
                BillGroupType.MONTH -> "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
            }
        }.map { (label, group) ->
            BillGroup(
                label = label,
                records = group.sortedByDescending { it.paidAtMillis },
                expenseCents = group.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
                incomeCents = group.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
            )
        }.sortedByDescending { it.label }

    private fun millisToDate(millis: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
}
