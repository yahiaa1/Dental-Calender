package com.example.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.CalendarViewMode
import com.example.ui.components.CalendarViewTabs
import com.example.ui.theme.ClinicBackground
import com.example.ui.theme.ClinicPrimary
import com.example.utils.DateUtils
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
    currentUser: com.example.data.model.UserEntity? = null,
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
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicBackground)
    ) {
        // Main Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date & Navigation Row (e.g. "< اليوم > أكتوبر 2026")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DateUtils.formatArabicMonthYear(selectedDate),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        // Left/Right day or week navigation
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
                                    if (prev.dayOfWeek == dayOff) {
                                        prev = prev.minusDays(1)
                                    }
                                }
                                onDateSelected(prev)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "السابق", tint = Color(0xFF455A64))
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
                                    if (next.dayOfWeek == dayOff) {
                                        next = next.plusDays(1)
                                    }
                                }
                                onDateSelected(next)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "التالي", tint = Color(0xFF455A64))
                        }

                        // "اليوم" Jump button
                        TextButton(
                            onClick = {
                                val today = LocalDate.now()
                                val target = if (hideDayOff && dayOff != null && today.dayOfWeek == dayOff) {
                                    today.plusDays(1)
                                } else {
                                    today
                                }
                                onDateSelected(target)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ClinicPrimary)
                        }
                    }

                    // Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Current User Profile & Logout Button
                        if (currentUser != null) {
                            val isAdmin = currentUser.role == com.example.data.model.UserRole.ADMIN
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAdmin) Color(0xFFE8F5E9) else Color(0xFFE0F2F1))
                                    .clickable { showLogoutConfirm = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAdmin) Color(0xFF2E7D32) else ClinicPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentUser.name.split(" ").firstOrNull() ?: currentUser.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) Color(0xFF1B5E20) else Color(0xFF004D40)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "خروج",
                                        tint = Color(0xFF78909C),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Notion Calendar Sync Icon
                        IconButton(
                            onClick = onOpenNotionSync,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEDE7F6))
                                .testTag("notion_sync_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = "ربط Notion Calendar",
                                tint = Color(0xFF5E35B1),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Hide/Show Cancelled Toggle Icon
                        IconButton(
                            onClick = onToggleHideCancelled,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hideCancelled) Color(0xFFFFF3E0) else Color(0xFFF5F7F7))
                                .testTag("toggle_hide_cancelled_button")
                        ) {
                            Icon(
                                imageVector = if (hideCancelled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideCancelled) "إظهار الملغاة" else "إخفاء الملغاة",
                                tint = if (hideCancelled) Color(0xFFE65100) else Color(0xFF78909C),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Hide/Show Day Off Toggle Icon
                        if (onToggleHideDayOff != null) {
                            val dayOffName = dayOff?.let { DateUtils.getArabicDayName(it) } ?: "الإجازة"
                            IconButton(
                                onClick = onToggleHideDayOff,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (hideDayOff) Color(0xFFFFEBEE) else Color(0xFFF5F7F7))
                                    .testTag("toggle_hide_day_off_button")
                            ) {
                                Icon(
                                    imageVector = if (hideDayOff) Icons.Default.EventBusy else Icons.Default.EventAvailable,
                                    contentDescription = if (hideDayOff) "إظهار يوم $dayOffName" else "إخفاء يوم $dayOffName",
                                    tint = if (hideDayOff) Color(0xFFC62828) else Color(0xFF546E7A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Filter Button
                        IconButton(
                            onClick = onOpenFilter,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFilterActive) ClinicPrimary.copy(alpha = 0.15f) else Color(0xFFF5F7F7))
                                .testTag("open_filter_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (isFilterActive) {
                                        Badge(containerColor = ClinicPrimary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "تصفية المواعيد",
                                    tint = if (isFilterActive) ClinicPrimary else Color(0xFF455A64),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // View Mode Tabs: [يوم] [4 أيام] [أسبوع] [شهر]
                CalendarViewTabs(
                    selectedMode = viewMode,
                    onModeSelected = onViewModeChange
                )
            }
        }

        // View Content
        Box(modifier = Modifier.weight(1f)) {
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
                    // 3 Days view with infinite continuous paging
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
                        dayOff = dayOff,
                        hideDayOff = hideDayOff,
                        onDateSelected = { date ->
                            onDateSelected(date)
                            onViewModeChange(CalendarViewMode.DAY)
                        }
                    )
                }
            }
        }
    }
}
