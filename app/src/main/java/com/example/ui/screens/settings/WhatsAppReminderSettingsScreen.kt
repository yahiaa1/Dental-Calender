package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.WhatsAppGreen
import com.example.utils.ReminderNotificationHelper

@Composable
fun WhatsAppReminderSettingsScreen(
    currentTemplate: WhatsAppTemplateEntity?,
    onSaveTemplate: (WhatsAppTemplateEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var messageText by remember(currentTemplate) {
        mutableStateOf(
            currentTemplate?.messageText
                ?: "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}. نحن في انتظار زيارتك!"
        )
    }

    var whatsappApiUrl by remember { mutableStateOf("") }
    var enableAutoNotification by remember { mutableStateOf(true) }

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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تنبيهات الواتساب قبل الموعد بساعة",
                    fontSize = 17.sp,
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
            // Enable Reminder Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "خدمة التنبيه في الخلفية (قبل ساعة)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Text(
                            text = "إرسال إشعار للموظف لفتح واتساب أو إرسال الرسالة عبر API تلقائياً",
                            fontSize = 12.sp,
                            color = Color(0xFF78909C)
                        )
                    }
                    Switch(
                        checked = enableAutoNotification,
                        onCheckedChange = { enableAutoNotification = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ClinicPrimary)
                    )
                }
            }

            // Message Template Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "نص رسالة التذكير الافتراضية (Default Message)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "يمكنك تعديل الرسالة التي ستصل للمريض قبل موعده بساعة. استخدم المتغيرات التالية لتعبئتها ديناميكياً:",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A)
                    )

                    // Variable tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("{اسم المريض}", "{وقت الموعد}", "{تاريخ الموعد}").forEach { variable ->
                            SuggestionChip(
                                onClick = { messageText += " $variable" },
                                label = { Text(variable, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("reminder_template_input"),
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val template = currentTemplate?.copy(messageText = messageText)
                                ?: WhatsAppTemplateEntity(
                                    id = 0,
                                    category = WhatsAppTemplateCategory.ONE_HOUR_BEFORE,
                                    title = "تذكير قبل الموعد بساعة",
                                    messageText = messageText
                                )
                            onSaveTemplate(template)
                            Toast.makeText(context, "تم حفظ نص الرسالة الافتراضية بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ نص الرسالة الافتراضية", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // WhatsApp API Integration Webhook
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "ربط WhatsApp API / Webhook (اختياري)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "إذا كان لديك مزود خدمة واتساب سحابي (WhatsApp Cloud API / Twilio / Green API)، أدخل رابط الـ Webhook للإرسال الآلي:",
                        fontSize = 12.sp,
                        color = Color(0xFF78909C)
                    )

                    OutlinedTextField(
                        value = whatsappApiUrl,
                        onValueChange = { whatsappApiUrl = it },
                        placeholder = { Text("https://api.yourprovider.com/send", fontSize = 12.sp) },
                        label = { Text("رابط API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Test Notification Button
                    OutlinedButton(
                        onClick = {
                            ReminderNotificationHelper.showOneHourReminderNotification(
                                context = context,
                                appointmentId = 99999,
                                patientName = "أحمد محمد (تجربة)",
                                patientPhone = "01012345678",
                                timeFormatted12 = "03:00 م",
                                customMessage = messageText.replace("{اسم المريض}", "أحمد محمد").replace("{وقت الموعد}", "03:00 م")
                            )
                            Toast.makeText(context, "تم إرسال إشعار التذكير التجريبي لجهازك!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال إشعار تجريبي الآن للهاتف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
