package com.example.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter
import java.time.LocalDate

enum class ReportScope(val label: String) {
    DAY("اليوم"),
    WEEK("الأسبوع"),
    MONTH("الشهر"),
    ALL("إجمالي الكل")
}

@Composable
fun ReportsScreen(
    appointments: List<AppointmentEntity>,
    patients: List<PatientEntity>,
    selectedReportDate: LocalDate,
    onSelectReportDate: (LocalDate) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedScope by remember { mutableStateOf(ReportScope.DAY) }

    // Direct real-time calculations from latest appointments state (strictly sorted chronologically):
    val filteredAppointments = remember(appointments, selectedScope, selectedReportDate) {
        val filtered = when (selectedScope) {
            ReportScope.DAY -> appointments.filter { DateUtils.isSameDay(it.dateStr, selectedReportDate) }
            ReportScope.WEEK -> appointments.filter { DateUtils.isSameWeek(it.dateStr, selectedReportDate) }
            ReportScope.MONTH -> appointments.filter { DateUtils.isSameMonth(it.dateStr, selectedReportDate) }
            ReportScope.ALL -> appointments
        }
        DateUtils.sortAppointmentsChronologically(filtered)
    }

    // Active/valid appointments contributing to plan and expected revenue (strictly excluding cancelled & no-show)
    val validActiveAppointments = filteredAppointments
        .filter { it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.NO_SHOW }

    // Expected target: Sum of prices of all non-cancelled & non-no-show appointments
    val expectedTarget = validActiveAppointments.sumOf { it.price }

    // Actual collected revenue for completed appointments
    val actualCollectedRevenue = filteredAppointments
        .filter { it.status == AppointmentStatus.COMPLETED }
        .sumOf { it.price }

    // Total value of all active bookings in this scope (خطة الإجمالي) - Cancelled appointments NOT included
    val totalBookingsValue = validActiveAppointments.sumOf { it.price }

    // Remaining / pending uncollected balance
    val remainingBalance = (expectedTarget - actualCollectedRevenue).coerceAtLeast(0.0)

    val targetAchievedPercentage = if (expectedTarget > 0) {
        ((actualCollectedRevenue / expectedTarget) * 100).toInt()
    } else 0

    val countCompleted = filteredAppointments.count { it.status == AppointmentStatus.COMPLETED }
    val countInTreatment = filteredAppointments.count { it.status == AppointmentStatus.IN_TREATMENT }
    val countConfirmed = filteredAppointments.count { it.status == AppointmentStatus.CONFIRMED }
    val countUnconfirmed = filteredAppointments.count { it.status == AppointmentStatus.UNCONFIRMED }
    val countCancelled = filteredAppointments.count { it.status == AppointmentStatus.CANCELLED }
    val countNoShow = filteredAppointments.count { it.status == AppointmentStatus.NO_SHOW }

    val periodLabel = when (selectedScope) {
        ReportScope.DAY -> DateUtils.formatArabicFullDate(selectedReportDate)
        ReportScope.WEEK -> {
            val weekDays = DateUtils.getWeekDays(selectedReportDate)
            if (weekDays.isNotEmpty()) {
                "${weekDays.first().dayOfMonth} ${DateUtils.getArabicMonthName(weekDays.first().monthValue)} - ${weekDays.last().dayOfMonth} ${DateUtils.getArabicMonthName(weekDays.last().monthValue)} ${weekDays.last().year}"
            } else {
                DateUtils.formatArabicMonthYear(selectedReportDate)
            }
        }
        ReportScope.MONTH -> DateUtils.formatArabicMonthYear(selectedReportDate)
        ReportScope.ALL -> "إجمالي كافة المواعيد بالعيادة (${appointments.size} موعد)"
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "رجوع",
                                tint = Color(0xFF37474F)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column {
                        Text(
                            text = "التقارير والإيرادات",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Text(
                            text = "متابعة التارجت اليومي والمحصل الفعلي",
                            fontSize = 11.sp,
                            color = Color(0xFF78909C)
                        )
                    }
                }

                // Share Report Button
                IconButton(
                    onClick = {
                        val reportText = buildString {
                            appendLine("📊 تقرير إيرادات وتارجت عيادة الأسنان")
                            appendLine("الفترة: $periodLabel [نطاق: ${selectedScope.label}]")
                            appendLine("-----------------------------")
                            appendLine("• المحصل الفعلي: ${actualCollectedRevenue.toInt()} ج.م")
                            appendLine("• التارجت المتوقع: ${expectedTarget.toInt()} ج.م")
                            appendLine("• نسبة الإنجاز: $targetAchievedPercentage%")
                            appendLine("• خطة إجمالي الحجوزات السارية: ${totalBookingsValue.toInt()} ج.م")
                            appendLine("• المتبقي المعلق: ${remainingBalance.toInt()} ج.م")
                            appendLine("• إجمالي عدد المواعيد: ${filteredAppointments.size} (السارية: ${validActiveAppointments.size} | الملغية: $countCancelled)")
                            appendLine("• مؤكد: $countConfirmed | جار العلاج: $countInTreatment | مكتمل: $countCompleted")
                            appendLine("• لم يتم التاكيد: $countUnconfirmed | ملغي: $countCancelled | لم يحضر: $countNoShow")
                            appendLine("-----------------------------")
                            appendLine("تفاصيل المواعيد:")
                            filteredAppointments.forEach {
                                val statusNotice = if (it.status == AppointmentStatus.CANCELLED || it.status == AppointmentStatus.NO_SHOW) " [غير محسوب بالخطة]" else ""
                                appendLine("- ${it.dateStr} [${it.startTime}]: ${it.patientName} (${it.procedureName}) - ${it.price.toInt()} ج.م [${it.status.label}]$statusNotice")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة التقرير المالي"))
                    },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE0F2F1))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = ClinicPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Scope Switcher Tabs: [ اليوم ] [ الأسبوع ] [ الشهر ] [ إجمالي الكل ]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F4F8))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReportScope.values().forEach { scope ->
                            val isSelected = selectedScope == scope
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ClinicPrimary else Color.Transparent)
                                    .clickable { selectedScope = scope }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = scope.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF546E7A)
                                )
                            }
                        }
                    }

                    // Scope-Specific Date Navigator
                    when (selectedScope) {
                        ReportScope.DAY -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF9FAFB))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.minusDays(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "اليوم السابق")
                                }

                                Text(
                                    text = DateUtils.formatArabicFullDate(selectedReportDate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClinicPrimary
                                )

                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.plusDays(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "اليوم التالي")
                                }
                            }

                            // Quick Jump Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickDatePill("اليوم", selectedReportDate == LocalDate.now()) {
                                    onSelectReportDate(LocalDate.now())
                                }
                                QuickDatePill("غداً", selectedReportDate == LocalDate.now().plusDays(1)) {
                                    onSelectReportDate(LocalDate.now().plusDays(1))
                                }
                                QuickDatePill("أمس", selectedReportDate == LocalDate.now().minusDays(1)) {
                                    onSelectReportDate(LocalDate.now().minusDays(1))
                                }
                            }
                        }

                        ReportScope.WEEK -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF9FAFB))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.minusWeeks(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "الأسبوع السابق")
                                }

                                Text(
                                    text = periodLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClinicPrimary
                                )

                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.plusWeeks(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "الأسبوع التالي")
                                }
                            }
                        }

                        ReportScope.MONTH -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF9FAFB))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.minusMonths(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "الشهر السابق")
                                }

                                Text(
                                    text = DateUtils.formatArabicMonthYear(selectedReportDate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ClinicPrimary
                                )

                                IconButton(
                                    onClick = { onSelectReportDate(selectedReportDate.plusMonths(1)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "الشهر التالي")
                                }
                            }
                        }

                        ReportScope.ALL -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "عرض كافة بيانات ومداخيل العيادة الشاملة",
                                    fontSize = 12.sp,
                                    color = Color(0xFF546E7A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // MAIN TARGET & REVENUE CARD (Prominent Gold & Emerald Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF004D40)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التارجت والمحصل (${selectedScope.label})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF00695C))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${filteredAppointments.size} مواعيد",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Main Big Numbers: Actual Collected & Target
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "المحصل الفعلي",
                                fontSize = 12.sp,
                                color = Color(0xFFB2DFDB)
                            )
                            Text(
                                text = "${actualCollectedRevenue.toInt()} ج.م",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "التارجت المتوقع",
                                fontSize = 12.sp,
                                color = Color(0xFFB2DFDB)
                            )
                            Text(
                                text = "${expectedTarget.toInt()} ج.م",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = periodLabel,
                        fontSize = 11.sp,
                        color = Color(0xFF80CBC4)
                    )

                    HorizontalDivider(color = Color(0xFF00796B), thickness = 1.dp)

                    // Target Achievement Percentage & Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسبة الإنجاز المحققة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "$targetAchievedPercentage%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (expectedTarget > 0) {
                                (actualCollectedRevenue.toFloat() / expectedTarget.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD54F),
                        trackColor = Color(0xFF00332C)
                    )
                }
            }

            // FINANCIAL SUMMARY ROW: Total Bookings Value & Remaining Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Bookings Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = ClinicPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "خطة إجمالي الحجوزات", fontSize = 11.sp, color = Color(0xFF546E7A))
                        }
                        Text(
                            text = "${totalBookingsValue.toInt()} ج.م",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Text(
                            text = "المواعيد السارية (الملغي مستبعد)",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                // Remaining Balance Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "المتبقي المعلق", fontSize = 11.sp, color = Color(0xFF546E7A))
                        }
                        Text(
                            text = "${remainingBalance.toInt()} ج.م",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "تارجت لم يُحصل بعد",
                            fontSize = 10.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
            }

            // STATUS BREAKDOWN MINI CARDS
            Text(
                text = "توزيع المواعيد حسب الحالة (${filteredAppointments.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniCard("مؤكد", "$countConfirmed", StatusConfirmed, Modifier.weight(1f))
                    StatusMiniCard("جار العلاج", "$countInTreatment", StatusInTreatment, Modifier.weight(1f))
                    StatusMiniCard("مكتمل", "$countCompleted", StatusCompleted, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniCard("لم يتم التاكيد", "$countUnconfirmed", StatusUnconfirmed, Modifier.weight(1f))
                    StatusMiniCard("ملغي", "$countCancelled", StatusCancelled, Modifier.weight(1f))
                    StatusMiniCard("لم يحضر", "$countNoShow", StatusNoShow, Modifier.weight(1f))
                }
            }

            // APPOINTMENTS LIST FOR THIS SCOPE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفاصيل المواعيد (${validActiveAppointments.size} سارية / ${filteredAppointments.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )

                        Text(
                            text = "خطة الإجمالي: ${totalBookingsValue.toInt()} ج.م",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClinicPrimary
                        )
                    }

                    if (filteredAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد مواعيد مجدولة في هذه الفترة المحددة.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        filteredAppointments.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFAFAFA))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.patientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF263238)
                                    )
                                    Text(
                                        text = "${app.procedureName} • ${TimeFormatter.formatTimeRange12(app.startTime, app.endTime)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF78909C)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "📅 ${app.dateStr}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF90A4AE)
                                        )
                                        Text(
                                            text = "👤 حجز: ${app.bookedByUserName}",
                                            fontSize = 10.sp,
                                            color = ClinicPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    val isCancelledOrNoShow = app.status == AppointmentStatus.CANCELLED || app.status == AppointmentStatus.NO_SHOW
                                    Text(
                                        text = "${app.price.toInt()} ج.م",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isCancelledOrNoShow -> Color(0xFFB0BEC5)
                                            app.status == AppointmentStatus.COMPLETED -> Color(0xFF2E7D32)
                                            else -> ClinicPrimary
                                        },
                                        style = if (isCancelledOrNoShow) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
                                    )
                                    if (isCancelledOrNoShow) {
                                        Text(
                                            text = "(غير محسوب)",
                                            fontSize = 9.sp,
                                            color = Color(0xFFE53935),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    StatusBadge(status = app.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMiniCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 11.sp, color = Color(0xFF546E7A))
        }
    }
}

@Composable
private fun QuickDatePill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ClinicPrimary else Color(0xFFECEFF1))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF455A64)
        )
    }
}
