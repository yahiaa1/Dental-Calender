package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary

@Composable
fun UserManagementScreen(
    currentUser: UserEntity,
    allUsers: List<UserEntity>,
    onSelectCurrentUser: (UserEntity) -> Unit,
    onAddUser: (UserEntity) -> Unit,
    onUpdateUser: (UserEntity) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var deletingUser by remember { mutableStateOf<UserEntity?>(null) }

    var newFullName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.STAFF) }

    // Add User Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة مستخدم جديد وتحديد كلمة المرور", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newFullName,
                        onValueChange = { newFullName = it },
                        label = { Text("الاسم الظاهر للموظف") },
                        placeholder = { Text("مثال: سارة (استقبال)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("اسم المستخدم للدخول (Username)") },
                        placeholder = { Text("مثال: sara") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور (Password)") },
                        placeholder = { Text("حدد كلمة مرور للدخول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        placeholder = { Text("010xxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "صلاحيات الحساب:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (newRole == UserRole.STAFF) ClinicPrimary else Color(0xFFEEEEEE))
                                .clickable { newRole = UserRole.STAFF }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "موظف استقبال",
                                color = if (newRole == UserRole.STAFF) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (newRole == UserRole.ADMIN) Color(0xFF2E7D32) else Color(0xFFEEEEEE))
                                .clickable { newRole = UserRole.ADMIN }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "أدمن",
                                color = if (newRole == UserRole.ADMIN) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFullName.isBlank() || newUsername.isBlank() || newPassword.isBlank()) {
                            Toast.makeText(context, "يرجى إدخال الاسم واسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onAddUser(
                            UserEntity(
                                id = 0,
                                name = newFullName.trim(),
                                username = newUsername.trim().lowercase(),
                                password = newPassword.trim(),
                                phone = newPhone.trim(),
                                role = newRole
                            )
                        )
                        showAddDialog = false
                        newFullName = ""
                        newUsername = ""
                        newPassword = ""
                        newPhone = ""
                        Toast.makeText(context, "تمت إضافة المستخدم بنجاح", Toast.LENGTH_SHORT).show()
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

    // Edit User Dialog
    if (editingUser != null) {
        var editName by remember(editingUser) { mutableStateOf(editingUser!!.name) }
        var editUsername by remember(editingUser) { mutableStateOf(editingUser!!.username) }
        var editPassword by remember(editingUser) { mutableStateOf(editingUser!!.password) }
        var editPhone by remember(editingUser) { mutableStateOf(editingUser!!.phone) }
        val isAdminUser = editingUser!!.username.equals("admin", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("تعديل بيانات الحساب وكلمة المرور", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { if (!isAdminUser) editUsername = it },
                        label = { Text("اسم المستخدم") },
                        enabled = !isAdminUser,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingUser?.let { user ->
                            onUpdateUser(
                                user.copy(
                                    name = editName.trim(),
                                    username = editUsername.trim().lowercase(),
                                    password = editPassword.trim(),
                                    phone = editPhone.trim()
                                )
                            )
                        }
                        editingUser = null
                        Toast.makeText(context, "تم حفظ تعديلات الحساب بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete confirmation dialog
    if (deletingUser != null) {
        AlertDialog(
            onDismissRequest = { deletingUser = null },
            title = { Text("حذف المستخدم", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف حساب '${deletingUser?.name}'؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingUser?.let { onDeleteUser(it.id) }
                        deletingUser = null
                        Toast.makeText(context, "تم حذف المستخدم", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingUser = null }) { Text("إلغاء") }
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
                        text = "إدارة المستخدمين وكلمات المرور",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                }

                // Add User Button
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClinicPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مستخدم جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Credentials Reminder Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بيانات حساب الأدمن الثابت (admin / 1234655)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "• حساب الأدمن هو الوحيد المصرح له بدخول قسم الإعدادات وتحديد كلمات المرور لجميع الموظفين وإلغاء أي موعد.\n• يمكنك إضافة وتعديل حسابات الموظفين وكلمات مرورهم أدناه بحرية.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "جميع الحسابات المسجلة بالعيادة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF37474F)
                )
            }

            // List of Users
            items(allUsers, key = { it.id }) { user ->
                val isSelected = user.id == currentUser.id
                val isAdmin = user.role == UserRole.ADMIN

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCurrentUser(user) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isAdmin) Color(0xFF2E7D32) else ClinicPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF263238)
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2E7D32))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("الحساب الحالي", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "اسم المستخدم: ${user.username.ifBlank { "admin" }} • كلمة المرور: ${user.password.ifBlank { "1234655" }}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF546E7A),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = user.role.title,
                                    fontSize = 10.sp,
                                    color = if (isAdmin) Color(0xFF2E7D32) else Color(0xFF78909C),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingUser = user },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ClinicPrimary, modifier = Modifier.size(18.dp))
                            }

                            if (!user.username.equals("admin", ignoreCase = true)) {
                                IconButton(
                                    onClick = { deletingUser = user },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
