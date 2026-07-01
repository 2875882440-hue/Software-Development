package com.localbookkeeping.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryLearningDao {
    @Query("SELECT * FROM merchant_category_learning ORDER BY isEnabled DESC, lastUsedAt DESC, useCount DESC, id DESC")
    fun observeAll(): Flow<List<MerchantCategoryLearning>>

    @Query("SELECT * FROM merchant_category_learning WHERE isEnabled = 1 ORDER BY useCount DESC, lastUsedAt DESC, id DESC")
    suspend fun getEnabledOnce(): List<MerchantCategoryLearning>

    @Query(
        """
        SELECT * FROM merchant_category_learning
        WHERE merchantNormalized = :merchantNormalized
            AND category = :category
            AND sourceApp = :sourceApp
        LIMIT 1
        """
    )
    suspend fun findExact(merchantNormalized: String, category: String, sourceApp: String): MerchantCategoryLearning?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(learning: MerchantCategoryLearning): Long

    @Update
    suspend fun update(learning: MerchantCategoryLearning)

    @Query("UPDATE merchant_category_learning SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long)
}
