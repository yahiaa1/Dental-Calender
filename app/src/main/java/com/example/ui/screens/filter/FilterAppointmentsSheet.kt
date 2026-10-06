package com.example.ui.screens.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientClassification
import com.example.data.model.ProcedureConfigEntity
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.ui.viewmodel.FilterState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAppointmentsSheet(
    initialFilter: FilterState,
    procedures: List<ProcedureConfigEntity>,
    onApply: (FilterState) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf(initialFilter.searchQuery) }
    var selectedDate by remember { mutableStateOf(initialFilter.date) }
    var selectedProcedure by remember { mutableStateOf(initialFilter.procedure) }
    var selectedStatus by remember { mutableStateOf(initialFilter.status) }
    var selectedClassification by remember { mutableStateOf(initialFilter.classification) }

    var procMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var classMenuExpanded by remember { mutableStateOf(false) }

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
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "إغلاق",
                        tint = Color(0xFF37474F)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "الفلاتر والفلترة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث عن مريض أو رقم هاتف ...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ClinicPrimary)
                },
                modifier = Modifier.fillMaxWidth().testTag("filter_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Date Filter
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ClinicPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "التاريخ", fontSize = 13.sp, color = Color(0xFF455A64))
                    }
                    Text(
                        text = selectedDate?.toString() ?: "كل التواريخ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }
            }

            // Procedure Dropdown Filter
            FilterDropdownItem(
                title = "نوع الإجراء",
                currentValue = selectedProcedure ?: "كل الإجراءات",
                expanded = procMenuExpanded,
                onExpandedChange = { procMenuExpanded = it }
            ) {
                DropdownMenuItem(
                    text = { Text("كل الإجراءات") },
                    onClick = {
                        selectedProcedure = null
                        procMenuExpanded = false
                    }
                )
                procedures.forEach { proc ->
                    DropdownMenuItem(
                        text = { Text(proc.name) },
                        onClick = {
                            selectedProcedure = proc.name
                            procMenuExpanded = false
                        }
                    )
                }
            }

            // Status Dropdown Filter
            FilterDropdownItem(
                title = "الحالة",
                currentValue = selectedStatus?.label ?: "كل الحالات",
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it }
            ) {
                DropdownMenuItem(
                    text = { Text("كل الحالات") },
                    onClick = {
                        selectedStatus = null
                        statusMenuExpanded = false
                    }
                )
                AppointmentStatus.values().forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st.label) },
                        onClick = {
                            selectedStatus = st
                            statusMenuExpanded = false
                        }
                    )
                }
            }

            // Patient Classification Filter
            FilterDropdownItem(
                title = "تصنيف سلوك المريض",
                currentValue = selectedClassification?.title ?: "كل التصنيفات",
                expanded = classMenuExpanded,
                onExpandedChange = { classMenuExpanded = it }
            ) {
                DropdownMenuItem(
                    text = { Text("كل التصنيفات") },
                    onClick = {
                        selectedClassification = null
                        classMenuExpanded = false
                    }
                )
                PatientClassification.values().forEach { cl ->
                    DropdownMenuItem(
                        text = { Text(cl.title) },
                        onClick = {
                            selectedClassification = cl
                            classMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Bottom Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        searchQuery = ""
                        selectedDate = null
                        selectedProcedure = null
                        selectedStatus = null
                        selectedClassification = null
                        onReset()
                    },
                    modifier = Modifier.weight(0.9f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(text = "مسح الكل", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        val filter = FilterState(
                            searchQuery = searchQuery,
                            date = selectedDate,
                            procedure = selectedProcedure,
                            status = selectedStatus,
                            classification = selectedClassification
                        )
                        onApply(filter)
                    },
                    modifier = Modifier.weight(1.3f).height(48.dp).testTag("apply_filters_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Text(text = "تطبيق الفلاتر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownItem(
    title: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 13.sp, color = Color(0xFF455A64))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currentValue, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content
        )
    }
}
