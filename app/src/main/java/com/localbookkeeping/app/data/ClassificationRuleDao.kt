package com.localbookkeeping.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassificationRuleDao {
    @Query("SELECT * FROM classification_rules ORDER BY enabled DESC, priority ASC, updatedAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<ClassificationRule>>

    @Query("SELECT * FROM classification_rules WHERE enabled = 1 ORDER BY priority ASC, updatedAtMillis DESC, id DESC")
    suspend fun getEnabledRulesOnce(): List<ClassificationRule>

    @Query("SELECT COUNT(*) FROM classification_rules")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: ClassificationRule): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rules: List<ClassificationRule>)

    @Update
    suspend fun update(rule: ClassificationRule)

    @Delete
    suspend fun delete(rule: ClassificationRule)

    @Query("UPDATE classification_rules SET enabled = :enabled, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAtMillis: Long)
}
