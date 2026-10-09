package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String, // Action execution date e.g. "2026-10-08"
    val timeStr: String, // Action execution time e.g. "10:45 م"
    val actionType: ActionLogType,
    val employeeId: Long,
    val employeeName: String,
    val employeeRole: String, // e.g. "طبيب / أدمن"
    val patientName: String,
    val patientPhone: String = "",
    val appointmentId: Long,
    val appointmentDate: String, // Date of the target appointment
    val appointmentTime: String, // Time of the target appointment
    val procedureName: String = "",
    val details: String, // Full descriptive text of the action
    val extraInfo: String = ""
)
