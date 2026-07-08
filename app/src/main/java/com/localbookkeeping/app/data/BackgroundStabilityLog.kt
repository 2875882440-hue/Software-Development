package com.localbookkeeping.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "background_stability_logs")
data class BackgroundStabilityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val message: String = "",
    val detail: String = "",
    val createdAtMillis: Long
)

object BackgroundEventType {
    const val APP_START = "appStart"
    const val APP_STOP = "appStop"
    const val FOREGROUND_SERVICE_START = "foregroundServiceStart"
    const val FOREGROUND_SERVICE_STOP = "foregroundServiceStop"
    const val LISTENER_CONNECTED = "listenerConnected"
    const val LISTENER_DISCONNECTED = "listenerDisconnected"
    const val REQUEST_REBIND = "requestRebind"
    const val REQUEST_REBIND_RESULT = "requestRebindResult"
    const val REQUEST_REBIND_ATTEMPT = "requestRebindAttempt"
    const val HEALTH_CHECK = "healthCheck"
    const val HEALTH_HEALTHY = "healthHealthy"
    const val HEALTH_STALE = "healthStale"
    const val HEALTH_SUSPICIOUS = "healthSuspicious"
    const val HEALTH_DISCONNECTED = "healthDisconnected"
    const val HEALTH_PERMISSION_MISSING = "healthPermissionMissing"
    const val HEALTH_PERMISSION_GRANTED_BUT_NOT_CONNECTED = "healthPermissionGrantedButNotConnected"
    const val HEALTH_FOREGROUND_RUNNING_BUT_LISTENER_DEAD = "healthForegroundRunningButListenerDead"
    const val HEALTH_VENDOR_BLOCKED = "healthVendorBlocked"
    const val HEALTH_PROBE_FAILED = "healthProbeFailed"
    const val HEALTH_SERVICE_UNKNOWN = "healthServiceUnknown"
    const val PACKAGE_REPLACED = "packageReplaced"
    const val BATTERY_OPTIMIZATION_STATUS = "batteryOptimizationStatus"
    const val NOTIFICATION_PERMISSION_STATUS = "notificationPermissionStatus"
    const val PAYMENT_NOTIFICATION_CAPTURED = "paymentNotificationCaptured"
    const val PAYMENT_PARSE_SUCCESS = "paymentParseSuccess"
    const val PAYMENT_PARSE_FAIL = "paymentParseFail"
    const val IGNORED_BY_APP_FILTER = "ignoredByAppFilter"
    const val GENERIC_PAYMENT_PARSE_RESULT = "genericPaymentParseResult"
    const val AUTO_LISTEN_ENABLED = "autoListenEnabled"
    const val AUTO_LISTEN_DISABLED = "autoListenDisabled"
    const val PROBE_NOTIFICATION_SENT = "probeNotificationSent"
    const val PROBE_SUCCESS = "probeSuccess"
    const val PROBE_FAIL = "probeFail"
    const val AUTO_LISTEN_RESTORE = "autoListenRestore"
    const val FOREGROUND_SERVICE_MISSING = "foregroundServiceMissing"
    const val LISTENER_PERMISSION_GRANTED_BUT_PROBE_FAIL = "listenerPermissionGrantedButProbeFail"
    const val REQUEST_REBIND_AFTER_PROBE_FAIL = "requestRebindAfterProbeFail"
    const val LISTENER_RESCUE = "listenerRescue"
    const val BOOT_COMPLETED = "bootCompleted"
}
