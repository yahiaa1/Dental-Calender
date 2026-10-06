package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientClassification

@Composable
fun ClassificationBadge(
    classification: PatientClassification,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (classification) {
        PatientClassification.DISCIPLINED -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        PatientClassification.OFTEN_LATE -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        PatientClassification.FREQUENT_ABSENT -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        PatientClassification.NEW -> Pair(Color(0xFFECEFF1), Color(0xFF455A64))
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = classification.title,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
