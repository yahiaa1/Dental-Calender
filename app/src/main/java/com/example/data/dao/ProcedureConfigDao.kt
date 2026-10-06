package com.example.data.dao

import androidx.room.*
import com.example.data.model.ProcedureConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcedureConfigDao {
    @Query("SELECT * FROM procedure_configs ORDER BY id ASC")
    fun getAllProcedures(): Flow<List<ProcedureConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedures(procedures: List<ProcedureConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedure(procedure: ProcedureConfigEntity): Long

    @Update
    suspend fun updateProcedure(procedure: ProcedureConfigEntity)

    @Query("UPDATE procedure_configs SET colorHex = :colorHex WHERE name = :name")
    suspend fun updateProcedureColor(name: String, colorHex: String)

    @Query("DELETE FROM procedure_configs WHERE id = :id")
    suspend fun deleteProcedureById(id: Long)
}
