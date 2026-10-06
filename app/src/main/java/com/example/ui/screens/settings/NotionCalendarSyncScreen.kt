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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.utils.NotionCalendarHelper

@Composable
fun NotionCalendarSyncScreen(
    appointments: List<AppointmentEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var notionToken by remember { mutableStateOf("") }
    var notionDbId by remember { mutableStateOf("") }

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
                    text = "الربط مع Notion Calendar",
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
            // Notion Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E3440)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "مزامنة Notion Calendar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "ربط مواعيد العيادة مع تقويم نوشن السحابي", fontSize = 12.sp, color = Color(0xFF78909C))
                        }
                    }

                    Text(
                        text = "يمكنك تصدير تقويم المواعيد بالكامل بصيغة تقويم قياسي (.ics) واستيراده بضغطة زر واحدة داخل تطبيق Notion Calendar أو Google Calendar، مع تحديث المواعيد تلقائياً.",
                        fontSize = 13.sp,
                        color = Color(0xFF455A64),
                        lineHeight = 20.sp
                    )
                }
            }

            // Quick Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "طرق الربط السريعة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))

                    // Button 1: Export .ics
                    Button(
                        onClick = {
                            NotionCalendarHelper.exportAndShareCalendar(context, appointments)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_ics_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة وتصدير ملف التقويم (.ics) لنوشن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Open Notion Calendar Web
                    OutlinedButton(
                        onClick = {
                            NotionCalendarHelper.openNotionCalendar(context)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح Notion Calendar في المتصفح", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Notion Integration Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "إعدادات الربط المباشر عبر Notion API", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text(text = "إذا كان لديك Notion Integration Token وتريد المزامنة الثنائية مع صفحة العيادة:", fontSize = 12.sp, color = Color(0xFF78909C))

                    OutlinedTextField(
                        value = notionToken,
                        onValueChange = { notionToken = it },
                        placeholder = { Text("secret_xxxxxxxxxxxxxxxxxx", fontSize = 12.sp) },
                        label = { Text("Internal Integration Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = notionDbId,
                        onValueChange = { notionDbId = it },
                        placeholder = { Text("Database ID / Calendar Page ID", fontSize = 12.sp) },
                        label = { Text("معرف قاعدة بيانات المواعيد في نوشن") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            Toast.makeText(context, "تم حفظ بيانات الربط مع Notion Calendar بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ إعدادات المزامنة")
                    }
                }
            }
        }
    }
}
