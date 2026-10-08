package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.ui.theme.WhatsAppGreen
import com.example.utils.DateUtils

@Composable
fun ClinicSettingsScreen(
    hideCancelled: Boolean,
    onToggleHideCancelled: () -> Unit,
    dayOff: java.time.DayOfWeek? = java.time.DayOfWeek.FRIDAY,
    hideDayOff: Boolean = false,
    onSelectDayOff: (java.time.DayOfWeek?) -> Unit = {},
    onToggleHideDayOff: () -> Unit = {},
    onOpenProcedureColors: () -> Unit,
    onOpenWhatsAppTemplates: () -> Unit,
    onOpenClassificationGuide: () -> Unit,
    onOpenNotionSync: () -> Unit,
    onOpenWorkingHours: () -> Unit,
    onOpenFirebaseSync: () -> Unit,
    onOpenUsersManagement: () -> Unit,
    onOpenWhatsAppReminders: () -> Unit,
    onClearAllAppointments: () -> Unit,
    onClearAllPatients: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("مسح جميع البيانات", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع المواعيد المسجلة والبدء بسجل فارغ تماماً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllAppointments()
                        onClearAllPatients()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "تم مسح جميع البيانات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color(0xFF37474F)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "الإعدادات والتخصيص",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User & Staff Management Card
            SettingsMenuCard(
                title = "مستخدمو العيادة والصلاحيات (Users & Roles)",
                subtitle = "تحديد الموظفين وصلاحيات الإلغاء والحذف للأدمن فقط",
                icon = Icons.Default.SupervisorAccount,
                iconTint = Color(0xFF1E88E5),
                onClick = onOpenUsersManagement,
                testTag = "settings_user_management"
            )

            // WhatsApp 1-Hour Reminder & API Card
            SettingsMenuCard(
                title = "خدمة التنبيه قبل الموعد بساعة (WhatsApp Reminders)",
                subtitle = "تنبيهات تلقائية في الخلفية والتحكم بنص الرسالة وربط API",
                icon = Icons.Default.AlarmOn,
                iconTint = WhatsAppGreen,
                onClick = onOpenWhatsAppReminders,
                testTag = "settings_wa_reminders"
            )

            // Notion Calendar
            SettingsMenuCard(
                title = "الربط مع Notion Calendar",
                subtitle = "تصدير ومزامنة مواعيد العيادة مع تطبيق نوشن كالندر",
                icon = Icons.Default.SyncAlt,
                iconTint = Color(0xFF5E35B1),
                onClick = onOpenNotionSync,
                testTag = "settings_notion_sync"
            )

            // Firebase Cloud Sync
            SettingsMenuCard(
                title = "الربط والمزامنة مع Firebase Cloud",
                subtitle = "نسخ احتياطي واسترجاع سحابي فوري لمواعيد وبيانات المرضى",
                icon = Icons.Default.CloudSync,
                iconTint = Color(0xFFFFA000),
                onClick = onOpenFirebaseSync,
                testTag = "settings_firebase_sync"
            )

            // Working Hours
            SettingsMenuCard(
                title = "مواقيت العمل في التقويم",
                subtitle = "تخصيص ساعة البدء والانتهاء لعرض التقويم بدون فراغات",
                icon = Icons.Default.Schedule,
                iconTint = ClinicPrimary,
                onClick = onOpenWorkingHours,
                testTag = "settings_working_hours"
            )

            // Hide Cancelled Appointments Toggle Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إخفاء المواعيد الملغاة من التقويم",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تظل المواعيد محفوظة في ملف المريض وسجلاته ولكن تختفي من العرض الزمني",
                            fontSize = 11.sp,
                            color = Color(0xFF78909C)
                        )
                    }
                    Switch(
                        checked = hideCancelled,
                        onCheckedChange = { onToggleHideCancelled() },
                        colors = SwitchDefaults.colors(checkedThumbColor = ClinicPrimary, checkedTrackColor = ClinicPrimary.copy(alpha = 0.5f))
                    )
                }
            }

            // Weekly Day Off Settings Card (تحديد يوم الإجازة وإخفاؤه أو إظهاره)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "يوم الإجازة الأسبوعية للعيادة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Text(
                                    text = if (dayOff != null) "اليوم المحدد: ${DateUtils.getArabicDayName(dayOff)}" else "لا توجد إجازة أسبوعية",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C)
                                )
                            }
                        }

                        // Hide/Show Toggle Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hideDayOff) "مخفي" else "ظاهر",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hideDayOff) Color(0xFFE53935) else ClinicPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = hideDayOff,
                                onCheckedChange = { onToggleHideDayOff() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFE53935),
                                    checkedTrackColor = Color(0xFFFFCDD2)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "اختر يوم الإجازة الرسمية للعيادة (الأسبوع يبدأ من السبت حتى الجمعة):",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Day of week selection chips (Saturday to Friday)
                    val daysList = listOf(
                        Pair(java.time.DayOfWeek.SATURDAY, "السبت"),
                        Pair(java.time.DayOfWeek.SUNDAY, "الأحد"),
                        Pair(java.time.DayOfWeek.MONDAY, "الاثنين"),
                        Pair(java.time.DayOfWeek.TUESDAY, "الثلاثاء"),
                        Pair(java.time.DayOfWeek.WEDNESDAY, "الأربعاء"),
                        Pair(java.time.DayOfWeek.THURSDAY, "الخميس"),
                        Pair(java.time.DayOfWeek.FRIDAY, "الجمعة"),
                        Pair(null, "بدون إجازة")
                    )

                    daysList.chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowDays.forEach { (dow, name) ->
                                val isSelected = dow == dayOff
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectDayOff(dow) },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            text = name,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (dow == null) Color(0xFFECEFF1) else Color(0xFFFFEBEE),
                                        selectedLabelColor = if (dow == null) Color(0xFF37474F) else Color(0xFFC62828)
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (hideDayOff && dayOff != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 يتم إخفاء يوم ${DateUtils.getArabicDayName(dayOff)} حالياً من العرض الأسبوعي في التقويم.",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            // Procedure Color Coding
            SettingsMenuCard(
                title = "إعدادات الألوان وتخصيص الإجراءات",
                subtitle = "تخصيص الألوان لكل إجراء (كشف، حشو، جراحة...)",
                icon = Icons.Default.Palette,
                iconTint = Color(0xFFE91E63),
                onClick = onOpenProcedureColors,
                testTag = "settings_procedure_colors"
            )

            // WhatsApp Templates
            SettingsMenuCard(
                title = "قوالب رسائل واتساب",
                subtitle = "تعديل رسائل التذكير، المتابعة، والتأكيد مع المتغيرات",
                icon = Icons.AutoMirrored.Filled.Chat,
                iconTint = WhatsAppGreen,
                onClick = onOpenWhatsAppTemplates,
                testTag = "settings_wa_templates"
            )

            // Patient Classification Guide
            SettingsMenuCard(
                title = "تصنيف سلوك المرضى",
                subtitle = "دليل وشارات المريض (منضبط، متأخر غالباً، كثير الغياب، جديد)",
                icon = Icons.Default.VerifiedUser,
                iconTint = Color(0xFF1E88E5),
                onClick = onOpenClassificationGuide,
                testTag = "settings_patient_classification"
            )

            // Clear All Data
            SettingsMenuCard(
                title = "مسح جميع المواعيد والبيانات",
                subtitle = "حذف كافة المواعيد والبدء بسجل فارغ وجديد",
                icon = Icons.Default.DeleteSweep,
                iconTint = Color(0xFFD32F2F),
                onClick = { showClearConfirmDialog = true },
                testTag = "settings_clear_data"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Clinic Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "معلومات النظام والعيادة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    ClinicInfoRow("نظام العمل", "كرسي علاجي رئيسي موحد (Single Chair)")
                    ClinicInfoRow("طريقة العرض", "تقويم تفاعلي بنظام السحب (Drag to select) والزوم")
                    ClinicInfoRow("دعم المزامنة", "Notion Calendar & iCal Export")
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF78909C)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFB0BEC5),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ClinicInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF78909C))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF37474F))
    }
}
