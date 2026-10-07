package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentStatus
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: AppointmentStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        AppointmentStatus.UNCONFIRMED -> Triple(StatusUnconfirmedBg, StatusUnconfirmed, Icons.Default.HourglassEmpty)
        AppointmentStatus.CONFIRMED -> Triple(StatusConfirmedBg, StatusConfirmed, Icons.Default.CheckCircle)
        AppointmentStatus.IN_TREATMENT -> Triple(StatusInTreatmentBg, StatusInTreatment, Icons.Default.PlayCircleFilled)
        AppointmentStatus.COMPLETED -> Triple(StatusCompletedBg, StatusCompleted, Icons.Default.TaskAlt)
        AppointmentStatus.CANCELLED -> Triple(StatusCancelledBg, StatusCancelled, Icons.Default.Cancel)
        AppointmentStatus.NO_SHOW -> Triple(StatusNoShowBg, StatusNoShow, Icons.Default.PersonOff)
    }

    Row(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
