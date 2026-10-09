package com.example.data.dao

import androidx.room.*
import com.example.data.model.ActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLogEntity): Long

    @Query("DELETE FROM action_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM action_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}
