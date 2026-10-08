package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.CalendarViewMode
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.utils.DateUtils
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMainScreen(
    selectedDate: LocalDate,
    viewMode: CalendarViewMode,
    appointments: List<AppointmentEntity>,
    appointmentDots: Map<String, List<AppointmentStatus>>,
    procedureColors: Map<String, String>,
    isFilterActive: Boolean,
    workStartHour: Int,
    workEndHour: Int,
    hourHeightDp: Float,
    hideCancelled: Boolean,
    hideDayOff: Boolean = false,
    dayOff: java.time.DayOfWeek? = java.time.DayOfWeek.FRIDAY,
    currentUser: UserEntity? = null,
    onLogout: (() -> Unit)? = null,
    onToggleHideCancelled: () -> Unit,
    onToggleHideDayOff: (() -> Unit)? = null,
    onZoomChange: (Float) -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onTimeSlotSelected: (date: LocalDate, startTime: String, endTime: String) -> Unit,
    onAppointmentMoved: ((appointment: AppointmentEntity, newDate: String, newStartTime: String, newEndTime: String) -> Unit)? = null,
    onOpenFilter: () -> Unit,
    onOpenNotionSync: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var menu3DotsExpanded by remember { mutableStateOf(false) }
    var zoomSubMenuExpanded by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في تسجيل الخروج والعودة لشاشة الدخول؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("تسجيل الخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                ) {
                    // Header: Google Calendar Style with Clinic Branding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ClinicPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تقويم العيادة 🦷",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238)
                            )
                            if (currentUser != null) {
                                Text(
                                    text = "${currentUser.name} (${if (currentUser.role == UserRole.ADMIN) "أدمن" else "موظف"})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78909C)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                    // Group 1: Google Calendar View Modes (Matching Screenshot 3)
                    Text(
                        text = "طرق عرض التقويم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.CalendarViewDay,
                        label = "يوم (Day)",
                        isSelected = viewMode == CalendarViewMode.DAY,
                        onClick = {
                            onViewModeChange(CalendarViewMode.DAY)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.ViewColumn,
                        label = "3 أيام (3 Day)",
                        isSelected = viewMode == CalendarViewMode.THREE_DAYS,
                        onClick = {
                            onViewModeChange(CalendarViewMode.THREE_DAYS)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.CalendarViewWeek,
                        label = "أسبوع (Week)",
                        isSelected = viewMode == CalendarViewMode.WEEK,
                        onClick = {
                            onViewModeChange(CalendarViewMode.WEEK)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "شهر (Month)",
                        isSelected = viewMode == CalendarViewMode.MONTH,
                        onClick = {
                            onViewModeChange(CalendarViewMode.MONTH)
                            scope.launch { drawerState.close() }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                    // Group 2: Clinic Sections (Transferred from Bottom Bar)
                    Text(
                        text = "أقسام العيادة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.People,
                        label = "سجلات المرضى",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToPatients()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.BarChart,
                        label = "التقارير والإيرادات",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToReports()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.Settings,
                        label = "إعدادات العيادة",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                    )
                    DrawerItem(
                        icon = Icons.Default.SyncAlt,
                        label = "مزامنة Notion Calendar",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenNotionSync()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                    // Group 3: Display & Zoom Controls
                    Text(
                        text = "خيارات العرض والتكبير",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90A4AE),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DrawerItem(
                        icon = Icons.Default.Search,
                        label = "بحث وتصفية المواعيد",
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenFilter()
                        }
                    )

                    DrawerItem(
                        icon = if (hideCancelled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        label = if (hideCancelled) "إظهار المواعيد الملغاة" else "إخفاء المواعيد الملغاة",
                        onClick = {
                            onToggleHideCancelled()
                        }
                    )

                    if (onToggleHideDayOff != null) {
                        val dayOffName = dayOff?.let { DateUtils.getArabicDayName(it) } ?: "الجمعة"
                        DrawerItem(
                            icon = if (hideDayOff) Icons.Default.EventBusy else Icons.Default.EventAvailable,
                            label = if (hideDayOff) "إظهار يوم $dayOffName" else "إخفاء يوم $dayOffName",
                            onClick = {
                                onToggleHideDayOff()
                            }
                        )
                    }

                    // Zoom level presets in drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "مستوى التكبير:", fontSize = 12.sp, color = Color(0xFF546E7A), fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ZoomButton("عادي", hourHeightDp in 80f..95f) { onZoomChange(85f) }
                            ZoomButton("مريح", hourHeightDp in 96f..120f) { onZoomChange(110f) }
                            ZoomButton("مفصل", hourHeightDp > 120f) { onZoomChange(140f) }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                    // Logout Item
                    DrawerItem(
                        icon = Icons.Default.Logout,
                        label = "تسجيل الخروج",
                        textColor = Color(0xFFD32F2F),
                        iconTint = Color(0xFFD32F2F),
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutConfirm = true
                        }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ClinicBackground)
        ) {
            // SINGLE ULTRA-CLEAN GOOGLE CALENDAR TOP BAR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation: Hamburger Menu Icon (☰) & Month/Year with navigation arrows
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // ☰ Menu Icon (Google Calendar Drawer)
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "القائمة الرئيسية", tint = Color(0xFF37474F))
                        }

                        // Month Year Text (e.g. "أكتوبر 2026")
                        Text(
                            text = DateUtils.formatArabicMonthYear(selectedDate),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238),
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                        )

                        // Next/Prev Arrows
                        IconButton(
                            onClick = {
                                val step = when (viewMode) {
                                    CalendarViewMode.DAY -> 1L
                                    CalendarViewMode.THREE_DAYS -> 3L
                                    CalendarViewMode.WEEK -> 7L
                                    CalendarViewMode.MONTH -> 30L
                                }
                                var prev = selectedDate.minusDays(step)
                                if (hideDayOff && dayOff != null && (viewMode == CalendarViewMode.DAY || viewMode == CalendarViewMode.THREE_DAYS)) {
                                    if (prev.dayOfWeek == dayOff) prev = prev.minusDays(1)
                                }
                                onDateSelected(prev)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "السابق", tint = Color(0xFF546E7A))
                        }

                        IconButton(
                            onClick = {
                                val step = when (viewMode) {
                                    CalendarViewMode.DAY -> 1L
                                    CalendarViewMode.THREE_DAYS -> 3L
                                    CalendarViewMode.WEEK -> 7L
                                    CalendarViewMode.MONTH -> 30L
                                }
                                var next = selectedDate.plusDays(step)
                                if (hideDayOff && dayOff != null && (viewMode == CalendarViewMode.DAY || viewMode == CalendarViewMode.THREE_DAYS)) {
                                    if (next.dayOfWeek == dayOff) next = next.plusDays(1)
                                }
                                onDateSelected(next)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "التالي", tint = Color(0xFF546E7A))
                        }
                    }

                    // Action Icons: [اليوم] [بحث] [3 نقط ⋮]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Quick "اليوم" (Today) jump
                        TextButton(
                            onClick = {
                                val today = LocalDate.now()
                                val target = if (hideDayOff && dayOff != null && today.dayOfWeek == dayOff) today.plusDays(1) else today
                                onDateSelected(target)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("اليوم", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClinicPrimary)
                        }

                        // Search / Filter Button
                        IconButton(
                            onClick = onOpenFilter,
                            modifier = Modifier.size(38.dp)
                        ) {
                            BadgedBox(badge = { if (isFilterActive) Badge(containerColor = ClinicPrimary) }) {
                                Icon(Icons.Default.Search, contentDescription = "بحث وتصفية", tint = if (isFilterActive) ClinicPrimary else Color(0xFF546E7A), modifier = Modifier.size(20.dp))
                            }
                        }

                        // 3 DOTS (⋮) OVERFLOW MENU
                        Box {
                            IconButton(
                                onClick = { menu3DotsExpanded = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "المزيد من الخيارات", tint = Color(0xFF37474F))
                            }

                            DropdownMenu(
                                expanded = menu3DotsExpanded,
                                onDismissRequest = {
                                    menu3DotsExpanded = false
                                    zoomSubMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("طريقة العرض: يوم", fontWeight = if (viewMode == CalendarViewMode.DAY) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.CalendarViewDay, contentDescription = null, tint = ClinicPrimary) },
                                    onClick = {
                                        onViewModeChange(CalendarViewMode.DAY)
                                        menu3DotsExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("طريقة العرض: 3 أيام", fontWeight = if (viewMode == CalendarViewMode.THREE_DAYS) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.ViewColumn, contentDescription = null, tint = ClinicPrimary) },
                                    onClick = {
                                        onViewModeChange(CalendarViewMode.THREE_DAYS)
                                        menu3DotsExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("طريقة العرض: أسبوع", fontWeight = if (viewMode == CalendarViewMode.WEEK) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.CalendarViewWeek, contentDescription = null, tint = ClinicPrimary) },
                                    onClick = {
                                        onViewModeChange(CalendarViewMode.WEEK)
                                        menu3DotsExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("طريقة العرض: شهر", fontWeight = if (viewMode == CalendarViewMode.MONTH) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ClinicPrimary) },
                                    onClick = {
                                        onViewModeChange(CalendarViewMode.MONTH)
                                        menu3DotsExpanded = false
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("سجلات المرضى") },
                                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF0288D1)) },
                                    onClick = {
                                        menu3DotsExpanded = false
                                        onNavigateToPatients()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("التقارير والإيرادات") },
                                    leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF00796B)) },
                                    onClick = {
                                        menu3DotsExpanded = false
                                        onNavigateToReports()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إعدادات العيادة") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF546E7A)) },
                                    onClick = {
                                        menu3DotsExpanded = false
                                        onNavigateToSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مزامنة Notion Calendar") },
                                    leadingIcon = { Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF5E35B1)) },
                                    onClick = {
                                        menu3DotsExpanded = false
                                        onOpenNotionSync()
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text(if (hideCancelled) "إظهار الملغاة" else "إخفاء الملغاة") },
                                    leadingIcon = { Icon(if (hideCancelled) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) },
                                    onClick = {
                                        onToggleHideCancelled()
                                        menu3DotsExpanded = false
                                    }
                                )

                                if (onToggleHideDayOff != null) {
                                    val dayOffName = dayOff?.let { DateUtils.getArabicDayName(it) } ?: "الجمعة"
                                    DropdownMenuItem(
                                        text = { Text(if (hideDayOff) "إظهار $dayOffName" else "إخفاء $dayOffName") },
                                        leadingIcon = { Icon(if (hideDayOff) Icons.Default.EventBusy else Icons.Default.EventAvailable, contentDescription = null) },
                                        onClick = {
                                            onToggleHideDayOff()
                                            menu3DotsExpanded = false
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("مستوى التكبير (عادي / مريح / مفصل)") },
                                    leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                                    onClick = {
                                        val nextZoom = when {
                                            hourHeightDp < 96f -> 110f
                                            hourHeightDp < 125f -> 140f
                                            else -> 85f
                                        }
                                        onZoomChange(nextZoom)
                                        menu3DotsExpanded = false
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("تسجيل الخروج", color = Color(0xFFD32F2F)) },
                                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFD32F2F)) },
                                    onClick = {
                                        menu3DotsExpanded = false
                                        showLogoutConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // FULL HEIGHT CALENDAR VIEW (Maximizes Screen Space!)
            Box(modifier = Modifier.fillMaxSize()) {
                when (viewMode) {
                    CalendarViewMode.DAY -> {
                        ProportionalTimelineView(
                            centerDate = selectedDate,
                            daysCount = 1,
                            workStartHour = workStartHour,
                            workEndHour = workEndHour,
                            hourHeightDp = hourHeightDp,
                            appointments = appointments,
                            procedureColors = procedureColors,
                            onAppointmentClick = onAppointmentClick,
                            onTimeSlotSelected = onTimeSlotSelected,
                            onAppointmentMoved = onAppointmentMoved,
                            onZoomChange = onZoomChange,
                            onDateChanged = onDateSelected,
                            hideDayOff = hideDayOff,
                            dayOff = dayOff
                        )
                    }

                    CalendarViewMode.THREE_DAYS -> {
                        ProportionalTimelineView(
                            centerDate = selectedDate,
                            daysCount = 3,
                            workStartHour = workStartHour,
                            workEndHour = workEndHour,
                            hourHeightDp = hourHeightDp,
                            appointments = appointments,
                            procedureColors = procedureColors,
                            onAppointmentClick = onAppointmentClick,
                            onTimeSlotSelected = onTimeSlotSelected,
                            onAppointmentMoved = onAppointmentMoved,
                            onZoomChange = onZoomChange,
                            onDateChanged = onDateSelected,
                            hideDayOff = hideDayOff,
                            dayOff = dayOff
                        )
                    }

                    CalendarViewMode.WEEK -> {
                        ProportionalTimelineView(
                            centerDate = selectedDate,
                            daysCount = 7,
                            workStartHour = workStartHour,
                            workEndHour = workEndHour,
                            hourHeightDp = hourHeightDp,
                            appointments = appointments,
                            procedureColors = procedureColors,
                            onAppointmentClick = onAppointmentClick,
                            onTimeSlotSelected = onTimeSlotSelected,
                            onAppointmentMoved = onAppointmentMoved,
                            onZoomChange = onZoomChange,
                            onDateChanged = onDateSelected,
                            hideDayOff = hideDayOff,
                            dayOff = dayOff
                        )
                    }

                    CalendarViewMode.MONTH -> {
                        MonthCalendarView(
                            selectedDate = selectedDate,
                            appointmentDots = appointmentDots,
                            onDateSelected = { clickedDate ->
                                onDateSelected(clickedDate)
                                onViewModeChange(CalendarViewMode.THREE_DAYS)
                            },
                            hideDayOff = hideDayOff,
                            dayOff = dayOff
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    textColor: Color = Color(0xFF263238),
    iconTint: Color = ClinicPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) ClinicPrimary.copy(alpha = 0.12f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ClinicPrimary else iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) ClinicPrimary else textColor
            )
        }
    }
}

@Composable
private fun ZoomButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ClinicPrimary else Color(0xFFECEFF1))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF455A64)
        )
    }
}
