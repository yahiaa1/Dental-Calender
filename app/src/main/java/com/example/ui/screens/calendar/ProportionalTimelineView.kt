package com.example.ui.screens.calendar

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AppointmentEntity
import com.example.data.model.PatientClassification
import com.example.ui.theme.ClinicPrimary
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

private const val PAGER_BASE_INDEX = 5000

data class PositionedAppointment(
    val appointment: AppointmentEntity,
    val colIndex: Int,
    val totalCols: Int,
    val startTotalMinutes: Int,
    val durationMinutes: Int
)

data class DragMoveState(
    val appointment: AppointmentEntity,
    val originDate: LocalDate,
    val originStartMins: Int,
    val durationMinutes: Int,
    val targetDate: LocalDate,
    val targetStartMins: Int,
    val targetEndMins: Int,
    val dragOffsetX: Float = 0f,
    val dragOffsetY: Float = 0f
)

data class TimeSlotSelection(
    val date: LocalDate,
    val anchorMins: Int,
    val startMins: Int,
    val endMins: Int
)

@Composable
fun ProportionalTimelineView(
    centerDate: LocalDate,
    daysCount: Int, // 1, 3, or 7
    workStartHour: Int, // e.g. 9
    workEndHour: Int,   // e.g. 21
    hourHeightDp: Float, // e.g. 85f
    appointments: List<AppointmentEntity>,
    procedureColors: Map<String, String>,
    onAppointmentClick: (AppointmentEntity) -> Unit,
    onTimeSlotSelected: (date: LocalDate, startTime: String, endTime: String) -> Unit,
    onAppointmentMoved: ((appointment: AppointmentEntity, newDate: String, newStartTime: String, newEndTime: String) -> Unit)? = null,
    onZoomChange: (Float) -> Unit,
    onDateChanged: ((LocalDate) -> Unit)? = null,
    hideDayOff: Boolean = false,
    dayOff: java.time.DayOfWeek? = java.time.DayOfWeek.FRIDAY,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalHours = max(1, workEndHour - workStartHour)

    // Animated zoom height for snappy transitions
    val animatedHourHeight by animateFloatAsState(
        targetValue = hourHeightDp,
        label = "animatedHourHeight"
    )
    val totalHeightDp = (totalHours * animatedHourHeight).dp

    // Anchor base date: 2026-01-03 is Saturday (Week starts Saturday -> Friday)
    val baseAnchorDate = remember { LocalDate.of(2026, 1, 3) }

    // Pre-group appointments by date for instantaneous O(1) rendering during scroll & pager gestures
    val appointmentsByDate = remember(appointments) { appointments.groupBy { it.dateStr } }

    val allWeekDays = remember {
        listOf(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
    }

    val activeWeekDays = remember(hideDayOff, dayOff) {
        if (hideDayOff && dayOff != null) {
            allWeekDays.filter { it != dayOff }
        } else {
            allWeekDays
        }
    }

    // Helper to calculate the dates shown in a specific page
    val getDatesForPage: (Int) -> List<LocalDate> = remember(daysCount, hideDayOff, dayOff, activeWeekDays) {
        { page: Int ->
            val pageOffset = (page - PAGER_BASE_INDEX).toLong()
            when (daysCount) {
                7 -> {
                    val weekOffset = pageOffset
                    val weekSaturday = baseAnchorDate.plusWeeks(weekOffset)
                    activeWeekDays.map { dow ->
                        val daysFromSat = (dow.value + 1) % 7
                        weekSaturday.plusDays(daysFromSat.toLong())
                    }
                }
                3 -> {
                    if (hideDayOff && dayOff != null) {
                        val weekOffset = Math.floorDiv(pageOffset, 2L)
                        val subPage = Math.floorMod(pageOffset, 2L).toInt()
                        val weekSaturday = baseAnchorDate.plusWeeks(weekOffset)
                        val weekDays = activeWeekDays.map { dow ->
                            val daysFromSat = (dow.value + 1) % 7
                            weekSaturday.plusDays(daysFromSat.toLong())
                        }
                        if (subPage == 0) weekDays.take(3) else weekDays.drop(3).take(3)
                    } else {
                        val pageStartDate = baseAnchorDate.plusDays(pageOffset * 3)
                        (0 until 3).map { pageStartDate.plusDays(it.toLong()) }
                    }
                }
                else -> { // daysCount == 1 (Day view)
                    if (hideDayOff && dayOff != null) {
                        val activePerWeek = activeWeekDays.size.toLong()
                        val weekOffset = Math.floorDiv(pageOffset, activePerWeek)
                        val dayIndex = Math.floorMod(pageOffset, activePerWeek).toInt()
                        val weekSaturday = baseAnchorDate.plusWeeks(weekOffset)
                        val targetDow = activeWeekDays[dayIndex]
                        val daysFromSat = (targetDow.value + 1) % 7
                        listOf(weekSaturday.plusDays(daysFromSat.toLong()))
                    } else {
                        listOf(baseAnchorDate.plusDays(pageOffset))
                    }
                }
            }
        }
    }

    // Helper to calculate the page index for a given date
    val getPageForDate: (LocalDate) -> Int = remember(daysCount, hideDayOff, dayOff, activeWeekDays) {
        { targetDate: LocalDate ->
            var activeDate = targetDate
            if (hideDayOff && dayOff != null && activeDate.dayOfWeek == dayOff) {
                activeDate = activeDate.plusDays(1)
            }
            val daysFromSat = (activeDate.dayOfWeek.value + 1) % 7
            val weekSaturday = activeDate.minusDays(daysFromSat.toLong())
            val weekOffset = ChronoUnit.DAYS.between(baseAnchorDate, weekSaturday) / 7

            when (daysCount) {
                7 -> {
                    (PAGER_BASE_INDEX + weekOffset).toInt()
                }
                3 -> {
                    if (hideDayOff && dayOff != null) {
                        val indexInActive = activeWeekDays.indexOf(activeDate.dayOfWeek).coerceAtLeast(0)
                        val subPage = if (indexInActive < 3) 0 else 1
                        (PAGER_BASE_INDEX + (weekOffset * 2 + subPage)).toInt()
                    } else {
                        val daysDiff = ChronoUnit.DAYS.between(baseAnchorDate, activeDate)
                        (PAGER_BASE_INDEX + (daysDiff / 3)).toInt()
                    }
                }
                else -> { // daysCount == 1
                    if (hideDayOff && dayOff != null) {
                        val indexInActive = activeWeekDays.indexOf(activeDate.dayOfWeek).coerceAtLeast(0)
                        (PAGER_BASE_INDEX + (weekOffset * activeWeekDays.size + indexInActive)).toInt()
                    } else {
                        val daysDiff = ChronoUnit.DAYS.between(baseAnchorDate, activeDate)
                        (PAGER_BASE_INDEX + daysDiff).toInt()
                    }
                }
            }
        }
    }

    // Infinite Horizontal Pager: 10,000 pages for continuous, endless swiping
    val initialPage = remember(daysCount, hideDayOff, dayOff) {
        getPageForDate(centerDate).coerceIn(0, 9999)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 10000 }
    )

    // Synchronize pager when centerDate or settings change externally
    LaunchedEffect(centerDate, daysCount, hideDayOff, dayOff) {
        val expectedPage = getPageForDate(centerDate).coerceIn(0, 9999)
        if (pagerState.currentPage != expectedPage) {
            pagerState.animateScrollToPage(expectedPage)
        }
    }

    // Synchronize external date when user swipes pager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentPage ->
            val pageDates = getDatesForPage(currentPage)
            val newPageDate = pageDates.firstOrNull() ?: centerDate
            if (newPageDate != centerDate) {
                onDateChanged?.invoke(newPageDate)
            }
        }
    }

    val verticalScrollState = rememberScrollState()

    // Scroll to current time or work start initially
    LaunchedEffect(Unit) {
        val nowHour = java.time.LocalTime.now().hour
        if (nowHour in workStartHour until workEndHour) {
            val initialScrollPx = ((nowHour - workStartHour) * hourHeightDp * 2.5f).roundToInt()
            verticalScrollState.scrollTo(initialScrollPx)
        }
    }

    // Unified Drag & Move State (supports moving across ANY DAY and time!)
    var activeDragState by remember { mutableStateOf<DragMoveState?>(null) }

    val onStartDrag: (AppointmentEntity, LocalDate, Int, Int) -> Unit = { app, date, startMins, durMins ->
        activeDragState = DragMoveState(
            appointment = app,
            originDate = date,
            originStartMins = startMins,
            durationMinutes = durMins,
            targetDate = date,
            targetStartMins = startMins,
            targetEndMins = startMins + durMins,
            dragOffsetX = 0f,
            dragOffsetY = 0f
        )
    }

    val onDrag: (Offset, Float) -> Unit = { dragAmount, colWidthPx ->
        activeDragState?.let { current ->
            val newOffsetX = current.dragOffsetX + dragAmount.x
            val newOffsetY = current.dragOffsetY + dragAmount.y

            val hourHeightPx = (animatedHourHeight * 2.625f) // approximate dp-to-px or safe scale
            val rawStartMins = current.originStartMins + ((newOffsetY / animatedHourHeight) * 20).roundToInt()
            val snappedStart = ((rawStartMins / 15) * 15).coerceIn(0, totalHours * 60 - current.durationMinutes)
            val snappedEnd = snappedStart + current.durationMinutes

            // In RTL, dragging left (negative X) moves to future days, dragging right moves to past days
            val stepPx = if (daysCount == 1) 220f else max(100f, colWidthPx)
            val dayDelta = (-newOffsetX / stepPx).roundToInt()
            val newTargetDate = current.originDate.plusDays(dayDelta.toLong())

            activeDragState = current.copy(
                targetDate = newTargetDate,
                targetStartMins = snappedStart,
                targetEndMins = snappedEnd,
                dragOffsetX = newOffsetX,
                dragOffsetY = newOffsetY
            )
        }
    }

    val performMove = { state: DragMoveState ->
        val startHour = (workStartHour + state.targetStartMins / 60).coerceIn(workStartHour, workEndHour - 1)
        val startMin = state.targetStartMins % 60
        val endHour = (workStartHour + state.targetEndMins / 60).coerceIn(startHour, workEndHour)
        val endMin = state.targetEndMins % 60

        val newStartTimeStr = String.format("%02d:%02d", startHour, startMin)
        val newEndTimeStr = String.format("%02d:%02d", endHour, endMin)
        val newDateStr = DateUtils.toDateString(state.targetDate)

        if (newStartTimeStr != state.appointment.startTime ||
            newEndTimeStr != state.appointment.endTime ||
            newDateStr != state.appointment.dateStr
        ) {
            onAppointmentMoved?.invoke(state.appointment, newDateStr, newStartTimeStr, newEndTimeStr)
            Toast.makeText(
                context,
                "تم نقل موعد ${state.appointment.patientName} إلى: ${DateUtils.formatArabicFullDate(state.targetDate)} الساعة ${TimeFormatter.format12Hour(newStartTimeStr)}",
                Toast.LENGTH_SHORT
            ).show()
        }
        activeDragState = null
    }

    val onFinishDrag: () -> Unit = {
        activeDragState?.let { state ->
            performMove(state)
        }
    }

    val onCancelDrag: () -> Unit = {
        activeDragState?.let { state ->
            if (state.targetDate == state.originDate && state.targetStartMins == state.originStartMins) {
                activeDragState = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {


        // Interactive Move Mode Floating Control Bar
        // Allows moving the appointment to ANY DAY or TIME with live buttons and calendar picker!
        if (activeDragState != null) {
            val moveState = activeDragState!!
            val startH = (workStartHour + moveState.targetStartMins / 60).coerceIn(workStartHour, workEndHour - 1)
            val startM = moveState.targetStartMins % 60
            val endH = (workStartHour + moveState.targetEndMins / 60).coerceIn(startH, workEndHour)
            val endM = moveState.targetEndMins % 60
            val liveTimeStr = "${TimeFormatter.format12Hour(String.format("%02d:%02d", startH, startM))} - ${TimeFormatter.format12Hour(String.format("%02d:%02d", endH, endM))}"
            val targetDateArabic = DateUtils.formatArabicFullDate(moveState.targetDate)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF263238)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "نقل موعد: ${moveState.appointment.patientName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "📅 $targetDateArabic • ⏰ $liveTimeStr",
                                fontSize = 11.sp,
                                color = Color(0xFF80CBC4),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Confirm Move Button
                            Button(
                                onClick = { performMove(moveState) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تأكيد النقل ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Cancel Move Button
                            OutlinedButton(
                                onClick = { activeDragState = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF90A4AE))
                            ) {
                                Text("إلغاء", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Day Selector Chips (Move to Any Day!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "اختر اليوم:", fontSize = 10.sp, color = Color(0xFFB0BEC5))

                        // Previous Day
                        MoveDayChip("◀ أمس") {
                            activeDragState = moveState.copy(targetDate = moveState.targetDate.minusDays(1))
                        }

                        // Today
                        MoveDayChip("اليوم") {
                            activeDragState = moveState.copy(targetDate = LocalDate.now())
                        }

                        // Tomorrow
                        MoveDayChip("غداً") {
                            activeDragState = moveState.copy(targetDate = LocalDate.now().plusDays(1))
                        }

                        // Day after tomorrow
                        MoveDayChip("بعد غد") {
                            activeDragState = moveState.copy(targetDate = LocalDate.now().plusDays(2))
                        }

                        // Next Day
                        MoveDayChip("التالي ▶") {
                            activeDragState = moveState.copy(targetDate = moveState.targetDate.plusDays(1))
                        }

                        // Pick Any Date Dialog
                        MoveDayChip("📅 يوم آخر...") {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val picked = LocalDate.of(y, m + 1, d)
                                    activeDragState = moveState.copy(targetDate = picked)
                                },
                                moveState.targetDate.year,
                                moveState.targetDate.monthValue - 1,
                                moveState.targetDate.dayOfMonth
                            ).show()
                        }
                    }
                }
            }
        }

        // Infinite Horizontal Pager - Clean swiping across the entire schedule
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val datesInThisPage = remember(page, daysCount, hideDayOff, dayOff) {
                getDatesForPage(page)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Days of the week row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(vertical = 6.dp)
                ) {
                    // Left gutter matching time labels width
                    Spacer(modifier = Modifier.width(52.dp))

                    datesInThisPage.forEach { date ->
                        val isToday = date == LocalDate.now()
                        val isDayOff = date.dayOfWeek == dayOff
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDateChanged?.invoke(date) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DateUtils.getArabicDayShortName(date.dayOfWeek) + if (isDayOff) " (إجازة)" else "",
                                fontSize = if (isDayOff) 10.sp else 11.sp,
                                color = when {
                                    isToday -> ClinicPrimary
                                    isDayOff -> Color(0xFFE53935)
                                    else -> Color(0xFF70757A)
                                },
                                fontWeight = if (isToday || isDayOff) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) ClinicPrimary else if (isDayOff) Color(0xFFFFEBEE) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) Color.White else if (isDayOff) Color(0xFFC62828) else Color(0xFF3C4043)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFDADCE0))

                // Scrollable Grid Area (Hours + Day Columns)
                // Note: No pointerInput here so HorizontalPager and verticalScroll work smoothly without interruption!
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Time Labels Column
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .height(totalHeightDp)
                            .background(Color(0xFFFAFAFA))
                    ) {
                        for (hour in workStartHour until workEndHour) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(animatedHourHeight.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text(
                                    text = TimeFormatter.formatHourLabel12(hour),
                                    fontSize = 11.sp,
                                    color = Color(0xFF70757A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Divider between time column and day grid
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(totalHeightDp)
                            .background(Color(0xFFDADCE0))
                    )

                    // Day Columns for this page
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(totalHeightDp)
                    ) {
                        datesInThisPage.forEach { date ->
                            val dateStr = DateUtils.toDateString(date)
                            val dayAppointments = appointmentsByDate[dateStr] ?: emptyList()

                            Box(modifier = Modifier.weight(1f)) {
                                DayColumnSlot(
                                    date = date,
                                    totalHeightDp = totalHeightDp,
                                    totalHours = totalHours,
                                    workStartHour = workStartHour,
                                    workEndHour = workEndHour,
                                    hourHeightDp = animatedHourHeight,
                                    appointments = dayAppointments,
                                    procedureColors = procedureColors,
                                    activeDragState = activeDragState,
                                    onTimeSlotSelected = onTimeSlotSelected,
                                    onStartDrag = onStartDrag,
                                    onDrag = onDrag,
                                    onFinishDrag = onFinishDrag,
                                    onCancelDrag = onCancelDrag,
                                    onAppointmentClick = onAppointmentClick
                                )
                            }

                            // Divider between days
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(totalHeightDp)
                                    .background(Color(0xFFDADCE0))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayColumnSlot(
    date: LocalDate,
    totalHeightDp: Dp,
    totalHours: Int,
    workStartHour: Int,
    workEndHour: Int,
    hourHeightDp: Float,
    appointments: List<AppointmentEntity>,
    procedureColors: Map<String, String>,
    activeDragState: DragMoveState?,
    onTimeSlotSelected: (date: LocalDate, startTime: String, endTime: String) -> Unit,
    onStartDrag: (AppointmentEntity, LocalDate, Int, Int) -> Unit,
    onDrag: (Offset, Float) -> Unit,
    onFinishDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    onAppointmentClick: (AppointmentEntity) -> Unit
) {
    val density = LocalDensity.current

    // Overlapping Layout Calculation (Google Calendar multi-column algorithm)
    val positionedAppointments = remember(appointments, workStartHour) {
        layoutOverlappingAppointments(appointments, workStartHour)
    }

    var activeSlotSelection by remember { mutableStateOf<TimeSlotSelection?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp)
            // 1. Slide with finger on empty timeline to select booking time range
            .pointerInput(date, hourHeightDp) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (activeDragState == null) {
                            val hourHeightPx = hourHeightDp.dp.toPx()
                            val tappedMins = ((offset.y / hourHeightPx) * 60).roundToInt()
                            val snappedStart = ((tappedMins / 15) * 15).coerceIn(0, totalHours * 60 - 15)
                            activeSlotSelection = TimeSlotSelection(
                                date = date,
                                anchorMins = snappedStart,
                                startMins = snappedStart,
                                endMins = (snappedStart + 30).coerceAtMost(totalHours * 60)
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        activeSlotSelection?.let { sel ->
                            val hourHeightPx = hourHeightDp.dp.toPx()
                            val curMins = ((change.position.y / hourHeightPx) * 60).roundToInt()
                            val snappedCur = ((curMins + 7) / 15 * 15).coerceIn(0, totalHours * 60)
                            val sMins = minOf(sel.anchorMins, snappedCur)
                            val eMins = maxOf(sel.anchorMins + 15, snappedCur)
                            activeSlotSelection = sel.copy(
                                startMins = sMins,
                                endMins = eMins
                            )
                        }
                    },
                    onDragEnd = {
                        activeSlotSelection?.let { sel ->
                            val startHour = (workStartHour + sel.startMins / 60).coerceIn(workStartHour, workEndHour - 1)
                            val startMin = sel.startMins % 60
                            val endHour = (workStartHour + sel.endMins / 60).coerceIn(startHour, workEndHour)
                            val endMin = sel.endMins % 60
                            val sTime = String.format("%02d:%02d", startHour, startMin)
                            val eTime = String.format("%02d:%02d", endHour, endMin)
                            activeSlotSelection = null
                            onTimeSlotSelected(date, sTime, eTime)
                        }
                    },
                    onDragCancel = {
                        activeSlotSelection = null
                    }
                )
            }
            // 2. Tap on empty slot to add default 45 min appointment
            .pointerInput(date, hourHeightDp) {
                detectTapGestures(
                    onTap = { offset ->
                        if (activeDragState == null && activeSlotSelection == null) {
                            val hourHeightPx = hourHeightDp.dp.toPx()
                            val tappedMins = ((offset.y / hourHeightPx) * 60).roundToInt()
                            val snappedStartMins = (tappedMins / 30) * 30
                            val snappedEndMins = snappedStartMins + 45

                            val startHour = (workStartHour + snappedStartMins / 60).coerceIn(workStartHour, workEndHour - 1)
                            val startMin = (snappedStartMins % 60)
                            val endHour = (workStartHour + snappedEndMins / 60).coerceIn(startHour, workEndHour)
                            val endMin = (snappedEndMins % 60)

                            val sTime = String.format("%02d:%02d", startHour, startMin)
                            val eTime = String.format("%02d:%02d", endHour, endMin)
                            onTimeSlotSelected(date, sTime, eTime)
                        }
                    }
                )
            }
    ) {
        val columnWidth = maxWidth
        val columnWidthPx = with(density) { columnWidth.toPx() }

        // Background horizontal hour grid lines
        Column(modifier = Modifier.fillMaxSize()) {
            for (h in 0 until totalHours) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeightDp.dp)
                ) {
                    // Main hour line
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE8EAED))
                    // Subtle half-hour dashed line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (hourHeightDp / 2).dp)
                            .height(0.5.dp)
                            .background(Color(0xFFF1F3F4))
                    )
                }
            }
        }

        // Live Ghost Target Drop Slot when dragging towards this day
        if (activeDragState != null && activeDragState.targetDate == date) {
            val targetTopDp = (activeDragState.targetStartMins / 60f * hourHeightDp).dp
            val targetHeightDp = max(24f, (activeDragState.targetEndMins - activeDragState.targetStartMins) / 60f * hourHeightDp).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .offset(y = targetTopDp)
                    .height(targetHeightDp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ClinicPrimary.copy(alpha = 0.20f))
                    .border(2.dp, ClinicPrimary, RoundedCornerShape(8.dp))
                    .zIndex(5f)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ClinicPrimary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "نقل إلى هنا",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Live Selection Box when sliding with finger to book time
        if (activeSlotSelection != null && activeSlotSelection!!.date == date) {
            val sel = activeSlotSelection!!
            val selTopDp = (sel.startMins / 60f * hourHeightDp).dp
            val selHeightDp = max(32f, (sel.endMins - sel.startMins) / 60f * hourHeightDp).dp
            val sHour = (workStartHour + sel.startMins / 60).coerceIn(workStartHour, workEndHour - 1)
            val sMin = sel.startMins % 60
            val eHour = (workStartHour + sel.endMins / 60).coerceIn(sHour, workEndHour)
            val eMin = sel.endMins % 60
            val start12 = TimeFormatter.format12Hour(String.format("%02d:%02d", sHour, sMin))
            val end12 = TimeFormatter.format12Hour(String.format("%02d:%02d", eHour, eMin))
            val durationMins = sel.endMins - sel.startMins
            val durText = if (durationMins >= 60) {
                val h = durationMins / 60
                val m = durationMins % 60
                if (m > 0) "$h س و $m د" else "$h ساعة"
            } else {
                "$durationMins دقيقة"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .offset(y = selTopDp)
                    .height(selHeightDp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ClinicPrimary.copy(alpha = 0.25f))
                    .border(2.dp, ClinicPrimary, RoundedCornerShape(8.dp))
                    .zIndex(8f)
            ) {
                // Top handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ClinicPrimary)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Start time badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ClinicPrimary)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "من $start12",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Duration center label
                    if (selHeightDp >= 44.dp) {
                        Text(
                            text = "⏱️ $durText (اسحب لتعديل الوقت)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClinicPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // End time badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF004D40))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إلى $end12",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Bottom handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ClinicPrimary)
                )
            }
        }

        // Render Positioned Appointments
        positionedAppointments.forEach { pos ->
            val app = pos.appointment
            val isBeingDragged = activeDragState?.appointment?.id == app.id

            // Calculate width and horizontal offset for overlapping cards
            val subWidth = columnWidth / pos.totalCols
            val xOffsetDp = subWidth * pos.colIndex
            val cardWidthDp = if (subWidth > 3.dp) subWidth - 3.dp else 0.dp

            val baseTopOffsetDp = (pos.startTotalMinutes / 60f * hourHeightDp).dp
            val cardHeightDp = max(26f, pos.durationMinutes / 60f * hourHeightDp).dp

            val colorHex = procedureColors[app.procedureName] ?: "#1A73E8"
            val procColor = try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                Color(0xFF1A73E8)
            }

            val isLatePatient = app.patientClassification == PatientClassification.OFTEN_LATE ||
                    app.patientClassification == PatientClassification.FREQUENT_ABSENT

            Card(
                modifier = Modifier
                    .width(cardWidthDp)
                    .offset(x = xOffsetDp, y = baseTopOffsetDp)
                    .height(cardHeightDp)
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .clip(RoundedCornerShape(6.dp))
                    // Long-press Drag Gesture Detector (Move across ANY DAY and hour)
                    .pointerInput(app.id, hourHeightDp) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                onStartDrag(app, date, pos.startTotalMinutes, pos.durationMinutes)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount, columnWidthPx)
                            },
                            onDragEnd = {
                                onFinishDrag()
                            },
                            onDragCancel = {
                                onCancelDrag()
                            }
                        )
                    }
                    .clickable {
                        if (activeDragState == null) {
                            onAppointmentClick(app)
                        }
                    }
                    .testTag("proportional_card_${app.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBeingDragged) Color(0xFFEF6C00) else procColor
                ),
                shape = RoundedCornerShape(6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isBeingDragged) 8.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = app.patientName,
                            fontSize = if (pos.totalCols > 1) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLatePatient) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD54F))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text("متأخر", fontSize = 7.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Procedure & Status & Time (12-Hour format)
                    Text(
                        text = "${app.procedureName} [${app.status.label}] • ${TimeFormatter.format12Hour(app.startTime)}",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Booking employee name + status in small font right beside it!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👤 ${app.bookedByUserName}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color.Black.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = app.status.label,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveDayChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF37474F))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = Color(0xFF80CBC4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// Google Calendar Overlapping Layout Algorithm:
// Divides overlapping appointments into parallel side-by-side sub-columns!
fun layoutOverlappingAppointments(
    appointments: List<AppointmentEntity>,
    workStartHour: Int
): List<PositionedAppointment> {
    if (appointments.isEmpty()) return emptyList()

    // 1. Convert to minute intervals and sort by start time asc, then duration desc
    val intervals = appointments.map { app ->
        val (sH, sM) = parseTimeToHourAndMinute(app.startTime)
        val (eH, eM) = parseTimeToHourAndMinute(app.endTime)
        val sTotal = (sH - workStartHour) * 60 + sM
        val eTotal = (eH - workStartHour) * 60 + eM
        val duration = max(15, eTotal - sTotal)
        Triple(app, sTotal, sTotal + duration)
    }.sortedWith(compareBy({ it.second }, { -(it.third - it.second) }))

    // 2. Group into overlapping clusters
    val clusters = mutableListOf<MutableList<Triple<AppointmentEntity, Int, Int>>>()
    var currentCluster = mutableListOf<Triple<AppointmentEntity, Int, Int>>()
    var clusterEndTime = -1

    for (item in intervals) {
        if (currentCluster.isEmpty() || item.second < clusterEndTime) {
            currentCluster.add(item)
            clusterEndTime = max(clusterEndTime, item.third)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(item)
            clusterEndTime = item.third
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    // 3. For each cluster, assign sub-columns greedily
    val result = mutableListOf<PositionedAppointment>()
    for (cluster in clusters) {
        val columnEndTimes = mutableListOf<Int>()
        val itemColumns = mutableListOf<Pair<Triple<AppointmentEntity, Int, Int>, Int>>()

        for (item in cluster) {
            var assignedCol = -1
            for (col in columnEndTimes.indices) {
                if (columnEndTimes[col] <= item.second) {
                    assignedCol = col
                    columnEndTimes[col] = item.third
                    break
                }
            }
            if (assignedCol == -1) {
                assignedCol = columnEndTimes.size
                columnEndTimes.add(item.third)
            }
            itemColumns.add(item to assignedCol)
        }

        val totalCols = max(1, columnEndTimes.size)
        for ((item, col) in itemColumns) {
            val app = item.first
            val sTotal = item.second
            val duration = item.third - item.second
            result.add(
                PositionedAppointment(
                    appointment = app,
                    colIndex = col,
                    totalCols = totalCols,
                    startTotalMinutes = sTotal,
                    durationMinutes = duration
                )
            )
        }
    }
    return result
}

@Composable
private fun ZoomPresetPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ClinicPrimary else Color(0xFFEEEEEE))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF616161)
        )
    }
}

private fun parseTimeToHourAndMinute(timeStr: String): Pair<Int, Int> {
    return try {
        val parts = timeStr.split(":")
        Pair(parts[0].toInt(), parts[1].toInt())
    } catch (e: Exception) {
        Pair(10, 0)
    }
}
