package com.example.data.dao

import androidx.room.*
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY dateStr ASC, startTime ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY dateStr ASC, startTime ASC")
    fun getAllAppointmentsSync(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE dateStr = :dateStr ORDER BY startTime ASC")
    fun getAppointmentsForDate(dateStr: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE dateStr = :dateStr ORDER BY startTime ASC")
    fun getAppointmentsForDateSync(dateStr: String): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE dateStr BETWEEN :startDateStr AND :endDateStr ORDER BY dateStr ASC, startTime ASC")
    fun getAppointmentsForDateRange(startDateStr: String, endDateStr: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY dateStr DESC, startTime DESC")
    fun getAppointmentsForPatient(patientId: Long): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateAppointmentStatus(id: Long, status: AppointmentStatus)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments")
    suspend fun clearAllAppointments()

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Long)
}
