package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.model.AppointmentStatus
import com.example.ui.theme.*
import com.example.utils.DateUtils
import java.time.LocalDate

@Composable
fun MonthCalendarView(
    selectedDate: LocalDate,
    appointmentDots: Map<String, List<AppointmentStatus>>,
    onDateSelected: (LocalDate) -> Unit,
    dayOff: java.time.DayOfWeek? = java.time.DayOfWeek.FRIDAY,
    hideDayOff: Boolean = false,
    modifier: Modifier = Modifier
) {
    val daysGrid = DateUtils.getMonthDaysGrid(selectedDate, selectedDate, hideDayOff, dayOff)
    val allDayNames = listOf(
        Pair("سبت", java.time.DayOfWeek.SATURDAY),
        Pair("أحد", java.time.DayOfWeek.SUNDAY),
        Pair("اثنين", java.time.DayOfWeek.MONDAY),
        Pair("ثلاثاء", java.time.DayOfWeek.TUESDAY),
        Pair("أربعاء", java.time.DayOfWeek.WEDNESDAY),
        Pair("خميس", java.time.DayOfWeek.THURSDAY),
        Pair("جمعة", java.time.DayOfWeek.FRIDAY)
    )
    val dayNames = if (hideDayOff && dayOff != null) {
        allDayNames.filter { it.second != dayOff }
    } else {
        allDayNames
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicBackground)
    ) {
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
                fontSize = 16.sp,
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

        // Days of week header row (e.g. Saturday -> Thursday if Friday is hidden)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayNames.forEach { (name, dow) ->
                val isDayOff = dow == dayOff
                Box(
                    modifier = Modifier.width(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = if (isDayOff) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isDayOff) Color(0xFFE53935) else Color(0xFF78909C)
                        )
                        if (isDayOff) {
                            Text(
                                text = "إجازة",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // Month Days Grid (6 or 7 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(dayNames.size),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(daysGrid) { cell ->
                val dateStr = DateUtils.toDateString(cell.date)
                val statuses = appointmentDots[dateStr] ?: emptyList()

                MonthDayCell(
                    cell = cell,
                    statuses = statuses,
                    isDayOff = cell.date.dayOfWeek == dayOff,
                    onClick = { onDateSelected(cell.date) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status Legend at bottom matching 6 statuses
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = StatusCompleted, label = "مكتمل")
                    LegendItem(color = StatusInTreatment, label = "جار العلاج")
                    LegendItem(color = StatusConfirmed, label = "مؤكد")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = StatusUnconfirmed, label = "لم يتم التأكيد")
                    LegendItem(color = StatusCancelled, label = "ملغي")
                    LegendItem(color = StatusNoShow, label = "لم يحضر")
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    cell: DateUtils.MonthCalendarDay,
    statuses: List<AppointmentStatus>,
    isDayOff: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(58.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDayOff && !cell.isSelected && cell.isCurrentMonth) Color(0xFFFFF8F7) else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag("month_day_${cell.date}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (cell.isSelected) ClinicPrimary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            val textColor = when {
                cell.isSelected -> Color.White
                !cell.isCurrentMonth -> Color(0xFFCFD8DC)
                isDayOff -> Color(0xFFD32F2F)
                else -> Color(0xFF263238)
            }
            Text(
                text = cell.date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (cell.isSelected || cell.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }

        // Colored dots for appointments on that day
        Row(
            modifier = Modifier
                .height(8.dp)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val distinctStatuses = statuses.distinct().take(3)
            distinctStatuses.forEach { status ->
                val dotColor = when (status) {
                    AppointmentStatus.UNCONFIRMED -> StatusUnconfirmed
                    AppointmentStatus.CONFIRMED -> StatusConfirmed
                    AppointmentStatus.IN_TREATMENT -> StatusInTreatment
                    AppointmentStatus.COMPLETED -> StatusCompleted
                    AppointmentStatus.CANCELLED -> StatusCancelled
                    AppointmentStatus.NO_SHOW -> StatusNoShow
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF455A64),
            fontWeight = FontWeight.Medium
        )
    }
}
