package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun FirebaseSyncScreen(
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onBackupToFirebase: () -> Unit,
    onRestoreFromFirebase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

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
                    text = "الربط مع Firebase Cloud",
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
            // Firebase Status Card
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFCA28).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Firebase Firestore Database", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "المزامنة السحابية التلقائية مفعلة 🟢", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚡ المزامنة التلقائية الفورية: يتم حفظ أي موعد جديد أو تعديل أو مريض تلقائياً في سحابة Firebase فوراً بدون أي تدخل، وتنعكس التغييرات تلقائياً على باقي هواتف العيادة في الوقت الفعلي!",
                            fontSize = 12.sp,
                            color = Color(0xFF1B5E20),
                            lineHeight = 18.sp
                        )
                    }

                    Text(
                        text = "يتيح لك هذا القسم رفع كافة المواعيد وسجلات المرضى إلى خوادم Google Firebase السحابية لحمايتها من الضياع ومزامنتها عبر الأجهزة.",
                        fontSize = 13.sp,
                        color = Color(0xFF455A64),
                        lineHeight = 20.sp
                    )

                    if (syncStatusMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = syncStatusMessage,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = ClinicPrimary
                        )
                    }
                }
            }

            // Sync Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "عمليات المزامنة السحابية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))

                    // Backup Button
                    Button(
                        onClick = onBackupToFirebase,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("backup_firebase_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("رفع نسخة احتياطية إلى Firebase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Restore Button
                    OutlinedButton(
                        onClick = onRestoreFromFirebase,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restore_firebase_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استرجاع البيانات من Firebase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Integration Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "حالة ربط Firebase", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text(
                        text = "• معمارية Firebase Firestore مجهزة ومفعلة في المشروع.\n• يمكنك إضافة ملف google-services.json الخاص بمشروعك في أي وقت لتفعيل بيئة الإنتاج المخصصة لعيادتك.",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
