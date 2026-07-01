package com.localbookkeeping.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugNotificationLogDao {
    @Query("SELECT * FROM debug_notification_logs ORDER BY receivedAtMillis DESC, id DESC LIMIT 100")
    fun observeRecent100(): Flow<List<DebugNotificationLog>>

    @Query("SELECT COUNT(*) FROM debug_notification_logs WHERE receivedAtMillis >= :sinceMillis")
    suspend fun countSince(sinceMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: DebugNotificationLog): Long

    @Query(
        """
        UPDATE debug_notification_logs
        SET parseStatus = :parseStatus,
            failureReason = :failureReason,
            isPaymentNotification = :isPaymentNotification,
            hasAmount = :hasAmount,
            pendingCreated = :pendingCreated,
            parseReason = :parseReason,
            ruleMatched = :ruleMatched,
            matchedRuleName = :matchedRuleName,
            confidence = :confidence,
            finalCategory = :finalCategory,
            isPaymentRelated = :isPaymentRelated,
            isParsed = :isParsed,
            failReason = :failReason
        WHERE id = :id
        """
    )
    suspend fun updateParseResult(
        id: Long,
        parseStatus: String,
        failureReason: String,
        isPaymentNotification: Boolean,
        hasAmount: Boolean,
        pendingCreated: Boolean,
        parseReason: String,
        ruleMatched: Boolean,
        matchedRuleName: String,
        confidence: Int,
        finalCategory: String,
        isPaymentRelated: Boolean,
        isParsed: Boolean,
        failReason: String
    )
}
