package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinic_users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val role: UserRole = UserRole.STAFF,
    val username: String = "",
    val password: String = "",
    val deviceIdentifier: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
