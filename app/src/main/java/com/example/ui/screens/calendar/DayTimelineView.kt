package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.ui.theme.*

@Composable
fun DayTimelineView(
    appointments: List<AppointmentEntity>,
    procedureColors: Map<String, String>,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onEmptySlotClick: (chair: Int, time: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = listOf(
        "09:00", "10:00", "11:00", "12:00", "13:00",
        "14:00", "15:00", "16:00", "17:00"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Chair Header row matching the screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer for the time column width
            Box(
                modifier = Modifier.width(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Chair 3 badge
            ChairHeaderChip(
                title = "الكرسي 3",
                bgColor = Chair3Bg,
                textColor = Chair3Text,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Chair 2 badge
            ChairHeaderChip(
                title = "الكرسي 2",
                bgColor = Chair2Bg,
                textColor = Chair2Text,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Chair 1 badge
            ChairHeaderChip(
                title = "الكرسي 1",
                bgColor = Chair1Bg,
                textColor = Chair1Text,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // Hour grid timeline with Zero-Lag LazyColumn and unique keys
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicBackground)
        ) {
            items(items = hours, key = { it }) { hour ->
                TimelineHourRow(
                    hour = hour,
                    appointments = appointments,
                    procedureColors = procedureColors,
                    onAppointmentClick = onAppointmentClick,
                    onEmptySlotClick = onEmptySlotClick
                )
            }
        }
    }
}

@Composable
private fun ChairHeaderChip(
    title: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimelineHourRow(
    hour: String,
    appointments: List<AppointmentEntity>,
    procedureColors: Map<String, String>,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onEmptySlotClick: (chair: Int, time: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hourPrefix = hour.substringBefore(":")

    Box(modifier = modifier.fillMaxWidth().height(72.dp)) {
        // Bottom divider line
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = Color(0xFFE8ECEB),
            thickness = 0.8.dp
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time label column
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxHeight()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = hour,
                    color = Color(0xFF78909C),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Vertical divider after time
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFFE8ECEB))
            )

            // Chair 3 slot (displayed in RTL order: 3, 2, 1)
            TimelineSlotCell(
                chair = 3,
                hourPrefix = hourPrefix,
                hour = hour,
                appointments = appointments,
                procedureColors = procedureColors,
                onAppointmentClick = onAppointmentClick,
                onEmptySlotClick = onEmptySlotClick,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFFE8ECEB))
            )

            // Chair 2 slot
            TimelineSlotCell(
                chair = 2,
                hourPrefix = hourPrefix,
                hour = hour,
                appointments = appointments,
                procedureColors = procedureColors,
                onAppointmentClick = onAppointmentClick,
                onEmptySlotClick = onEmptySlotClick,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFFE8ECEB))
            )

            // Chair 1 slot
            TimelineSlotCell(
                chair = 1,
                hourPrefix = hourPrefix,
                hour = hour,
                appointments = appointments,
                procedureColors = procedureColors,
                onAppointmentClick = onAppointmentClick,
                onEmptySlotClick = onEmptySlotClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimelineSlotCell(
    chair: Int,
    hourPrefix: String,
    hour: String,
    appointments: List<AppointmentEntity>,
    procedureColors: Map<String, String>,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onEmptySlotClick: (chair: Int, time: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find appointment that falls on this chair and matches the start hour
    val appointment = appointments.firstOrNull {
        it.chairNumber == chair && it.startTime.startsWith(hourPrefix)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (appointment != null) {
            // Find procedure custom color
            val colorHex = procedureColors[appointment.procedureName] ?: "#2196F3"
            val accentColor = try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color(0xFF2196F3)
            }
            val cardBg = accentColor.copy(alpha = 0.14f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(cardBg)
                    .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                    .clickable { onAppointmentClick(appointment) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("appointment_card_${appointment.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = appointment.patientName,
                    color = Color(0xFF263238),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = appointment.procedureName,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "👤 ${appointment.bookedByUserName}",
                    color = Color(0xFF546E7A),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Empty clickable slot
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onEmptySlotClick(chair, hour) }
            )
        }
    }
}
