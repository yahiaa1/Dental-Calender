package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcedureConfigEntity
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun ProcedureColorsScreen(
    procedures: List<ProcedureConfigEntity>,
    onAddProcedure: (ProcedureConfigEntity) -> Unit,
    onUpdateProcedure: (ProcedureConfigEntity) -> Unit,
    onDeleteProcedure: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProcedure by remember { mutableStateOf<ProcedureConfigEntity?>(null) }
    var deletingProcedure by remember { mutableStateOf<ProcedureConfigEntity?>(null) }

    var newName by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("30") }
    var newPrice by remember { mutableStateOf("300") }
    var newColorHex by remember { mutableStateOf("#1E88E5") }

    val richPalette = listOf(
        "#1E88E5", "#0D47A1", "#00ACC1", "#00838F",
        "#43A047", "#1B5E20", "#00897B", "#004D40",
        "#FB8C00", "#E65100", "#F4511E", "#BF360C",
        "#E53935", "#B71C1C", "#D81B60", "#880E4F",
        "#8E24AA", "#4A148C", "#5E35B1", "#311B92",
        "#3949AB", "#1A237E", "#546E7A", "#263238"
    )

    // Add Procedure Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة إجراء جديد وتخصيص لونه", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم الإجراء (مثل: تقويم، زراعة، تبييض)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDuration,
                            onValueChange = { newDuration = it },
                            label = { Text("المدة (دقيقة)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = { newPrice = it },
                            label = { Text("السعر الافتراضي") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("اختر لون الإجراء في التقويم:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    ColorPaletteGrid(
                        selectedColor = newColorHex,
                        palette = richPalette,
                        onSelectColor = { newColorHex = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            Toast.makeText(context, "يرجى كتابة اسم الإجراء", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onAddProcedure(
                            ProcedureConfigEntity(
                                id = 0,
                                name = newName.trim(),
                                colorHex = newColorHex,
                                defaultDurationMinutes = newDuration.toIntOrNull() ?: 30,
                                defaultPrice = newPrice.toDoubleOrNull() ?: 0.0
                            )
                        )
                        showAddDialog = false
                        newName = ""
                        Toast.makeText(context, "تمت إضافة الإجراء وتخصيص لونه بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Edit Color Dialog
    if (editingProcedure != null) {
        var editColorHex by remember(editingProcedure) { mutableStateOf(editingProcedure!!.colorHex) }
        var editName by remember(editingProcedure) { mutableStateOf(editingProcedure!!.name) }

        AlertDialog(
            onDismissRequest = { editingProcedure = null },
            title = { Text("تعديل لون: ${editingProcedure?.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم الإجراء") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر لوناً جديداً للتقويم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ColorPaletteGrid(
                        selectedColor = editColorHex,
                        palette = richPalette,
                        onSelectColor = { editColorHex = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingProcedure?.let { proc ->
                            onUpdateProcedure(proc.copy(name = editName.trim(), colorHex = editColorHex))
                        }
                        editingProcedure = null
                        Toast.makeText(context, "تم تحديث لون الإجراء بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProcedure = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete confirmation
    if (deletingProcedure != null) {
        AlertDialog(
            onDismissRequest = { deletingProcedure = null },
            title = { Text("حذف الإجراء", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف إجراء '${deletingProcedure?.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingProcedure?.let { onDeleteProcedure(it.id) }
                        deletingProcedure = null
                        Toast.makeText(context, "تم حذف الإجراء بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProcedure = null }) { Text("إلغاء") }
            }
        )
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color(0xFF37474F)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إدارة الإجراءات والألوان",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }

                // Add Procedure Button as requested: "امكانية اضافة اجراءات معينه وتخصيص لونها"
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إجراء جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = ClinicPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تظهر بطاقات المواعيد في التقويم بنفس لون الإجراء المخصص أدناه، ويمكنك إضافة أي إجراءات إضافية وتخصيص ألوانها بحرية.",
                            fontSize = 12.sp,
                            color = Color(0xFF546E7A)
                        )
                    }
                }
            }

            items(procedures, key = { it.id }) { procedure ->
                val procColor = try {
                    Color(android.graphics.Color.parseColor(procedure.colorHex))
                } catch (e: Exception) {
                    Color(0xFF2196F3)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(procColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = procedure.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238)
                                )
                                Text(
                                    text = "${procedure.defaultDurationMinutes} دقيقة • ${procedure.defaultPrice.toInt()} ج.م",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingProcedure = procedure },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ColorLens, contentDescription = "تعديل اللون", tint = ClinicPrimary, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { deletingProcedure = procedure },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الإجراء", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteGrid(
    selectedColor: String,
    palette: List<String>,
    onSelectColor: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        palette.chunked(6).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowColors.forEach { hex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                    val isSelected = hex.equals(selectedColor, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onSelectColor(hex) }
                            .then(
                                if (isSelected) Modifier.border(3.dp, Color.Black, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
