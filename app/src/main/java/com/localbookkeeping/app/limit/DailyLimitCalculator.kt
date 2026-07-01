package com.localbookkeeping.app.limit

import com.localbookkeeping.app.data.ExpenseRecord
import com.localbookkeeping.app.data.RecordStatus
import com.localbookkeeping.app.data.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyLimitConfig(
    val enabled: Boolean = false,
    val limitCents: Long = 0L,
    val mutedDate: String = ""
)

data class DailyLimitStatus(
    val config: DailyLimitConfig,
    val spentCents: Long,
    val remainingCents: Long,
    val exceededCents: Long,
    val progress: Float,
    val shouldAlert: Boolean
) {
    val enabled: Boolean get() = config.enabled
    val limitCents: Long get() = config.limitCents
    val exceeded: Boolean get() = enabled && limitCents > 0L && spentCents > limitCents
}

object DailyLimitCalculator {
    fun statusForToday(
        records: List<ExpenseRecord>,
        config: DailyLimitConfig,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyLimitStatus {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val spent = spentForDate(records, today, zoneId)
        val remaining = (config.limitCents - spent).coerceAtLeast(0L)
        val exceeded = (spent - config.limitCents).coerceAtLeast(0L)
        val progress = if (config.limitCents > 0L) {
            (spent.toDouble() / config.limitCents).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
        val shouldAlert = config.enabled &&
            config.limitCents > 0L &&
            spent > config.limitCents &&
            config.mutedDate != today.toString()
        return DailyLimitStatus(
            config = config,
            spentCents = spent,
            remainingCents = remaining,
            exceededCents = exceeded,
            progress = progress,
            shouldAlert = shouldAlert
        )
    }

    fun spentForDate(records: List<ExpenseRecord>, date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val from = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return records
            .filter { it.status == RecordStatus.CONFIRMED }
            .filterNot { it.isDeleted }
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.paidAtMillis >= from && it.paidAtMillis < to }
            .sumOf { it.amountCents }
    }
}
