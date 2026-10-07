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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun ReportsScreen(
    appointments: List<AppointmentEntity>,
    patients: List<PatientEntity>,
    selectedReportDate: LocalDate,
    onSelectReportDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateStr = DateUtils.toDateString(selectedReportDate)
    val dayAppointments = remember(appointments, dateStr) {
        appointments.filter { it.dateStr == dateStr }
    }

    // Expected daily target: Sum of prices of all non-cancelled/non-absent appointments for that day as requested!
    val expectedDailyTarget = remember(dayAppointments) {
        dayAppointments.filter { it.status != AppointmentStatus.NO_SHOW && it.status != AppointmentStatus.CANCELLED }.sumOf { it.price }
    }
    // Actual collected revenue for completed appointments
    val actualCollectedRevenue = remember(dayAppointments) {
        dayAppointments.filter { it.status == AppointmentStatus.COMPLETED }.sumOf { it.price }
    }
    val targetAchievedPercentage = remember(expectedDailyTarget, actualCollectedRevenue) {
        if (expectedDailyTarget > 0) ((actualCollectedRevenue / expectedDailyTarget) * 100).toInt() else 0
    }

    val dayCompleted = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.COMPLETED }
    }
    val dayInTreatment = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.IN_TREATMENT }
    }
    val dayConfirmed = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.CONFIRMED }
    }
    val dayUnconfirmed = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.UNCONFIRMED }
    }
    val dayCancelled = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.CANCELLED }
    }
    val dayNoShow = remember(dayAppointments) {
        dayAppointments.count { it.status == AppointmentStatus.NO_SHOW }
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
                Text(
                    text = "تقرير التارجت اليومي والمواعيد",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )

                // Share Report Button
                IconButton(
                    onClick = {
                        val reportText = buildString {
                            appendLine("🎯 تقرير تارجت عيادة الأسنان ليوم: ${DateUtils.formatArabicFullDate(selectedReportDate)}")
                            appendLine("-----------------------------")
                            appendLine("• التارجت المتوقع: ${expectedDailyTarget.toInt()} ج.م")
                            appendLine("• المحصل الفعلي: ${actualCollectedRevenue.toInt()} ج.م (نسبة الإنجاز: $targetAchievedPercentage%)")
                            appendLine("• إجمالي مواعيد اليوم: ${dayAppointments.size}")
                            appendLine("• مؤكد: $dayConfirmed | جار العلاج: $dayInTreatment | مكتمل: $dayCompleted | لم يتم التاكيد: $dayUnconfirmed | ملغي: $dayCancelled | لم يحضر: $dayNoShow")
                            appendLine("-----------------------------")
                            appendLine("تفاصيل المواعيد:")
                            dayAppointments.forEach {
                                appendLine("- ${it.startTime}: ${it.patientName} (${it.procedureName}) - ${it.price.toInt()} ج.م [${it.status.label}]")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير التارجت"))
                    },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0F2F1))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Day Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اختر اليوم لعرض التارجت والمواعيد:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF455A64)
                    )

                    // Date Navigator: < التاريخ >
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F7F8))
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
            }

            // PROMINENT TARGET CARD as requested:
            // "في التقارير عاوز تقرير بالاجمالي المتوقع مش مهم ابدا الاجمالي كام اهم حاجة اعرف التارجت في اليوم ده"
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
                                text = "التارجت اليومي المتوقع",
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
                                text = "${dayAppointments.size} مواعيد",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Huge expected target number
                    Text(
                        text = "${expectedDailyTarget.toInt()} ج.م",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F)
                    )

                    Text(
                        text = "إجمالي قيمة حجوزات اليوم (${DateUtils.getArabicDayName(selectedReportDate.dayOfWeek)})",
                        fontSize = 12.sp,
                        color = Color(0xFFB2DFDB)
                    )

                    HorizontalDivider(color = Color(0xFF00796B), thickness = 1.dp)

                    // Target achievement progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المحصل الفعلي: ${actualCollectedRevenue.toInt()} ج.م",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$targetAchievedPercentage% إنجاز",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (expectedDailyTarget > 0) (actualCollectedRevenue.toFloat() / expectedDailyTarget.toFloat()).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD54F),
                        trackColor = Color(0xFF00332C)
                    )
                }
            }

            // Status Breakdown for this Day
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniCard("مؤكد", "$dayConfirmed", StatusConfirmed, Modifier.weight(1f))
                    StatusMiniCard("جار العلاج", "$dayInTreatment", StatusInTreatment, Modifier.weight(1f))
                    StatusMiniCard("مكتمل", "$dayCompleted", StatusCompleted, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniCard("لم يتم التاكيد", "$dayUnconfirmed", StatusUnconfirmed, Modifier.weight(1f))
                    StatusMiniCard("ملغي", "$dayCancelled", StatusCancelled, Modifier.weight(1f))
                    StatusMiniCard("لم يحضر", "$dayNoShow", StatusNoShow, Modifier.weight(1f))
                }
            }

            // Appointments List contributing to this target
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
                    Text(
                        text = "مواعيد اليوم ومساهمتها بالتارجت (${dayAppointments.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )

                    if (dayAppointments.isEmpty()) {
                        Text(
                            text = "لا توجد مواعيد مجدولة في هذا اليوم.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        dayAppointments.forEach { app ->
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
                                    Text(text = app.patientName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${app.procedureName} • ${TimeFormatter.formatTimeRange12(app.startTime, app.endTime)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF78909C)
                                    )
                                    Text(
                                        text = "👤 حجز: ${app.bookedByUserName}",
                                        fontSize = 10.sp,
                                        color = ClinicPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${app.price.toInt()} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClinicPrimary)
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
