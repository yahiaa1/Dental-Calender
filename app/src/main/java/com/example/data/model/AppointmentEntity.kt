package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val patientName: String,
    val patientPhone: String,
    val procedureName: String,
    val chairNumber: Int = 1, // Single chair clinic
    val dateStr: String, // Format: YYYY-MM-DD
    val startTime: String, // Format: HH:mm (24h internal storage)
    val endTime: String, // Format: HH:mm (24h internal storage)
    val price: Double = 0.0,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val patientClassification: PatientClassification = PatientClassification.DISCIPLINED,
    val notes: String = "",
    val bookedByUserId: Long = 1L,
    val bookedByUserName: String = "د. محمد (أدمن)",
    val reminderSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
