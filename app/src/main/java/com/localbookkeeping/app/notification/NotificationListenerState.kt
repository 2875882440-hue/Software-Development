package com.localbookkeeping.app.notification

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log

data class NotificationListenerRuntimeState(
    val listenerConnected: Boolean,
    val rawListenerConnected: Boolean,
    val lastConnectedTime: Long,
    val lastDisconnectedTime: Long,
    val lastHeartbeatTime: Long,
    val lastNotificationTime: Long,
    val lastPaymentNotificationTime: Long,
    val lastWechatNotificationTime: Long,
    val lastAlipayNotificationTime: Long,
    val lastNotificationPackage: String,
    val lastNotificationTitle: String,
    val lastNotificationText: String,
    val lastRemovedTime: Long,
    val lastRemovedPackage: String,
    val lastRemovedTitle: String,
    val lastRemovedText: String
)

object NotificationListenerState {
    private const val PREFS = "notification_listener_state"
    private const val KEY_CONNECTED = "connected"
    private const val KEY_LAST_CONNECTED_AT = "last_connected_at"
    private const val KEY_LAST_DISCONNECTED_AT = "last_disconnected_at"
    private const val KEY_LAST_HEARTBEAT_AT = "last_heartbeat_at"
    private const val KEY_LAST_NOTIFICATION_AT = "last_notification_at"
    private const val KEY_LAST_PAYMENT_NOTIFICATION_AT = "last_payment_notification_at"
    private const val KEY_LAST_WECHAT_NOTIFICATION_AT = "last_wechat_notification_at"
    private const val KEY_LAST_ALIPAY_NOTIFICATION_AT = "last_alipay_notification_at"
    private const val KEY_LAST_NOTIFICATION_PACKAGE = "last_notification_package"
    private const val KEY_LAST_NOTIFICATION_TITLE = "last_notification_title"
    private const val KEY_LAST_NOTIFICATION_TEXT = "last_notification_text"
    private const val KEY_LAST_REMOVED_AT = "last_removed_at"
    private const val KEY_LAST_REMOVED_PACKAGE = "last_removed_package"
    private const val KEY_LAST_REMOVED_TITLE = "last_removed_title"
    private const val KEY_LAST_REMOVED_TEXT = "last_removed_text"

    fun markConnected(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_LAST_CONNECTED_AT, now)
            .putLong(KEY_LAST_HEARTBEAT_AT, now)
            .apply()
    }

    fun markDisconnected(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONNECTED, false)
            .putLong(KEY_LAST_DISCONNECTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun markHeartbeat(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_LAST_HEARTBEAT_AT, System.currentTimeMillis())
            .apply()
    }

    fun markNotificationReceived(
        context: Context,
        packageName: String,
        title: String,
        text: String
    ) {
        val now = System.currentTimeMillis()
        val editor = prefs(context).edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_LAST_HEARTBEAT_AT, now)
            .putLong(KEY_LAST_NOTIFICATION_AT, now)
            .putString(KEY_LAST_NOTIFICATION_PACKAGE, packageName)
            .putString(KEY_LAST_NOTIFICATION_TITLE, title)
            .putString(KEY_LAST_NOTIFICATION_TEXT, text)
        when (packageName) {
            WECHAT_PACKAGE -> editor.putLong(KEY_LAST_WECHAT_NOTIFICATION_AT, now)
            ALIPAY_PACKAGE -> editor.putLong(KEY_LAST_ALIPAY_NOTIFICATION_AT, now)
        }
        if (isPaymentPackage(packageName)) {
            editor.putLong(KEY_LAST_PAYMENT_NOTIFICATION_AT, now)
        }
        editor.apply()
    }

    fun markPaymentParsed(context: Context, packageName: String) {
        val now = System.currentTimeMillis()
        val editor = prefs(context).edit()
            .putLong(KEY_LAST_PAYMENT_NOTIFICATION_AT, now)
        when (packageName) {
            WECHAT_PACKAGE -> editor.putLong(KEY_LAST_WECHAT_NOTIFICATION_AT, now)
            ALIPAY_PACKAGE -> editor.putLong(KEY_LAST_ALIPAY_NOTIFICATION_AT, now)
        }
        editor.apply()
    }

    fun markNotificationRemoved(
        context: Context,
        packageName: String,
        title: String,
        text: String
    ) {
        prefs(context).edit()
            .putLong(KEY_LAST_REMOVED_AT, System.currentTimeMillis())
            .putString(KEY_LAST_REMOVED_PACKAGE, packageName)
            .putString(KEY_LAST_REMOVED_TITLE, title)
            .putString(KEY_LAST_REMOVED_TEXT, text)
            .apply()
    }

    fun isPermissionEnabled(context: Context): Boolean {
        val expected = ComponentName(context, PaymentNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure
            .getString(context.contentResolver, "enabled_notification_listeners")
            .orEmpty()
        return enabledListeners
            .split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it.packageName == expected.packageName && it.className == expected.className }
    }

    fun isConnected(context: Context): Boolean =
        isPermissionEnabled(context) &&
            prefs(context).getBoolean(KEY_CONNECTED, false) &&
            isHeartbeatFresh(context)

    fun isProbablyActive(context: Context): Boolean = isConnected(context)

    fun current(context: Context): NotificationListenerRuntimeState {
        val prefs = prefs(context)
        return NotificationListenerRuntimeState(
            listenerConnected = isConnected(context),
            rawListenerConnected = prefs.getBoolean(KEY_CONNECTED, false),
            lastConnectedTime = prefs.getLong(KEY_LAST_CONNECTED_AT, 0L),
            lastDisconnectedTime = prefs.getLong(KEY_LAST_DISCONNECTED_AT, 0L),
            lastHeartbeatTime = prefs.getLong(KEY_LAST_HEARTBEAT_AT, 0L),
            lastNotificationTime = prefs.getLong(KEY_LAST_NOTIFICATION_AT, 0L),
            lastPaymentNotificationTime = prefs.getLong(KEY_LAST_PAYMENT_NOTIFICATION_AT, 0L),
            lastWechatNotificationTime = prefs.getLong(KEY_LAST_WECHAT_NOTIFICATION_AT, 0L),
            lastAlipayNotificationTime = prefs.getLong(KEY_LAST_ALIPAY_NOTIFICATION_AT, 0L),
            lastNotificationPackage = prefs.getString(KEY_LAST_NOTIFICATION_PACKAGE, "").orEmpty(),
            lastNotificationTitle = prefs.getString(KEY_LAST_NOTIFICATION_TITLE, "").orEmpty(),
            lastNotificationText = prefs.getString(KEY_LAST_NOTIFICATION_TEXT, "").orEmpty(),
            lastRemovedTime = prefs.getLong(KEY_LAST_REMOVED_AT, 0L),
            lastRemovedPackage = prefs.getString(KEY_LAST_REMOVED_PACKAGE, "").orEmpty(),
            lastRemovedTitle = prefs.getString(KEY_LAST_REMOVED_TITLE, "").orEmpty(),
            lastRemovedText = prefs.getString(KEY_LAST_REMOVED_TEXT, "").orEmpty()
        )
    }

    fun componentName(context: Context): ComponentName =
        ComponentName(context, PaymentNotificationListenerService::class.java)

    fun requestRebind(context: Context): Boolean {
        val componentName = componentName(context)
        Log.i(TAG, "requestRebind componentName=$componentName, sdk=${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(componentName)
            Log.i(TAG, "requestRebind called result=true, componentName=$componentName")
            return true
        }
        Log.w(TAG, "requestRebind skipped result=false, componentName=$componentName")
        return false
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isHeartbeatFresh(context: Context): Boolean {
        val lastHeartbeatAt = prefs(context).getLong(KEY_LAST_HEARTBEAT_AT, 0L)
        if (lastHeartbeatAt <= 0L) return false
        return System.currentTimeMillis() - lastHeartbeatAt <= CONNECTED_HEARTBEAT_TIMEOUT_MILLIS
    }

    private const val TAG = "PaymentNotificationListener"
    private const val CONNECTED_HEARTBEAT_TIMEOUT_MILLIS = 90_000L
    private const val WECHAT_PACKAGE = "com.tencent.mm"
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"

    private fun isPaymentPackage(packageName: String): Boolean =
        packageName == WECHAT_PACKAGE || packageName == ALIPAY_PACKAGE
}
