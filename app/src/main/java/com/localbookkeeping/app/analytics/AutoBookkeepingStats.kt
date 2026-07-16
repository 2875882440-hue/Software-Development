package com.localbookkeeping.app.analytics

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class AutoBookkeepingSource(val displayName: String) {
    WECHAT("微信"),
    ALIPAY("支付宝"),
    OTHER("其他应用")
}

enum class AutoBookkeepingEvent {
    NOTIFICATION_RECEIVED,
    PAYMENT_RELATED,
    PENDING_CREATED,
    AMOUNT_PARSE_FAILED,
    USER_CONFIRMED,
    USER_AMOUNT_EDITED,
    USER_DELETED,
    DUPLICATE_FILTERED
}

data class AutoBookkeepingCounters(
    val notificationReceived: Int = 0,
    val paymentRelated: Int = 0,
    val pendingCreated: Int = 0,
    val amountParseFailed: Int = 0,
    val userConfirmed: Int = 0,
    val userAmountEdited: Int = 0,
    val userDeleted: Int = 0,
    val duplicateFiltered: Int = 0
) {
    val generationSuccessRatePercent: Int?
        get() = percentage(pendingCreated, paymentRelated)

    val notificationConversionRatePercent: Int?
        get() = percentage(pendingCreated, notificationReceived)

    operator fun plus(other: AutoBookkeepingCounters): AutoBookkeepingCounters =
        AutoBookkeepingCounters(
            notificationReceived = notificationReceived + other.notificationReceived,
            paymentRelated = paymentRelated + other.paymentRelated,
            pendingCreated = pendingCreated + other.pendingCreated,
            amountParseFailed = amountParseFailed + other.amountParseFailed,
            userConfirmed = userConfirmed + other.userConfirmed,
            userAmountEdited = userAmountEdited + other.userAmountEdited,
            userDeleted = userDeleted + other.userDeleted,
            duplicateFiltered = duplicateFiltered + other.duplicateFiltered
        )

    companion object {
        private fun percentage(numerator: Int, denominator: Int): Int? =
            denominator.takeIf { it > 0 }?.let {
                (numerator.toDouble() * 100.0 / it.toDouble()).roundToInt()
            }
    }
}

data class AutoBookkeepingSourceSummary(
    val source: AutoBookkeepingSource,
    val counters: AutoBookkeepingCounters
)

data class AutoBookkeepingStatsSnapshot(
    val windowDays: Int,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val total: AutoBookkeepingCounters,
    val bySource: List<AutoBookkeepingSourceSummary>
)

internal interface AutoBookkeepingCounterStorage {
    fun readAll(): Map<String, Int>
    fun increment(key: String)
    fun remove(keys: Set<String>)
}

class AutoBookkeepingStatsStore internal constructor(
    private val storage: AutoBookkeepingCounterStorage,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun record(
        event: AutoBookkeepingEvent,
        packageName: String,
        atMillis: Long = System.currentTimeMillis()
    ) {
        val date = Instant.ofEpochMilli(atMillis).atZone(zoneId).toLocalDate()
        record(event, sourceForPackage(packageName), date)
    }

    internal fun record(
        event: AutoBookkeepingEvent,
        source: AutoBookkeepingSource,
        date: LocalDate
    ) {
        storage.increment(keyFor(date, source, event))
        prune(date)
    }

    fun snapshot(
        windowDays: Int = DEFAULT_REPORT_DAYS,
        today: LocalDate = LocalDate.now(zoneId)
    ): AutoBookkeepingStatsSnapshot {
        require(windowDays in 1..RETENTION_DAYS)
        val fromDate = today.minusDays(windowDays - 1L)
        val countersBySource = AutoBookkeepingSource.entries.associateWith { mutableMapOf<AutoBookkeepingEvent, Int>() }

        storage.readAll().forEach { (key, count) ->
            val parsed = parseKey(key) ?: return@forEach
            if (parsed.date !in fromDate..today) return@forEach
            val sourceCounters = countersBySource.getValue(parsed.source)
            sourceCounters[parsed.event] = sourceCounters.getOrDefault(parsed.event, 0) + count
        }

        val summaries = AutoBookkeepingSource.entries.map { source ->
            AutoBookkeepingSourceSummary(
                source = source,
                counters = countersFrom(countersBySource.getValue(source))
            )
        }
        return AutoBookkeepingStatsSnapshot(
            windowDays = windowDays,
            fromDate = fromDate,
            toDate = today,
            total = summaries.fold(AutoBookkeepingCounters()) { total, summary -> total + summary.counters },
            bySource = summaries
        )
    }

    private fun prune(today: LocalDate) {
        val oldestDateToKeep = today.minusDays(RETENTION_DAYS - 1L)
        val expiredKeys = storage.readAll().keys.filterTo(mutableSetOf()) { key ->
            val parsed = parseKey(key)
            parsed != null && parsed.date < oldestDateToKeep
        }
        if (expiredKeys.isNotEmpty()) storage.remove(expiredKeys)
    }

    companion object {
        const val DEFAULT_REPORT_DAYS = 14
        const val RETENTION_DAYS = 30
        private const val PREFS_NAME = "auto_bookkeeping_stats"
        private const val KEY_VERSION = "v1"

        fun create(context: Context): AutoBookkeepingStatsStore =
            AutoBookkeepingStatsStore(
                storage = SharedPreferencesCounterStorage(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                )
            )

        fun sourceForPackage(packageName: String): AutoBookkeepingSource = when (packageName) {
            "com.tencent.mm" -> AutoBookkeepingSource.WECHAT
            "com.eg.android.AlipayGphone" -> AutoBookkeepingSource.ALIPAY
            else -> AutoBookkeepingSource.OTHER
        }

        internal fun keyFor(
            date: LocalDate,
            source: AutoBookkeepingSource,
            event: AutoBookkeepingEvent
        ): String = "$KEY_VERSION|$date|${source.name}|${event.name}"

        internal fun parseKey(key: String): ParsedCounterKey? {
            val parts = key.split('|')
            if (parts.size != 4 || parts[0] != KEY_VERSION) return null
            return runCatching {
                ParsedCounterKey(
                    date = LocalDate.parse(parts[1]),
                    source = AutoBookkeepingSource.valueOf(parts[2]),
                    event = AutoBookkeepingEvent.valueOf(parts[3])
                )
            }.getOrNull()
        }

        private fun countersFrom(values: Map<AutoBookkeepingEvent, Int>): AutoBookkeepingCounters =
            AutoBookkeepingCounters(
                notificationReceived = values.getOrDefault(AutoBookkeepingEvent.NOTIFICATION_RECEIVED, 0),
                paymentRelated = values.getOrDefault(AutoBookkeepingEvent.PAYMENT_RELATED, 0),
                pendingCreated = values.getOrDefault(AutoBookkeepingEvent.PENDING_CREATED, 0),
                amountParseFailed = values.getOrDefault(AutoBookkeepingEvent.AMOUNT_PARSE_FAILED, 0),
                userConfirmed = values.getOrDefault(AutoBookkeepingEvent.USER_CONFIRMED, 0),
                userAmountEdited = values.getOrDefault(AutoBookkeepingEvent.USER_AMOUNT_EDITED, 0),
                userDeleted = values.getOrDefault(AutoBookkeepingEvent.USER_DELETED, 0),
                duplicateFiltered = values.getOrDefault(AutoBookkeepingEvent.DUPLICATE_FILTERED, 0)
            )
    }
}

internal data class ParsedCounterKey(
    val date: LocalDate,
    val source: AutoBookkeepingSource,
    val event: AutoBookkeepingEvent
)

internal fun isAutoNotificationRecordEligible(
    sourceType: String,
    packageName: String,
    notificationFingerprint: String
): Boolean =
    sourceType == "notification" &&
        packageName.isNotBlank() &&
        !notificationFingerprint.startsWith("manual-debug-log-")

private class SharedPreferencesCounterStorage(
    private val preferences: SharedPreferences
) : AutoBookkeepingCounterStorage {
    override fun readAll(): Map<String, Int> =
        preferences.all.mapNotNull { (key, value) ->
            (value as? Number)?.toInt()?.let { key to it }
        }.toMap()

    override fun increment(key: String) {
        synchronized(lock) {
            val nextValue = preferences.getInt(key, 0) + 1
            preferences.edit().putInt(key, nextValue).commit()
        }
    }

    override fun remove(keys: Set<String>) {
        synchronized(lock) {
            val editor = preferences.edit()
            keys.forEach(editor::remove)
            editor.commit()
        }
    }

    companion object {
        private val lock = Any()
    }
}
