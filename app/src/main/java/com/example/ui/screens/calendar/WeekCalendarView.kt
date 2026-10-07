package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.WhatsAppButton
import com.example.ui.theme.*
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter
import java.time.LocalDate

@Composable
fun WeekCalendarView(
    selectedDate: LocalDate,
    appointments: List<AppointmentEntity>,
    onDateSelected: (LocalDate) -> Unit,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onWhatsAppClick: (AppointmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = DateUtils.getWeekDays(selectedDate)

    Column(modifier = modifier.fillMaxSize().background(ClinicBackground)) {
        // Month Selector Header: "< أغسطس 2026 >"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onDateSelected(selectedDate.minusMonths(1)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "الشهر السابق",
                    tint = Color(0xFF455A64)
                )
            }

            Text(
                text = DateUtils.formatArabicMonthYear(selectedDate),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )

            IconButton(
                onClick = { onDateSelected(selectedDate.plusMonths(1)) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "الشهر التالي",
                    tint = Color(0xFF455A64)
                )
            }
        }

        // Horizontal week days row: أحد 2, اثنين 3, ثلاثاء 4, أربعاء 5, خميس 6, جمعة 7, سبت 8
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                val dayShort = DateUtils.getArabicDayShortName(date.dayOfWeek)
                val dayNumber = date.dayOfMonth.toString()

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDateSelected(date) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayShort,
                        fontSize = 11.sp,
                        color = if (isSelected) ClinicPrimary else Color(0xFF78909C),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ClinicPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF263238)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE8ECEB), thickness = 1.dp)

        // List of appointments for that date
        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مواعيد مسجلة في هذا اليوم",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = appointments, key = { it.id }) { appointment ->
                    AppointmentCardItem(
                        appointment = appointment,
                        onClick = { onAppointmentClick(appointment) },
                        onWhatsAppClick = { onWhatsAppClick(appointment) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentCardItem(
    appointment: AppointmentEntity,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("week_appointment_card_${appointment.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time range on left side
            Column(
                modifier = Modifier.width(62.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isCancelledOrDelayed = appointment.status == AppointmentStatus.NO_SHOW || appointment.status == AppointmentStatus.CANCELLED
                val timeColor = if (isCancelledOrDelayed) {
                    StatusCancelled
                } else {
                    Color(0xFF37474F)
                }
                Text(
                    text = TimeFormatter.format12Hour(appointment.startTime),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = timeColor
                )
                Text(
                    text = TimeFormatter.format12Hour(appointment.endTime),
                    fontSize = 10.sp,
                    color = if (isCancelledOrDelayed) StatusCancelled else Color(0xFF78909C)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Patient Info & Procedure in center
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.patientName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${appointment.patientPhone} • 👤 ${appointment.bookedByUserName} [${appointment.status.label}]",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${appointment.procedureName} (${appointment.status.label})",
                    fontSize = 12.sp,
                    color = Color(0xFF00796B),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge
            StatusBadge(status = appointment.status)

            Spacer(modifier = Modifier.width(8.dp))

            // WhatsApp Button
            WhatsAppButton(
                onClick = onWhatsAppClick
            )
        }
    }
}
