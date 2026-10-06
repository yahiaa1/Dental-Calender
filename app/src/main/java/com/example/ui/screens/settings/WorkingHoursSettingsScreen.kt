package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun WorkingHoursSettingsScreen(
    currentStartHour: Int,
    currentEndHour: Int,
    onSaveHours: (start: Int, end: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var startHour by remember { mutableStateOf(currentStartHour) }
    var endHour by remember { mutableStateOf(currentEndHour) }

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
                    text = "مواقيت العمل في التقويم",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ClinicPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "تحديد ساعات العمل اليومية للعيادة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "اختر بداية ونهاية ساعات العمل ليتم إظهارها في التقويم اليومي وشاشة الـ 4 أيام بدون فراغات غير مستخدمة:",
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A)
                    )

                    // Start Hour Slider / Selector
                    Column {
                        Text(text = "ساعة بدء العمل: ${String.format("%02d:00", startHour)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = startHour.toFloat(),
                            onValueChange = { startHour = it.toInt() },
                            valueRange = 6f..14f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = ClinicPrimary, activeTrackColor = ClinicPrimary)
                        )
                    }

                    // End Hour Slider / Selector
                    Column {
                        Text(text = "ساعة انتهاء العمل: ${String.format("%02d:00", endHour)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = endHour.toFloat(),
                            onValueChange = { endHour = it.toInt() },
                            valueRange = 15f..24f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = ClinicPrimary, activeTrackColor = ClinicPrimary)
                        )
                    }

                    Button(
                        onClick = {
                            if (startHour >= endHour) {
                                Toast.makeText(context, "يجب أن تكون بداية العمل قبل نهايته", Toast.LENGTH_SHORT).show()
                            } else {
                                onSaveHours(startHour, endHour)
                                Toast.makeText(context, "تم حفظ مواقيت العمل بنجاح", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ مواقيت العمل", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
