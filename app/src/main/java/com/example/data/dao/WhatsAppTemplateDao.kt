package com.example.data.dao

import androidx.room.*
import com.example.data.model.WhatsAppTemplateCategory
import com.example.data.model.WhatsAppTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhatsAppTemplateDao {
    @Query("SELECT * FROM whatsapp_templates")
    fun getAllTemplates(): Flow<List<WhatsAppTemplateEntity>>

    @Query("SELECT * FROM whatsapp_templates WHERE category = :category LIMIT 1")
    suspend fun getTemplateByCategory(category: WhatsAppTemplateCategory): WhatsAppTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<WhatsAppTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WhatsAppTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: WhatsAppTemplateEntity)
}
