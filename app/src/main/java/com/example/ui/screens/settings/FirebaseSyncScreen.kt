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
import com.example.data.firebase.FirebaseConfig
import kotlinx.coroutines.launch
import com.example.data.firebase.FirebaseSyncManager
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun FirebaseSyncScreen(
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onBackupToFirebase: () -> Unit,
    onRestoreFromFirebase: () -> Unit,
    onTestConnection: ((onResult: (Boolean, String) -> Unit) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Firebase live configuration state
    var currentConfig by remember { mutableStateOf(FirebaseSyncManager.getConfig(context)) }
    var projectIdText by remember { mutableStateOf(currentConfig.projectId) }
    var apiKeyText by remember { mutableStateOf(currentConfig.apiKey) }
    var appIdText by remember { mutableStateOf(currentConfig.appId) }
    var storageBucketText by remember { mutableStateOf(currentConfig.storageBucket) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<Boolean?>(null) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicBackground)
    ) {
        // Top Header
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
                    text = "الربط الفعلي مع Firebase Cloud",
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
            // Live Status Card
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
                            Text(text = "Google Firebase Firestore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "المزامنة السحابية اللحظية متصلة 🟢",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
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
                            text = "⚡ المزامنة الفورية: عند إضافة أو تعديل أي حجز أو حالة موعد، تنعكس التغييرات فوراً في سحابة Firebase ويتم تحديثها لحظياً على أجهزة العيادة الأخرى.",
                            fontSize = 12.sp,
                            color = Color(0xFF1B5E20),
                            lineHeight = 18.sp
                        )
                    }

                    // Test Connection Button
                    Button(
                        onClick = {
                            isTestingConnection = true
                            testResultStatus = null
                            testResultMessage = "جارِ اختبار الاتصال وقراءة/كتابة مستند في Firebase Firestore..."
                            if (onTestConnection != null) {
                                onTestConnection { success, msg ->
                                    isTestingConnection = false
                                    testResultStatus = success
                                    testResultMessage = msg
                                }
                            } else {
                                scope.launch {
                                    val res = FirebaseSyncManager.testFirebaseConnection(context)
                                    isTestingConnection = false
                                    res.onSuccess {
                                        testResultStatus = true
                                        testResultMessage = it
                                    }.onFailure {
                                        testResultStatus = false
                                        testResultMessage = "فشل الاتصال: ${it.localizedMessage}"
                                    }
                                }
                            }
                        },
                        enabled = !isTestingConnection && !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("test_firebase_connection_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestingConnection) "جارِ الفحص..." else "⚡ فحص واختبار الاتصال الفعلي بـ Firebase الآن",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Test Result Feedback Banner
                    if (testResultMessage != null) {
                        val isSuccess = testResultStatus == true
                        val bg = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val textCol = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = textCol,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = testResultMessage ?: "",
                                    color = textCol,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (syncStatusMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2F1))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = syncStatusMessage,
                                color = Color(0xFF004D40),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isSyncing || isTestingConnection) {
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
                    Text(text = "عمليات المزامنة السحابية اليدوية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))

                    // Backup Button
                    Button(
                        onClick = onBackupToFirebase,
                        enabled = !isSyncing && !isTestingConnection,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("backup_firebase_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("رفع نسخة احتياطية كاملة إلى Firebase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Restore Button
                    OutlinedButton(
                        onClick = onRestoreFromFirebase,
                        enabled = !isSyncing && !isTestingConnection,
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("restore_firebase_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استرجاع البيانات من Firebase", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Actual Firebase Settings Card (Project ID, API Key, App ID)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إعدادات الربط الفعلي مع خوادم Firebase",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        IconButton(
                            onClick = {
                                val reset = FirebaseSyncManager.resetConfig(context)
                                currentConfig = reset
                                projectIdText = reset.projectId
                                apiKeyText = reset.apiKey
                                appIdText = reset.appId
                                storageBucketText = reset.storageBucket
                                Toast.makeText(context, "تمت استعادة الإعدادات الافتراضية", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "استعادة الافتراضي", tint = Color(0xFF00796B))
                        }
                    }

                    Text(
                        text = "يمكنك تعديل مفاتيح مشروع Firebase الخاص بك هنا أو استخدام الإعدادات الافتراضية المعتمدة.",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A)
                    )

                    // Project ID Field
                    OutlinedTextField(
                        value = projectIdText,
                        onValueChange = { projectIdText = it },
                        label = { Text("معرف المشروع (Project ID)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // API Key Field
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("مفتاح الـ API (API Key)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // App ID Field
                    OutlinedTextField(
                        value = appIdText,
                        onValueChange = { appIdText = it },
                        label = { Text("معرف التطبيق السحابي (Application ID)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Storage Bucket Field
                    OutlinedTextField(
                        value = storageBucketText,
                        onValueChange = { storageBucketText = it },
                        label = { Text("مستودع التخزين (Storage Bucket)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Save Config Button
                    Button(
                        onClick = {
                            val newConfig = FirebaseConfig(
                                projectId = projectIdText,
                                apiKey = apiKeyText,
                                appId = appIdText,
                                storageBucket = storageBucketText
                            )
                            FirebaseSyncManager.saveConfig(context, newConfig)
                            currentConfig = newConfig
                            Toast.makeText(context, "تم حفظ إعدادات Firebase وتطبيقها بنجاح", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ وتطبيق الإعدادات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
