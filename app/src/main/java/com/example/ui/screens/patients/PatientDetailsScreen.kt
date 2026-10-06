package com.example.ui.screens.patients

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.AppointmentEntity
import com.example.data.model.PatientClassification
import com.example.data.model.PatientEntity
import com.example.ui.components.ClassificationBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.WhatsAppButton
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.utils.ClinicActions
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter

@Composable
fun PatientDetailsScreen(
    patient: PatientEntity,
    patientAppointments: List<AppointmentEntity>,
    onBack: () -> Unit,
    onBookNewAppointment: (PatientEntity) -> Unit,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onUpdatePatient: (PatientEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember(patient) { mutableStateOf(patient.name) }
    var editPhone by remember(patient) { mutableStateOf(patient.phone) }
    var editNotes by remember(patient) { mutableStateOf(patient.notes) }
    var editClassification by remember(patient) { mutableStateOf(patient.classification) }

    // Edit Patient Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("تعديل بيانات المريض", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم المريض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("ملاحظات طبية") },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        maxLines = 3
                    )

                    Text("تصنيف سلوك المريض:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            PatientClassification.DISCIPLINED to "منضبط",
                            PatientClassification.OFTEN_LATE to "متأخر غالباً",
                            PatientClassification.FREQUENT_ABSENT to "كثير الغياب"
                        ).forEach { (cls, label) ->
                            val isSelected = editClassification == cls
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ClinicPrimary else Color(0xFFEEEEEE))
                                    .clickable { editClassification = cls }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF37474F)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال اسم المريض", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onUpdatePatient(
                            patient.copy(
                                name = editName.trim(),
                                phone = editPhone.trim(),
                                notes = editNotes.trim(),
                                classification = editClassification
                            )
                        )
                        showEditDialog = false
                        Toast.makeText(context, "تم حفظ بيانات المريض بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("إلغاء") }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color(0xFF37474F)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ملف وبيانات المريض",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(ClinicPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ClinicPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Patient Info Card
            item {
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
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCFD8DC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = patient.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (patient.phone.isNotBlank()) patient.phone else "لا يوجد رقم هاتف",
                                    fontSize = 13.sp,
                                    color = Color(0xFF78909C)
                                )
                            }

                            if (patient.phone.isNotBlank()) {
                                IconButton(
                                    onClick = { ClinicActions.makeCall(context, patient.phone) },
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFF1F5F4))
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = ClinicPrimary, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                WhatsAppButton(
                                    onClick = {
                                        val msg = "مرحباً ${patient.name} 🌟، نتواصل معك من عيادة الأسنان."
                                        ClinicActions.openWhatsApp(context, patient.phone, msg)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Classification & Behavior Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "سلوك المريض:", fontSize = 12.sp, color = Color(0xFF546E7A))
                                Spacer(modifier = Modifier.width(8.dp))
                                ClassificationBadge(classification = patient.classification)
                            }

                            val isLate = patient.classification == PatientClassification.OFTEN_LATE
                            TextButton(
                                onClick = {
                                    val next = if (isLate) PatientClassification.DISCIPLINED else PatientClassification.OFTEN_LATE
                                    onUpdatePatient(patient.copy(classification = next))
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isLate) "تحديد كـ منضبط ✅" else "تحديد كـ متأخر ⚠️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLate) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }

                        // Medical Notes
                        if (patient.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "ملاحظات طبية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = patient.notes,
                                fontSize = 12.sp,
                                color = Color(0xFF546E7A),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Appointments History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل المواعيد والزيارات (${patientAppointments.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }
            }

            // Appointments List
            if (patientAppointments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد مواعيد مسجلة لهذا المريض بعد.",
                                fontSize = 13.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                }
            } else {
                items(patientAppointments, key = { it.id }) { app ->
                    val formattedDate = DateUtils.formatArabicFullDate(DateUtils.parseDate(app.dateStr))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppointmentClick(app) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.procedureName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$formattedDate • ${TimeFormatter.formatTimeRange12(app.startTime, app.endTime)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C)
                                )
                                Text(
                                    text = "👤 حجز: ${app.bookedByUserName} • ${app.price.toInt()} ج.م",
                                    fontSize = 10.sp,
                                    color = ClinicPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            StatusBadge(status = app.status)
                        }
                    }
                }
            }
        }

        // Bottom Button to Book New Appointment for this Patient
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onBookNewAppointment(patient) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("book_patient_appointment_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حجز موعد جديد لهذا المريض",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
