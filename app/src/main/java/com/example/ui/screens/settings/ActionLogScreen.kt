package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionLogEntity
import com.example.data.model.ActionLogType
import com.example.data.model.UserRole
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionLogScreen(
    logs: List<ActionLogEntity>,
    currentUserRole: UserRole,
    onClearLogs: () -> Unit,
    onDeleteLog: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<ActionLogType?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Filtered logs
    val filteredLogs = remember(logs, searchQuery, selectedFilterType) {
        logs.filter { log ->
            val matchType = selectedFilterType == null || log.actionType == selectedFilterType
            val matchQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                log.employeeName.contains(q, ignoreCase = true) ||
                        log.patientName.contains(q, ignoreCase = true) ||
                        log.procedureName.contains(q, ignoreCase = true) ||
                        log.details.contains(q, ignoreCase = true) ||
                        log.dateStr.contains(q)
            }
            matchType && matchQuery
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح سجل الحركات", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في مسح كافة الحركات المسجلة في السجل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLogs()
                        showClearDialog = false
                        Toast.makeText(context, "تم مسح سجل الحركات بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color(0xFF263238)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "سجل الحركات (Audit Log)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = ClinicPrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${filteredLogs.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClinicPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "متابعة حركة الموظفين والمواعيد",
                                fontSize = 11.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }

                    if (currentUserRole == UserRole.ADMIN && logs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السجل",
                                tint = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        },
        containerColor = ClinicBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 1.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("ابحث باسم الموظف، المريض، أو الإجراء...", fontSize = 13.sp, color = Color(0xFF90A4AE))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF78909C)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color(0xFF78909C))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "الكل" Chip
                FilterChip(
                    selected = selectedFilterType == null,
                    onClick = { selectedFilterType = null },
                    label = { Text("الكل (${logs.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClinicPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                ActionLogType.values().forEach { type ->
                    val typeCount = logs.count { it.actionType == type }
                    val isSelected = selectedFilterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedFilterType = if (isSelected) null else type
                        },
                        label = {
                            Text(
                                "${type.label} ($typeCount)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(type.toComposeColor())
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = type.toComposeColor(),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Logs List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = Color(0xFFECEFF1)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF90A4AE),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedFilterType != null)
                                "لا توجد حركات تطابق التصفية الحالية"
                            else
                                "سجل الحركات فارغ حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF455A64)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "أي حركة حجز أو تعديل أو نقل أو حذف موعد ستظهر هنا تلقائياً مع اسم الموظف.",
                            fontSize = 12.sp,
                            color = Color(0xFF90A4AE),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        ActionLogCard(
                            log = log,
                            canDelete = currentUserRole == UserRole.ADMIN,
                            onDelete = { onDeleteLog(log.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionLogCard(
    log: ActionLogEntity,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val actionColor = log.actionType.toComposeColor()
    val actionIcon = when (log.actionType) {
        ActionLogType.BOOKING -> Icons.Default.EventAvailable
        ActionLogType.MOVE -> Icons.Default.SwapHoriz
        ActionLogType.STATUS_CHANGE -> Icons.Default.PublishedWithChanges
        ActionLogType.EDIT -> Icons.Default.EditNote
        ActionLogType.DELETE -> Icons.Default.EventBusy
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Action Badge + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Action Badge
                Surface(
                    color = actionColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = log.actionType.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionColor
                        )
                    }
                }

                // Time & Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.dateStr}  ${log.timeStr}",
                        fontSize = 11.sp,
                        color = Color(0xFF78909C),
                        fontWeight = FontWeight.Medium
                    )
                    if (canDelete) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "حذف الحركة",
                                tint = Color(0xFFB0BEC5),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Employee info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = Color(0xFFE8EAF6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF3F51B5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.employeeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xFFECEFF1),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = log.employeeRole,
                        fontSize = 10.sp,
                        color = Color(0xFF546E7A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Narrative Details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8F9FA)
            ) {
                Text(
                    text = log.details,
                    fontSize = 12.5.sp,
                    color = Color(0xFF37474F),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Context: Patient & Appointment Details
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "المريض: ${log.patientName}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF455A64),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (log.procedureName.isNotBlank()) {
                    Surface(
                        color = ClinicPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.procedureName,
                            fontSize = 11.sp,
                            color = ClinicPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
