package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_templates")
data class WhatsAppTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: WhatsAppTemplateCategory,
    val title: String,
    val messageText: String
)
