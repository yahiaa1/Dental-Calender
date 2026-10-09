package com.example.ui.screens.appointment

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientClassification
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WhatsAppTemplateCategory
import com.example.data.model.WhatsAppTemplateEntity
import com.example.ui.components.ClassificationBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.WhatsAppButton
import com.example.ui.theme.*
import com.example.utils.ClinicActions
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter

@Composable
fun AppointmentDetailsScreen(
    appointment: AppointmentEntity,
    currentUser: UserEntity,
    templates: List<WhatsAppTemplateEntity> = emptyList(),
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit,
    onClassificationChange: (PatientClassification) -> Unit,
    onOpenClassificationGuide: () -> Unit,
    onSendOneHourReminder: () -> Unit,
    onSendPostTreatmentReminder: () -> Unit = {},
    onSendWhatsAppTemplate: (WhatsAppTemplateCategory) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(0) }
    var showCancelOrDeleteDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPostTreatmentDialog by remember { mutableStateOf(false) }

    val formattedDate = remember(appointment.dateStr) {
        DateUtils.formatArabicFullDate(DateUtils.parseDate(appointment.dateStr))
    }

    // Permission check as requested:
    // "وال يسجل ميقدرش يلغي مواعيد التاني الا اليوزر الادمن ال هو بتاعي"
    val canCancelOrDelete = remember(currentUser, appointment) {
        currentUser.role == UserRole.ADMIN || appointment.bookedByUserId == currentUser.id
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("عفواً، لا تملك الصلاحية", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "لا يمكنك إلغاء أو حذف هذا الموعد لأنه مسجل بواسطة: '${appointment.bookedByUserName}'.\n\nإلغاء مواعيد الموظفين الآخرين متاح فقط لحساب الأدمن (د. محمد)."
                )
            },
            confirmButton = {
                Button(onClick = { showPermissionDeniedDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }

    if (showCancelOrDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showCancelOrDeleteDialog = false },
            title = { Text("إلغاء الموعد أو حذفه", fontWeight = FontWeight.Bold) },
            text = {
                Text("يمكنك تحويل الموعد إلى 'لم يحضر / ملغي' ليختفي من التقويم لكن يظل محفوظاً في سجل المريض، أو حذفه نهائياً.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelOrDeleteDialog = false
                        onStatusChange(AppointmentStatus.CANCELLED)
                        Toast.makeText(context, "تم إلغاء الموعد واستبعاده من التقويم وخطة الإيرادات", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("إلغاء وإخفاء من التقويم (حفظ بالسجل)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCancelOrDeleteDialog = false
                        onDelete()
                        Toast.makeText(context, "تم حذف الموعد نهائياً وخصم إيراده من التقارير", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حذف نهائي", color = Color(0xFFD32F2F))
                }
            }
        )
    }

    if (showPostTreatmentDialog) {
        AlertDialog(
            onDismissRequest = { showPostTreatmentDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(32.dp)) },
            title = { Text("اكتمال الموعد", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "اكتمل العلاج للمريض '${appointment.patientName}'.\n\nهل ترغب في إرسال رسالة (قالب بعد العلاج) عبر واتساب الآن أم إرسالها لاحقاً؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPostTreatmentDialog = false
                        onSendPostTreatmentReminder()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("إرسال الآن 💬", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPostTreatmentDialog = false }
                ) {
                    Text("إرسال لاحقاً", color = Color(0xFF546E7A), fontWeight = FontWeight.Bold)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF37474F)
                    )
                }

                Text(
                    text = "تفاصيل الموعد",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).testTag("edit_appointment_button")) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل الموعد",
                        tint = ClinicPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCFD8DC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appointment.patientName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = appointment.patientPhone,
                                fontSize = 13.sp,
                                color = Color(0xFF78909C)
                            )
                        }

                        IconButton(
                            onClick = { ClinicActions.makeCall(context, appointment.patientPhone) },
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF1F5F4))
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = ClinicPrimary, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        WhatsAppButton(
                            onClick = {
                                if (appointment.status == AppointmentStatus.COMPLETED) {
                                    onSendWhatsAppTemplate(WhatsAppTemplateCategory.AFTER_TREATMENT)
                                } else {
                                    onSendWhatsAppTemplate(WhatsAppTemplateCategory.BEFORE_APPOINTMENT)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Patient classification row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8F9FA))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "سلوك المريض:", fontSize = 12.sp, color = Color(0xFF546E7A))
                            Spacer(modifier = Modifier.width(6.dp))
                            ClassificationBadge(classification = appointment.patientClassification)
                        }

                        val isLate = appointment.patientClassification == PatientClassification.OFTEN_LATE
                        TextButton(
                            onClick = {
                                val newClassification = if (isLate) PatientClassification.DISCIPLINED else PatientClassification.OFTEN_LATE
                                onClassificationChange(newClassification)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isLate) "تحديد كـ منضبط ✅" else "تحديد كـ متأخر غالباً ⚠️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLate) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            // Quick 1-Hour Reminder Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "تنبيه قبل الموعد بساعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "إرسال تذكير واتساب للمريض قبل ساعته المحددة", fontSize = 11.sp, color = Color(0xFF388E3C))
                        }
                    }

                    Button(
                        onClick = onSendOneHourReminder,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("إرسال الآن 💬", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Post-Treatment WhatsApp Button (shown when completed to allow sending later)
            if (appointment.status == AppointmentStatus.COMPLETED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "رسالة بعد العلاج", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "إرسال رسالة متابعة بعد العلاج للمريض عبر واتساب", fontSize = 11.sp, color = Color(0xFF388E3C))
                            }
                        }

                        Button(
                            onClick = onSendPostTreatmentReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("إرسال الآن 💬", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tabs: [التفاصيل] [رسائل واتساب] [تاريخ المريض] [الملاحظات]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                listOf("التفاصيل", "رسائل واتساب", "تاريخ المريض", "الملاحظات").forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ClinicPrimary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) ClinicPrimary else Color(0xFF78909C),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailItemRow(Icons.Default.CalendarToday, "التاريخ", formattedDate)

                            // 12-Hour Time Format as requested: "المواعيد مش عاوزها بنظام 24 عاوزها بنظام 12"
                            DetailItemRow(
                                Icons.Default.Schedule,
                                "الوقت (نظام 12 ساعة)",
                                TimeFormatter.formatTimeRange12(appointment.startTime, appointment.endTime)
                            )

                            // Staff who booked this appointment as requested:
                            // "يظهر جنب كل مريض اسم الموظف ال حجز"
                            DetailItemRow(
                                Icons.Default.PersonOutline,
                                "الموظف المسؤول عن الحجز",
                                appointment.bookedByUserName
                            )

                            DetailItemRow(Icons.Default.MedicalServices, "نوع الإجراء", appointment.procedureName)
                            DetailItemRow(Icons.Default.AttachMoney, "السعر", "${appointment.price.toInt()} جنيه")

                            // Status row with badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "حالة الموعد", fontSize = 13.sp, color = Color(0xFF455A64))
                                }
                                StatusBadge(status = appointment.status)
                            }

                            // Quick change status chips
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "تغيير حالة الموعد مباشرة:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF78909C)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AppointmentStatus.values().forEach { st ->
                                        val isSelected = appointment.status == st
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) st.toComposeColor() else Color(0xFFECEFF1),
                                            modifier = Modifier.clickable { 
                                                onStatusChange(st)
                                                if (st == AppointmentStatus.COMPLETED && appointment.status != AppointmentStatus.COMPLETED) {
                                                    showPostTreatmentDialog = true
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = st.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(0xFF37474F),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Notes row
                            DetailItemRow(
                                Icons.Default.Notes,
                                "ملاحظات",
                                if (appointment.notes.isNotBlank()) appointment.notes else "لا توجد ملاحظات"
                            )
                        }
                    }
                }
                1 -> {
                    // WhatsApp Templates Tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "رسائل واتساب للمريض",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF263238)
                                    )
                                    Text(
                                        text = "إرسال رسائل باستخدام القوالب المحفوظة مع تعويض بيانات المريض تلقائياً.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF78909C)
                                    )
                                }
                            }
                        }

                        val templateCategories = listOf(
                            Triple(
                                WhatsAppTemplateCategory.CONFIRMATION,
                                Icons.Default.CheckCircle,
                                Color(0xFF2E7D32)
                            ),
                            Triple(
                                WhatsAppTemplateCategory.BEFORE_APPOINTMENT,
                                Icons.Default.NotificationsActive,
                                Color(0xFF0288D1)
                            ),
                            Triple(
                                WhatsAppTemplateCategory.ONE_HOUR_BEFORE,
                                Icons.Default.Schedule,
                                Color(0xFFE65100)
                            ),
                            Triple(
                                WhatsAppTemplateCategory.AFTER_TREATMENT,
                                Icons.Default.Favorite,
                                Color(0xFF7B1FA2)
                            ),
                            Triple(
                                WhatsAppTemplateCategory.CANCELLATION,
                                Icons.Default.Cancel,
                                Color(0xFFD32F2F)
                            )
                        )

                        templateCategories.forEach { (cat, icon, themeColor) ->
                            val formattedMsg = getFormattedWhatsAppTemplate(cat, templates, appointment)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(themeColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = themeColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = cat.label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF263238)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = themeColor.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "قالب مخصص",
                                                color = themeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF8F9FA))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = formattedMsg,
                                            fontSize = 12.sp,
                                            color = Color(0xFF37474F),
                                            lineHeight = 18.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(formattedMsg))
                                                Toast.makeText(context, "تم نسخ نص الرسالة", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(40.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ",
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF546E7A)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "نسخ", fontSize = 11.5.sp, color = Color(0xFF546E7A))
                                        }

                                        Button(
                                            onClick = { onSendWhatsAppTemplate(cat) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "إرسال عبر واتساب 💬",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "سجل المريض والزيارات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "جميع مواعيد المريض تظل محفوظة هنا في سجله الدائم حتى لو تم إلغاء الموعد أو إخفاؤه من التقويم الرئيسي.",
                                fontSize = 12.sp,
                                color = Color(0xFF546E7A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• موعد حالي: ${appointment.procedureName} بتاريخ $formattedDate الساعة ${TimeFormatter.format12Hour(appointment.startTime)} (${appointment.status.label})",
                                fontSize = 13.sp,
                                color = ClinicPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                3 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "الملاحظات الطبية:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (appointment.notes.isNotBlank()) appointment.notes else "لا توجد ملاحظات إضافية لهذا الموعد.",
                                fontSize = 13.sp,
                                color = Color(0xFF37474F)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFECEFF1))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "إرسال سريع عبر واتساب:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF546E7A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSendWhatsAppTemplate(WhatsAppTemplateCategory.CONFIRMATION) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("تأكيد الحجز ✅", fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { onSendWhatsAppTemplate(WhatsAppTemplateCategory.BEFORE_APPOINTMENT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("تذكير قبل الموعد 🌟", fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { onSendWhatsAppTemplate(WhatsAppTemplateCategory.CANCELLATION) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("إلغاء الموعد ⚠️", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Buttons with Role-based validation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Appointment Button (Validated by User Role!)
                OutlinedButton(
                    onClick = {
                        if (canCancelOrDelete) {
                            showCancelOrDeleteDialog = true
                        } else {
                            showPermissionDeniedDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إلغاء / حذف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.9f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFB0BEC5))),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF455A64))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تعديل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Toggle Treatment Status
                val isOngoing = appointment.status == AppointmentStatus.IN_TREATMENT
                Button(
                    onClick = {
                        val nextStatus = if (isOngoing) AppointmentStatus.COMPLETED else AppointmentStatus.IN_TREATMENT
                        onStatusChange(nextStatus)
                        if (nextStatus == AppointmentStatus.COMPLETED) {
                            showPostTreatmentDialog = true
                        }
                    },
                    modifier = Modifier.weight(1.1f).height(46.dp).testTag("toggle_treatment_status_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Icon(
                        imageVector = if (isOngoing) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOngoing) "اكتمال العلاج" else "بدء العلاج",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, color = Color(0xFF455A64))
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF263238))
    }
}

private fun getFormattedWhatsAppTemplate(
    category: WhatsAppTemplateCategory,
    templates: List<WhatsAppTemplateEntity>,
    appointment: AppointmentEntity
): String {
    val template = templates.firstOrNull { it.category == category }
    val defaultText = when (category) {
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
    val rawText = template?.messageText?.takeIf { it.isNotBlank() } ?: defaultText
    val formattedDate = DateUtils.formatArabicFullDate(DateUtils.parseDate(appointment.dateStr))
    val time12 = TimeFormatter.format12Hour(appointment.startTime)
    return rawText
        .replace("{اسم المريض}", appointment.patientName)
        .replace("{تاريخ الموعد}", formattedDate)
        .replace("{وقت الموعد}", time12)
        .replace("{اسم العيادة}", "عيادة الأسنان")
        .replace("{اسم الإجراء}", appointment.procedureName)
        .replace("{نوع الإجراء}", appointment.procedureName)
}
