package com.localbookkeeping.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BackgroundStabilityLogDao {
    @Query("SELECT * FROM background_stability_logs ORDER BY createdAtMillis DESC, id DESC LIMIT 200")
    fun observeRecent200(): Flow<List<BackgroundStabilityLog>>

    @Query("SELECT * FROM background_stability_logs WHERE createdAtMillis >= :sinceMillis ORDER BY createdAtMillis DESC, id DESC")
    suspend fun getSince(sinceMillis: Long): List<BackgroundStabilityLog>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: BackgroundStabilityLog): Long
}
