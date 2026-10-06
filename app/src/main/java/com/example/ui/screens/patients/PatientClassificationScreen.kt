package com.example.ui.screens.patients

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PatientClassification
import com.example.ui.theme.ClinicBackground

@Composable
fun PatientClassificationScreen(
    currentClassification: PatientClassification,
    onSelectClassification: (PatientClassification) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    var selected by remember { mutableStateOf(currentClassification) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF37474F)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تصنيف المرضى",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. منضبط (Disciplined)
            ClassificationSelectCard(
                classification = PatientClassification.DISCIPLINED,
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF2E7D32),
                bgTint = Color(0xFFE8F5E9),
                isSelected = selected == PatientClassification.DISCIPLINED,
                onClick = {
                    selected = PatientClassification.DISCIPLINED
                    onSelectClassification(PatientClassification.DISCIPLINED)
                }
            )

            // 2. متأخر (Often late)
            ClassificationSelectCard(
                classification = PatientClassification.OFTEN_LATE,
                icon = Icons.Default.Schedule,
                iconColor = Color(0xFFF57F17),
                bgTint = Color(0xFFFFF8E1),
                isSelected = selected == PatientClassification.OFTEN_LATE,
                onClick = {
                    selected = PatientClassification.OFTEN_LATE
                    onSelectClassification(PatientClassification.OFTEN_LATE)
                }
            )

            // 3. كثير الغياب (Frequent Absentee)
            ClassificationSelectCard(
                classification = PatientClassification.FREQUENT_ABSENT,
                icon = Icons.Default.Cancel,
                iconColor = Color(0xFFD32F2F),
                bgTint = Color(0xFFFFEBEE),
                isSelected = selected == PatientClassification.FREQUENT_ABSENT,
                onClick = {
                    selected = PatientClassification.FREQUENT_ABSENT
                    onSelectClassification(PatientClassification.FREQUENT_ABSENT)
                }
            )

            // 4. جديد (New)
            ClassificationSelectCard(
                classification = PatientClassification.NEW,
                icon = Icons.Default.Person,
                iconColor = Color(0xFF455A64),
                bgTint = Color(0xFFECEFF1),
                isSelected = selected == PatientClassification.NEW,
                onClick = {
                    selected = PatientClassification.NEW
                    onSelectClassification(PatientClassification.NEW)
                }
            )
        }
    }
}

@Composable
private fun ClassificationSelectCard(
    classification: PatientClassification,
    icon: ImageVector,
    iconColor: Color,
    bgTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("classification_card_${classification.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgTint.copy(alpha = 0.5f)),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, iconColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = classification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = classification.description,
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A)
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = iconColor,
                    unselectedColor = Color(0xFFB0BEC5)
                )
            )
        }
    }
}
