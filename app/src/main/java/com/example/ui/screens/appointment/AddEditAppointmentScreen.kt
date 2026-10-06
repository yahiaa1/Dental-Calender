package com.example.ui.screens.appointment

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.utils.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAppointmentScreen(
    existingAppointment: AppointmentEntity?,
    initialDateStr: String,
    initialStartTime: String,
    initialEndTime: String,
    patientsList: List<PatientEntity>,
    proceduresList: List<ProcedureConfigEntity>,
    currentUser: UserEntity,
    prefillPatientId: Long? = null,
    prefillPatientName: String? = null,
    prefillPatientPhone: String? = null,
    prefillClassification: PatientClassification? = null,
    onSave: (AppointmentEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCancel() }
    val context = LocalContext.current

    var patientName by remember { mutableStateOf(existingAppointment?.patientName ?: prefillPatientName ?: "") }
    var patientPhone by remember { mutableStateOf(existingAppointment?.patientPhone ?: prefillPatientPhone ?: "") }
    var selectedPatientId by remember { mutableStateOf(existingAppointment?.patientId ?: prefillPatientId ?: 0L) }
    var patientClassification by remember {
        mutableStateOf(existingAppointment?.patientClassification ?: prefillClassification ?: PatientClassification.DISCIPLINED)
    }

    var selectedProcedure by remember {
        mutableStateOf(existingAppointment?.procedureName ?: (proceduresList.firstOrNull()?.name ?: "كشف"))
    }
    var dateText by remember { mutableStateOf(existingAppointment?.dateStr ?: initialDateStr) }
    var startTime by remember { mutableStateOf(existingAppointment?.startTime ?: initialStartTime) }
    var endTime by remember { mutableStateOf(existingAppointment?.endTime ?: initialEndTime) }
    var priceText by remember {
        mutableStateOf(
            existingAppointment?.price?.let { if (it > 0) it.toInt().toString() else "" } ?: ""
        )
    }
    var selectedStatus by remember {
        mutableStateOf(existingAppointment?.status ?: AppointmentStatus.SCHEDULED)
    }
    var notesText by remember { mutableStateOf(existingAppointment?.notes ?: "") }

    var procedureMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var patientSuggestionsExpanded by remember { mutableStateOf(false) }

    val matchedExistingPatient = remember(patientName, patientsList) {
        patientsList.firstOrNull {
            it.name.trim().equals(patientName.trim(), ignoreCase = true) ||
            (patientPhone.isNotBlank() && it.phone.trim() == patientPhone.trim())
        }
    }

    LaunchedEffect(matchedExistingPatient) {
        if (matchedExistingPatient != null) {
            patientClassification = matchedExistingPatient.classification
            if (patientPhone.isBlank()) {
                patientPhone = matchedExistingPatient.phone
            }
        }
    }

    val filteredPatients = remember(patientName, patientsList) {
        if (patientName.isBlank()) emptyList()
        else patientsList.filter { it.name.contains(patientName, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicBackground)
    ) {
        // Top Header
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
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp).testTag("cancel_appointment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إلغاء",
                        tint = Color(0xFF37474F)
                    )
                }

                Text(
                    text = if (existingAppointment != null) "تعديل الموعد" else "إضافة موعد جديد",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )

                Button(
                    onClick = {
                        if (patientName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم المريض", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (patientPhone.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة رقم الهاتف", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val priceVal = priceText.toDoubleOrNull() ?: 0.0
                        val appointment = AppointmentEntity(
                            id = existingAppointment?.id ?: 0L,
                            patientId = selectedPatientId,
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            procedureName = selectedProcedure,
                            chairNumber = 1, // Single chair clinic
                            dateStr = dateText,
                            startTime = startTime,
                            endTime = endTime,
                            price = priceVal,
                            status = selectedStatus,
                            patientClassification = patientClassification,
                            notes = notesText,
                            bookedByUserId = existingAppointment?.bookedByUserId ?: currentUser.id,
                            bookedByUserName = existingAppointment?.bookedByUserName ?: currentUser.name
                        )
                        onSave(appointment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_appointment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "حفظ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Form Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Patient Name field
            Text(text = "المريض *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            OutlinedTextField(
                value = patientName,
                onValueChange = {
                    patientName = it
                    patientSuggestionsExpanded = it.isNotBlank()
                },
                placeholder = { Text("البحث عن مريض أو كتابة اسم جديد", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ClinicPrimary)
                },
                trailingIcon = {
                    IconButton(onClick = { patientSuggestionsExpanded = !patientSuggestionsExpanded }) {
                        Icon(Icons.Default.Add, contentDescription = "اقتراحات", tint = ClinicPrimary)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("appointment_patient_name_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Patient suggestions dropdown
            if (patientSuggestionsExpanded && filteredPatients.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Column {
                        filteredPatients.take(4).forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        patientName = p.name
                                        patientPhone = p.phone
                                        selectedPatientId = p.id
                                        patientClassification = p.classification
                                        patientSuggestionsExpanded = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = p.phone, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Small patient behavior message alert from their file as requested:
            // "لما مريض يجي ياخد ميعاد في رسالة تيجي صغيرة تكون موجودة ف ملفة لما اختاره فيها تصنيف السلوك"
            if (patientName.isNotBlank()) {
                val alertStyle = when (patientClassification) {
                    PatientClassification.DISCIPLINED -> BehaviorAlertStyle(
                        Color(0xFFE8F5E9), Color(0xFF81C784), Icons.Default.CheckCircle, "منضبط ✅"
                    )
                    PatientClassification.OFTEN_LATE -> BehaviorAlertStyle(
                        Color(0xFFFFF3E0), Color(0xFFFFB74D), Icons.Default.Warning, "متأخر غالباً ⚠️"
                    )
                    PatientClassification.FREQUENT_ABSENT -> BehaviorAlertStyle(
                        Color(0xFFFFEBEE), Color(0xFFE57373), Icons.Default.ErrorOutline, "كثير الغياب 🚫"
                    )
                    PatientClassification.NEW -> BehaviorAlertStyle(
                        Color(0xFFECEFF1), Color(0xFFB0BEC5), Icons.Default.Person, "مريض جديد ℹ️"
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = alertStyle.bg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, alertStyle.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = alertStyle.icon,
                            contentDescription = null,
                            tint = alertStyle.border,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "رسالة في ملف المريض: سلوك ${alertStyle.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                            Text(
                                text = patientClassification.description,
                                fontSize = 11.sp,
                                color = Color(0xFF546E7A)
                            )
                        }
                    }
                }
            }

            // Phone Field
            Text(text = "رقم الهاتف *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            OutlinedTextField(
                value = patientPhone,
                onValueChange = { patientPhone = it },
                placeholder = { Text("010xxxxxxxx", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = ClinicPrimary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("appointment_patient_phone_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Patient Classification Selector (Punctual vs Late)
            Text(text = "تصنيف سلوك المريض *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PatientClassification.values().forEach { cl ->
                    val isSelected = cl == patientClassification
                    val (bgColor, textColor) = when (cl) {
                        PatientClassification.DISCIPLINED -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        PatientClassification.OFTEN_LATE -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                        PatientClassification.FREQUENT_ABSENT -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
                        PatientClassification.NEW -> Pair(Color(0xFFECEFF1), Color(0xFF455A64))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) textColor else bgColor)
                            .clickable { patientClassification = cl }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cl.title,
                            color = if (isSelected) Color.White else textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Procedure Dropdown
            Text(text = "نوع الإجراء *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedProcedure,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = ClinicPrimary)
                    },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("procedure_dropdown_field"),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFF263238),
                        disabledBorderColor = Color(0xFFB0BEC5),
                        disabledContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Box(modifier = Modifier.matchParentSize().clickable { procedureMenuExpanded = true })
                DropdownMenu(
                    expanded = procedureMenuExpanded,
                    onDismissRequest = { procedureMenuExpanded = false }
                ) {
                    val defaultProcedures = if (proceduresList.isNotEmpty()) {
                        proceduresList.map { it.name }
                    } else {
                        listOf("كشف", "حشو", "تنظيف الأسنان", "علاج عصب", "تركيبات", "جراحة", "متابعة")
                    }
                    defaultProcedures.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc, fontSize = 13.sp) },
                            onClick = {
                                selectedProcedure = proc
                                val found = proceduresList.firstOrNull { it.name == proc }
                                if (found != null && found.defaultPrice > 0) {
                                    priceText = found.defaultPrice.toInt().toString()
                                }
                                procedureMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Responsible staff info banner
            val bookingStaffName = existingAppointment?.bookedByUserName ?: currentUser.name
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE8F5E9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الموظف المسئول عن الحجز: $bookingStaffName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            // Interactive Date Picker Card
            Text(text = "تاريخ الموعد *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            val arabicDateLabel = remember(dateText) {
                try {
                    com.example.utils.DateUtils.formatArabicFullDate(com.example.utils.DateUtils.parseDate(dateText))
                } catch (e: Exception) {
                    dateText
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val initialDate = try {
                            java.time.LocalDate.parse(dateText)
                        } catch (e: Exception) {
                            java.time.LocalDate.now()
                        }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val picked = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                                dateText = com.example.utils.DateUtils.toDateString(picked)
                            },
                            initialDate.year,
                            initialDate.monthValue - 1,
                            initialDate.dayOfMonth
                        ).show()
                    }
                    .testTag("appointment_date_input"),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFD8DC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "اختيار التاريخ",
                            tint = ClinicPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = arabicDateLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                            Text(
                                text = "اضغط لتغيير التاريخ ($dateText)",
                                fontSize = 11.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = ClinicPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Interactive Start & End Time Pickers with Auto-Calculation
            Text(text = "توقيت الموعد (نظام 12 ساعة) *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Start Time Picker
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val (h, m) = try {
                                val parts = startTime.split(":")
                                Pair(parts[0].toInt(), parts[1].toInt())
                            } catch (e: Exception) {
                                Pair(10, 0)
                            }
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newStart = String.format("%02d:%02d", hourOfDay, minute)
                                    startTime = newStart
                                    // Automatically calculate end time (+45 minutes)
                                    val totalMins = hourOfDay * 60 + minute + 45
                                    val endH = (totalMins / 60) % 24
                                    val endM = totalMins % 60
                                    endTime = String.format("%02d:%02d", endH, endM)
                                },
                                h,
                                m,
                                false
                            ).show()
                        }
                        .testTag("start_time_input"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFD8DC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "من الساعة", fontSize = 11.sp, color = Color(0xFF78909C), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = ClinicPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TimeFormatter.format12Hour(startTime),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }

                // End Time Picker
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val (h, m) = try {
                                val parts = endTime.split(":")
                                Pair(parts[0].toInt(), parts[1].toInt())
                            } catch (e: Exception) {
                                Pair(10, 45)
                            }
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    endTime = String.format("%02d:%02d", hourOfDay, minute)
                                },
                                h,
                                m,
                                false
                            ).show()
                        }
                        .testTag("end_time_input"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFD8DC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "إلى الساعة (تلقائي)", fontSize = 11.sp, color = Color(0xFF78909C), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = ClinicPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TimeFormatter.format12Hour(endTime),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }
            }

            // Interactive Finger Slider for Time Selection
            val currentStartMinutes = remember(startTime) {
                try {
                    val parts = startTime.split(":")
                    parts[0].toInt() * 60 + parts[1].toInt()
                } catch (e: Exception) {
                    600 // 10:00
                }
            }
            val currentDurationMinutes = remember(startTime, endTime) {
                try {
                    val sParts = startTime.split(":")
                    val eParts = endTime.split(":")
                    val s = sParts[0].toInt() * 60 + sParts[1].toInt()
                    val e = eParts[0].toInt() * 60 + eParts[1].toInt()
                    if (e > s) e - s else 45
                } catch (e: Exception) {
                    45
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👆 اسحب بالإصبع لضبط وقت الموعد سريعاً",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClinicPrimary
                    )
                    Text(
                        text = "${TimeFormatter.format12Hour(startTime)} - ${TimeFormatter.format12Hour(endTime)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }

                Slider(
                    value = currentStartMinutes.toFloat().coerceIn(8f * 60f, 23f * 60f),
                    onValueChange = { newMinsFloat ->
                        val snapped = ((newMinsFloat / 15f).roundToInt() * 15).coerceIn(8 * 60, 23 * 60)
                        val h = snapped / 60
                        val m = snapped % 60
                        startTime = String.format("%02d:%02d", h, m)
                        val endTotal = (snapped + currentDurationMinutes).coerceAtMost(24 * 60 - 1)
                        endTime = String.format("%02d:%02d", endTotal / 60, endTotal % 60)
                    },
                    valueRange = (8f * 60f)..(23f * 60f),
                    steps = ((23 - 8) * 4) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = ClinicPrimary,
                        activeTrackColor = ClinicPrimary,
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.fillMaxWidth().height(28.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("08:00 ص", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    Text("01:00 م", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    Text("06:00 م", fontSize = 10.sp, color = Color(0xFF90A4AE))
                    Text("11:00 م", fontSize = 10.sp, color = Color(0xFF90A4AE))
                }
            }

            // Quick Duration Chips to adjust duration with single click
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "مدة الموعد:", fontSize = 10.sp, color = Color(0xFF78909C), fontWeight = FontWeight.Medium)
                listOf(15 to "15 د", 30 to "30 د", 45 to "45 د", 60 to "ساعة").forEach { (mins, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDE7F6))
                            .clickable {
                                try {
                                    val parts = startTime.split(":")
                                    val h = parts[0].toInt()
                                    val m = parts[1].toInt()
                                    val totalMins = h * 60 + m + mins
                                    val endH = (totalMins / 60) % 24
                                    val endM = totalMins % 60
                                    endTime = String.format("%02d:%02d", endH, endM)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = "+$label", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5E35B1))
                    }
                }
            }

            // Price Field
            Text(text = "السعر (جنيه)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                placeholder = { Text("450", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ClinicPrimary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("price_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Status Dropdown
            Text(text = "الحالة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedStatus.label,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = selectedStatus.toComposeColor())
                    },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFF263238),
                        disabledBorderColor = Color(0xFFB0BEC5),
                        disabledContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Box(modifier = Modifier.matchParentSize().clickable { statusMenuExpanded = true })
                DropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    AppointmentStatus.values().forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st.label, fontSize = 13.sp) },
                            onClick = {
                                selectedStatus = st
                                statusMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Notes
            Text(text = "ملاحظات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = { Text("ادخل ملاحظات ...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = ClinicPrimary)
                },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("appointment_notes_input"),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private data class BehaviorAlertStyle(
    val bg: Color,
    val border: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)
