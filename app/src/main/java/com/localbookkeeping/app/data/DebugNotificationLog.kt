package com.localbookkeeping.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_notification_logs")
data class DebugNotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String,
    val bigText: String,
    val textLines: String,
    val rawText: String,
    val postTime: Long,
    val receivedAtMillis: Long,
    val parseStatus: String,
    val failureReason: String,
    val notificationKey: String = "",
    val isPaymentNotification: Boolean = false,
    val hasAmount: Boolean = false,
    val pendingCreated: Boolean = false,
    val parseReason: String = "",
    val ruleMatched: Boolean = false,
    val matchedRuleName: String = "",
    val confidence: Int = 0,
    val finalCategory: String = "",
    val isFromPaymentApp: Boolean = false,
    val isPaymentRelated: Boolean = false,
    val isParsed: Boolean = false,
    val failReason: String = "",
    val serviceAlive: Boolean = false,
    val appInBackground: Boolean = false,
    val screenLocked: Boolean = false,
    val amountCandidates: String = "",
    val selectedAmount: String = "",
    val selectedReason: String = ""
)
