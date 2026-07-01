package com.localbookkeeping.app.notification

import android.content.Context

data class ListenerRecoverySnapshot(
    val lastHealthStatus: String,
    val lastHealthCheckAt: Long,
    val lastAutoRepairAt: Long,
    val autoRepairCount: Int,
    val lastRebindAttemptAt: Long,
    val lastRebindResult: String,
    val lastFailureReason: String,
    val needsListenerRecheck: Boolean,
    val lastPackageReplacedAt: Long
)

object ListenerRecoveryState {
    private const val PREFS = "listener_recovery_state"
    private const val KEY_LAST_HEALTH_STATUS = "last_health_status"
    private const val KEY_LAST_HEALTH_CHECK_AT = "last_health_check_at"
    private const val KEY_LAST_AUTO_REPAIR_AT = "last_auto_repair_at"
    private const val KEY_AUTO_REPAIR_COUNT = "auto_repair_count"
    private const val KEY_LAST_REBIND_ATTEMPT_AT = "last_rebind_attempt_at"
    private const val KEY_LAST_REBIND_RESULT = "last_rebind_result"
    private const val KEY_LAST_FAILURE_REASON = "last_failure_reason"
    private const val KEY_NEEDS_LISTENER_RECHECK = "needs_listener_recheck"
    private const val KEY_LAST_PACKAGE_REPLACED_AT = "last_package_replaced_at"

    fun snapshot(context: Context): ListenerRecoverySnapshot {
        val prefs = prefs(context)
        return ListenerRecoverySnapshot(
            lastHealthStatus = prefs.getString(KEY_LAST_HEALTH_STATUS, "").orEmpty(),
            lastHealthCheckAt = prefs.getLong(KEY_LAST_HEALTH_CHECK_AT, 0L),
            lastAutoRepairAt = prefs.getLong(KEY_LAST_AUTO_REPAIR_AT, 0L),
            autoRepairCount = prefs.getInt(KEY_AUTO_REPAIR_COUNT, 0),
            lastRebindAttemptAt = prefs.getLong(KEY_LAST_REBIND_ATTEMPT_AT, 0L),
            lastRebindResult = prefs.getString(KEY_LAST_REBIND_RESULT, "").orEmpty(),
            lastFailureReason = prefs.getString(KEY_LAST_FAILURE_REASON, "").orEmpty(),
            needsListenerRecheck = prefs.getBoolean(KEY_NEEDS_LISTENER_RECHECK, false),
            lastPackageReplacedAt = prefs.getLong(KEY_LAST_PACKAGE_REPLACED_AT, 0L)
        )
    }

    fun markHealthCheck(context: Context, evaluation: ListenerHealthEvaluation, checkedAtMillis: Long) {
        prefs(context).edit()
            .putString(KEY_LAST_HEALTH_STATUS, evaluation.status.name)
            .putLong(KEY_LAST_HEALTH_CHECK_AT, checkedAtMillis)
            .putString(KEY_LAST_FAILURE_REASON, evaluation.reasons.joinToString("；"))
            .apply()
    }

    fun markRebindAttempt(context: Context, attemptedAtMillis: Long) {
        prefs(context).edit()
            .putLong(KEY_LAST_REBIND_ATTEMPT_AT, attemptedAtMillis)
            .apply()
    }

    fun markRebindResult(context: Context, result: String, repairedAtMillis: Long, success: Boolean) {
        val currentCount = snapshot(context).autoRepairCount
        prefs(context).edit()
            .putString(KEY_LAST_REBIND_RESULT, result)
            .putLong(KEY_LAST_AUTO_REPAIR_AT, repairedAtMillis)
            .putInt(KEY_AUTO_REPAIR_COUNT, currentCount + 1)
            .putString(KEY_LAST_FAILURE_REASON, if (success) "" else result)
            .apply()
    }

    fun markPackageReplaced(context: Context, replacedAtMillis: Long) {
        prefs(context).edit()
            .putBoolean(KEY_NEEDS_LISTENER_RECHECK, true)
            .putLong(KEY_LAST_PACKAGE_REPLACED_AT, replacedAtMillis)
            .apply()
    }

    fun clearListenerRecheckNeeded(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_NEEDS_LISTENER_RECHECK, false)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
