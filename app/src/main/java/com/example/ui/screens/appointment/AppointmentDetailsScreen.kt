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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientClassification
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
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
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit,
    onClassificationChange: (PatientClassification) -> Unit,
    onOpenClassificationGuide: () -> Unit,
    onSendOneHourReminder: () -> Unit,
    onSendPostTreatmentReminder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
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
                                    onSendPostTreatmentReminder()
                                } else {
                                    val time12 = TimeFormatter.format12Hour(appointment.startTime)
                                    val msg = "مرحباً ${appointment.patientName} 🌟، نذكرك بموعدك في عيادة الأسنان يوم $formattedDate الساعة $time12."
                                    ClinicActions.openWhatsApp(context, appointment.patientPhone, msg)
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

            // Tabs: [التفاصيل] [تاريخ المريض] [ملاحظات]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                listOf("التفاصيل", "تاريخ المريض", "ملاحظات").forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ClinicPrimary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) ClinicPrimary else Color(0xFF78909C),
                            fontSize = 13.sp,
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
                2 -> {
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
