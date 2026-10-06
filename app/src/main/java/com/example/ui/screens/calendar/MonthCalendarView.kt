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
    modifier: Modifier = Modifier
) {
    val daysGrid = DateUtils.getMonthDaysGrid(selectedDate, selectedDate)
    val dayNames = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

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

        // Days of week header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayNames.forEach { name ->
                Box(
                    modifier = Modifier.width(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78909C)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // Month Days Grid (7 columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
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
                    onClick = { onDateSelected(cell.date) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status Legend at bottom matching screenshot
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
                    LegendItem(color = StatusScheduled, label = "مجدول")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = StatusDelayed, label = "متأخر")
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(58.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
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
                    AppointmentStatus.COMPLETED -> StatusCompleted
                    AppointmentStatus.SCHEDULED -> StatusScheduled
                    AppointmentStatus.DELAYED -> StatusDelayed
                    AppointmentStatus.IN_TREATMENT -> StatusInTreatment
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
