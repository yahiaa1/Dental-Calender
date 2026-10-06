package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val classification: PatientClassification = PatientClassification.DISCIPLINED,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
