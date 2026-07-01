package com.localbookkeeping.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT * FROM expense_records
        WHERE status != 'IGNORED'
            AND isDeleted = 0
        ORDER BY
            CASE
                WHEN status = 'NEED_AMOUNT' THEN 0
                WHEN status = 'PENDING_CONFIRM' THEN 1
                WHEN status = 'PENDING' THEN 1
                ELSE 2
            END,
            paidAtMillis DESC,
            id DESC
        """
    )
    fun observeAll(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expense_records WHERE isDeleted = 0 ORDER BY paidAtMillis DESC, id DESC")
    suspend fun getAllOnce(): List<ExpenseRecord>

    @Query("SELECT * FROM expense_records ORDER BY paidAtMillis DESC, id DESC")
    suspend fun getAllIncludingDeletedOnce(): List<ExpenseRecord>

    @Query(
        """
        SELECT * FROM expense_records
        WHERE status = 'CONFIRMED'
            AND isDeleted = 0
            AND paidAtMillis >= :fromMillis
            AND paidAtMillis < :toMillis
        ORDER BY paidAtMillis DESC, id DESC
        """
    )
    suspend fun getConfirmedBetween(fromMillis: Long, toMillis: Long): List<ExpenseRecord>

    @Query(
        """
        SELECT category AS label, SUM(amountCents) AS amountCents, COUNT(*) AS count
        FROM expense_records
        WHERE status = 'CONFIRMED'
            AND isDeleted = 0
            AND type = 'EXPENSE'
            AND paidAtMillis >= :fromMillis
            AND paidAtMillis < :toMillis
        GROUP BY category
        ORDER BY amountCents DESC
        """
    )
    suspend fun getExpenseSummaryByCategory(fromMillis: Long, toMillis: Long): List<SummaryRow>

    @Query(
        """
        SELECT sourceApp AS label, SUM(amountCents) AS amountCents, COUNT(*) AS count
        FROM expense_records
        WHERE status = 'CONFIRMED'
            AND isDeleted = 0
            AND paidAtMillis >= :fromMillis
            AND paidAtMillis < :toMillis
        GROUP BY sourceApp
        ORDER BY amountCents DESC
        """
    )
    suspend fun getSummaryBySource(fromMillis: Long, toMillis: Long): List<SummaryRow>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', paidAtMillis / 1000, 'unixepoch', 'localtime') AS label,
               SUM(CASE WHEN type = 'EXPENSE' THEN amountCents ELSE 0 END) AS amountCents,
               COUNT(*) AS count
        FROM expense_records
        WHERE status = 'CONFIRMED'
            AND isDeleted = 0
            AND paidAtMillis >= :fromMillis
            AND paidAtMillis < :toMillis
        GROUP BY label
        ORDER BY label DESC
        """
    )
    suspend fun getDailySummary(fromMillis: Long, toMillis: Long): List<SummaryRow>

    @Query("SELECT COUNT(*) FROM expense_records WHERE notificationFingerprint = :fingerprint AND notificationFingerprint != '' AND isDeleted = 0")
    suspend fun countByNotificationFingerprint(fingerprint: String): Int

    @Query(
        """
        SELECT * FROM expense_records
        WHERE sourceApp = :sourceApp
            AND notificationPackageName = :packageName
            AND amountCents = :amountCents
            AND notificationPostedAtMillis BETWEEN :fromMillis AND :toMillis
            AND rawText != ''
            AND status != 'IGNORED'
            AND isDeleted = 0
        ORDER BY notificationPostedAtMillis DESC, id DESC
        LIMIT 20
        """
    )
    suspend fun findRecentNotificationCandidates(
        sourceApp: String,
        packageName: String,
        amountCents: Long,
        fromMillis: Long,
        toMillis: Long
    ): List<ExpenseRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ExpenseRecord): Long

    @Query("UPDATE expense_records SET status = 'CONFIRMED', updatedAtMillis = :updatedAtMillis WHERE id = :id AND status IN ('PENDING_CONFIRM', 'PENDING')")
    suspend fun confirmPending(id: Long, updatedAtMillis: Long)

    @Query(
        """
        UPDATE expense_records
        SET amountCents = :amountCents,
            type = :type,
            category = :category,
            categorySource = 'manual',
            matchedRuleName = '',
            matchedKeyword = '',
            learnedMerchantId = :learnedMerchantId,
            merchantName = :merchantName,
            note = :note,
            paidAtMillis = :paidAtMillis,
            status = 'CONFIRMED',
            updatedAtMillis = :updatedAtMillis
        WHERE id = :id
        """
    )
    suspend fun updatePendingAndConfirm(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long,
        learnedMerchantId: Long,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE expense_records
        SET amountCents = :amountCents,
            type = :type,
            category = :category,
            categorySource = 'manual',
            matchedRuleName = '',
            matchedKeyword = '',
            learnedMerchantId = :learnedMerchantId,
            merchantName = :merchantName,
            note = :note,
            paidAtMillis = :paidAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :id
            AND status = 'CONFIRMED'
            AND isDeleted = 0
        """
    )
    suspend fun updateConfirmedRecord(
        id: Long,
        amountCents: Long,
        type: TransactionType,
        category: String,
        merchantName: String,
        note: String,
        paidAtMillis: Long,
        learnedMerchantId: Long,
        updatedAtMillis: Long
    )

    @Query("UPDATE expense_records SET status = 'IGNORED', updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun ignorePending(id: Long, updatedAtMillis: Long)

    @Query("UPDATE expense_records SET isDeleted = 1, deletedAtMillis = :deletedAtMillis, updatedAtMillis = :deletedAtMillis WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAtMillis: Long)
}

data class SummaryRow(
    val label: String,
    val amountCents: Long,
    val count: Int
)
