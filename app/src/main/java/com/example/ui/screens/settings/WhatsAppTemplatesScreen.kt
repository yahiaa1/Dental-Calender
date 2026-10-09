package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WhatsAppTemplateCategory
import com.example.data.model.WhatsAppTemplateEntity
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun WhatsAppTemplatesScreen(
    templates: List<WhatsAppTemplateEntity>,
    onSaveTemplate: (WhatsAppTemplateEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(WhatsAppTemplateCategory.BEFORE_APPOINTMENT) }

    val currentTemplate = remember(templates, selectedCategory) {
        templates.firstOrNull { it.category == selectedCategory }
            ?: WhatsAppTemplateEntity(
                0,
                selectedCategory,
                selectedCategory.label,
                when (selectedCategory) {
                    WhatsAppTemplateCategory.ONE_HOUR_BEFORE ->
                        "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}. نحن في انتظار زيارتك!"
                    WhatsAppTemplateCategory.BEFORE_APPOINTMENT ->
                        "مرحبا {اسم المريض} 🌟\nنذكرك بموعدك في {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nفي حالة عدم الحضور يرجى إبلاغنا مسبقاً.\nشكراً لك 🌟"
                    WhatsAppTemplateCategory.AFTER_TREATMENT ->
                        "أهلاً {اسم المريض} 🌸\nنتمنى لك دوام الصحة والعافية بعد زيارتك لعيادة {اسم العيادة} اليوم.\nيرجى اتباع إرشادات الطبيب والتواصل معنا في حال حدوث أي استفسار.\nمع تمنياتنا لك بابتسامة مشرقة! ✨"
                    WhatsAppTemplateCategory.CONFIRMATION ->
                        "مرحباً {اسم المريض} ✅\nتم تأكيد حجز موعدك بنجاح في عيادة {اسم العيادة}.\nالتاريخ: {تاريخ الموعد}\nالوقت: {وقت الموعد}\nنتشرف بزيارتك ويسعدنا تقديم أفضل رعاية لك."
                    WhatsAppTemplateCategory.CANCELLATION ->
                        "عزيزنا {اسم المريض} ⚠️\nنود إفادتك بأنه تم إلغاء موعدك المحدد بتاريخ {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nلتحديد موعد بديل يناسبك، نسعد بتواصلك معنا في أي وقت."
                }
            )
    }

    var messageText by remember(currentTemplate) { mutableStateOf(currentTemplate.messageText) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF37474F)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "قوالب رسائل واتساب",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Tabs: [قبل الموعد] [بعد العلاج] [تأكيد الحجز] [إلغاء الموعد]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WhatsAppTemplateCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ClinicPrimary else Color(0xFFECEFF1))
                            .clickable { selectedCategory = category }
                            .testTag("wa_tab_${category.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.label,
                            color = if (isSelected) Color.White else Color(0xFF455A64),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Template Editor Card matching screenshot
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نص القالب (${selectedCategory.label})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78909C)
                        )
                        IconButton(
                            onClick = { messageText = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("wa_template_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ClinicPrimary,
                            unfocusedBorderColor = Color(0xFFCFD8DC),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Dynamic Variables Section matching screenshot
            Text(
                text = "متغيرات الرسالة:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val variables = listOf(
                    "{اسم المريض}" to "المريض",
                    "{تاريخ الموعد}" to "التاريخ",
                    "{وقت الموعد}" to "الوقت",
                    "{اسم العيادة}" to "العيادة",
                    "{اسم الإجراء}" to "الإجراء"
                )
                variables.forEach { (variableCode, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2F1))
                            .clickable {
                                messageText = if (messageText.isBlank()) variableCode else "$messageText $variableCode"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = ClinicPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Template Button matching screenshot
            Button(
                onClick = {
                    onSaveTemplate(currentTemplate.copy(messageText = messageText))
                    Toast.makeText(context, "تم حفظ القالب بنجاح", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_wa_template_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
            ) {
                Text(
                    text = "حفظ القالب",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
