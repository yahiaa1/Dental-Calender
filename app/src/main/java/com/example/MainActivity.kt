package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientClassification
import com.example.data.model.UserRole
import com.example.data.model.WhatsAppTemplateCategory
import com.example.ui.components.AppBottomBar
import com.example.ui.screens.appointment.AddEditAppointmentScreen
import com.example.ui.screens.appointment.AppointmentDetailsScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.calendar.CalendarMainScreen
import com.example.ui.screens.filter.FilterAppointmentsSheet
import com.example.ui.screens.patients.PatientClassificationScreen
import com.example.ui.screens.patients.PatientDetailsScreen
import com.example.ui.screens.patients.PatientsListScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.*
import com.example.ui.theme.DentalClinicTheme
import com.example.ui.viewmodel.ClinicScreen
import com.example.ui.viewmodel.ClinicViewModel
import com.example.utils.DateUtils
import com.example.utils.ReminderNotificationHelper
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            FirebaseSyncManager.init(this)
            ReminderNotificationHelper.createNotificationChannel(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Exception) {
            // Defensive protection against any initialization crash
        }

        setContent {
            DentalClinicTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DentalClinicApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun DentalClinicApp(
    viewModel: ClinicViewModel = viewModel()
) {
    val context = LocalContext.current
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val bottomNavIndex by viewModel.bottomNavIndex.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarViewMode by viewModel.calendarViewMode.collectAsStateWithLifecycle()

    val appointmentsForCalendar by viewModel.calendarVisibleAppointments.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val allPatients by viewModel.allPatients.collectAsStateWithLifecycle()
    val allProcedures by viewModel.allProcedures.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()
    val monthAppointmentDots by viewModel.monthAppointmentDots.collectAsStateWithLifecycle()
    val procedureColorsMap by viewModel.procedureColorsMap.collectAsStateWithLifecycle()

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val workStartHour by viewModel.workStartHour.collectAsStateWithLifecycle()
    val workEndHour by viewModel.workEndHour.collectAsStateWithLifecycle()
    val hourHeightDp by viewModel.hourHeightDp.collectAsStateWithLifecycle()
    val hideCancelled by viewModel.hideCancelledFromCalendar.collectAsStateWithLifecycle()
    val dayOff by viewModel.dayOff.collectAsStateWithLifecycle()
    val hideDayOff by viewModel.hideDayOff.collectAsStateWithLifecycle()
    val reportsSelectedDate by viewModel.reportsSelectedDate.collectAsStateWithLifecycle()

    val isFirebaseSyncing by viewModel.isFirebaseSyncing.collectAsStateWithLifecycle()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

    val activeAppointment by viewModel.activeAppointment.collectAsStateWithLifecycle()
    val activePatient by viewModel.activePatient.collectAsStateWithLifecycle()
    val isFilterSheetVisible by viewModel.isFilterSheetVisible.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()

    var showAdminOnlyAlert by remember { mutableStateOf(false) }

    // Admin Only Alert Dialog: "لايمكن دخول الاعدادات الا من يوزر admin فقط"
    if (showAdminOnlyAlert) {
        AlertDialog(
            onDismissRequest = { showAdminOnlyAlert = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("الدخول للإعدادات للأدمن فقط", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "عفواً! الدخول إلى قسم الإعدادات وتعديل إعدادات العيادة وصلاحيات الموظفين متاح فقط لحساب الأدمن الرئيسي (admin).\n\nبصفتك موظف، يمكنك حجز وإدارة المواعيد وتصفح سجلات المرضى والتقارير."
                )
            },
            confirmButton = {
                Button(onClick = { showAdminOnlyAlert = false }) {
                    Text("فهمت ذلك")
                }
            }
        )
    }

    // 1. Authentication Gate:
    // "اول ما يفتح البرنامج من الاندرويد يطلب من اليوزر والباسورد"
    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = { user ->
                viewModel.selectCurrentUser(user)
                viewModel.setAuthenticated(true)
            },
            onAttemptLogin = { u, p ->
                viewModel.attemptLogin(u, p)
            }
        )
        return
    }

    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent, isAuthenticated) {
        if (!isAuthenticated) return@LaunchedEffect
        val intent = activity?.intent ?: return@LaunchedEffect
        val action = intent.getStringExtra("action")
        val appointmentId = intent.getLongExtra("appointment_id", 0L)
        if (action == "add_appointment") {
            viewModel.openAddAppointment()
            activity.intent?.removeExtra("action")
        } else if (appointmentId > 0L) {
            viewModel.navigateTo(ClinicScreen.AppointmentDetails(appointmentId))
            activity.intent?.removeExtra("appointment_id")
        }
    }

    val isFilterActive = filterState.searchQuery.isNotBlank() ||
            filterState.date != null ||
            filterState.procedure != null ||
            filterState.status != null ||
            filterState.classification != null

    val isRootTab = currentScreen is ClinicScreen.MainCalendar ||
            currentScreen is ClinicScreen.PatientsList ||
            currentScreen is ClinicScreen.Reports ||
            currentScreen is ClinicScreen.Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isRootTab && !isFilterSheetVisible) {
                AppBottomBar(
                    selectedIndex = bottomNavIndex,
                    onItemSelected = { index ->
                        // Settings Protection:
                        // "لايمكن دخول الاعدادات الا من يوزر admin فقط"
                        if (index == 3 && currentUser.role != UserRole.ADMIN) {
                            showAdminOnlyAlert = true
                        } else {
                            viewModel.setBottomNavIndex(index)
                        }
                    },
                    onFabClicked = {
                        viewModel.openAddAppointment()
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is ClinicScreen.MainCalendar -> {
                    CalendarMainScreen(
                        selectedDate = selectedDate,
                        viewMode = calendarViewMode,
                        appointments = appointmentsForCalendar,
                        appointmentDots = monthAppointmentDots,
                        procedureColors = procedureColorsMap,
                        isFilterActive = isFilterActive,
                        workStartHour = workStartHour,
                        workEndHour = workEndHour,
                        hourHeightDp = hourHeightDp,
                        hideCancelled = hideCancelled,
                        hideDayOff = hideDayOff,
                        dayOff = dayOff,
                        currentUser = currentUser,
                        onLogout = { viewModel.logout() },
                        onToggleHideCancelled = { viewModel.toggleHideCancelled() },
                        onToggleHideDayOff = { viewModel.toggleHideDayOff() },
                        onZoomChange = { viewModel.setZoom(it) },
                        onViewModeChange = { viewModel.setCalendarViewMode(it) },
                        onDateSelected = { viewModel.setSelectedDate(it) },
                        onAppointmentClick = { viewModel.openAppointmentDetails(it) },
                        onTimeSlotSelected = { date, startTime, endTime ->
                            viewModel.openAddAppointment(
                                date = DateUtils.toDateString(date),
                                startTime = startTime,
                                endTime = endTime
                            )
                        },
                        onAppointmentMoved = { app, date, start, end ->
                            viewModel.moveAppointment(app, date, start, end)
                        },
                        onOpenFilter = { viewModel.isFilterSheetVisible.value = true },
                        onOpenNotionSync = { viewModel.navigateTo(ClinicScreen.NotionCalendarSync) }
                    )
                }

                is ClinicScreen.PatientsList -> {
                    BackHandler { viewModel.setBottomNavIndex(0) }
                    PatientsListScreen(
                        patients = allPatients,
                        onPatientClick = { patient ->
                            // Open patient profile & history instead of booking directly!
                            // "لما بشوف المريض من المرضى واضغط عليه بيجيب المواعيد علطول اني احجز مفروض اخش عليه واشوف بياناته"
                            viewModel.openPatientDetails(patient)
                        },
                        onWhatsAppClick = { viewModel.sendPatientWhatsApp(context, it) },
                        onAddNewPatient = { viewModel.savePatient(it) }
                    )
                }

                // Patient Profile & History Details Screen:
                is ClinicScreen.PatientDetails -> {
                    val patient = activePatient ?: allPatients.firstOrNull { it.id == screen.patientId }
                    if (patient != null) {
                        val patientAppointments = allAppointments.filter {
                            it.patientId == patient.id || it.patientName.equals(patient.name, ignoreCase = true)
                        }
                        PatientDetailsScreen(
                            patient = patient,
                            patientAppointments = patientAppointments,
                            onBack = { viewModel.navigateTo(ClinicScreen.PatientsList) },
                            onBookNewAppointment = { p ->
                                viewModel.openAddAppointment(prefillPatient = p)
                            },
                            onAppointmentClick = { app ->
                                viewModel.openAppointmentDetails(app)
                            },
                            onUpdatePatient = { updated ->
                                viewModel.updatePatient(updated)
                            }
                        )
                    } else {
                        viewModel.navigateTo(ClinicScreen.PatientsList)
                    }
                }

                is ClinicScreen.Reports -> {
                    BackHandler { viewModel.setBottomNavIndex(0) }
                    ReportsScreen(
                        appointments = allAppointments,
                        patients = allPatients,
                        selectedReportDate = reportsSelectedDate,
                        onSelectReportDate = { viewModel.setReportsSelectedDate(it) }
                    )
                }

                is ClinicScreen.Settings -> {
                    BackHandler { viewModel.setBottomNavIndex(0) }
                    // Extra security check for settings screen
                    if (currentUser.role != UserRole.ADMIN) {
                        LaunchedEffect(Unit) {
                            showAdminOnlyAlert = true
                            viewModel.setBottomNavIndex(0)
                        }
                    } else {
                        ClinicSettingsScreen(
                            hideCancelled = hideCancelled,
                            onToggleHideCancelled = { viewModel.toggleHideCancelled() },
                            dayOff = dayOff,
                            hideDayOff = hideDayOff,
                            onSelectDayOff = { viewModel.setDayOff(it) },
                            onToggleHideDayOff = { viewModel.toggleHideDayOff() },
                            onOpenProcedureColors = { viewModel.navigateTo(ClinicScreen.ProcedureColors) },
                            onOpenWhatsAppTemplates = { viewModel.navigateTo(ClinicScreen.WhatsAppTemplates) },
                            onOpenClassificationGuide = { viewModel.navigateTo(ClinicScreen.PatientClassificationGuide()) },
                            onOpenNotionSync = { viewModel.navigateTo(ClinicScreen.NotionCalendarSync) },
                            onOpenWorkingHours = { viewModel.navigateTo(ClinicScreen.WorkingHoursSettings) },
                            onOpenFirebaseSync = { viewModel.navigateTo(ClinicScreen.FirebaseSync) },
                            onOpenUsersManagement = { viewModel.navigateTo(ClinicScreen.UserManagement) },
                            onOpenWhatsAppReminders = { viewModel.navigateTo(ClinicScreen.WhatsAppReminderSettings) },
                            onClearAllAppointments = { viewModel.clearAllAppointments() },
                            onClearAllPatients = { viewModel.clearAllPatients() }
                        )
                    }
                }

                is ClinicScreen.AppointmentDetails -> {
                    val app = activeAppointment ?: allAppointments.firstOrNull { it.id == screen.appointmentId }
                    if (app != null) {
                        AppointmentDetailsScreen(
                            appointment = app,
                            currentUser = currentUser,
                            onBack = { viewModel.navigateTo(ClinicScreen.MainCalendar) },
                            onEdit = { viewModel.openEditAppointment(app) },
                            onDelete = {
                                viewModel.deleteAppointment(app)
                                viewModel.navigateTo(ClinicScreen.MainCalendar)
                            },
                            onStatusChange = { newStatus ->
                                viewModel.updateAppointmentStatus(app.id, newStatus)
                            },
                            onClassificationChange = { newClassification ->
                                viewModel.updatePatientClassification(app.patientId, newClassification)
                            },
                            onOpenClassificationGuide = {
                                viewModel.navigateTo(ClinicScreen.PatientClassificationGuide(app.patientId))
                            },
                            onSendOneHourReminder = {
                                viewModel.sendOneHourWhatsAppReminder(context, app)
                            }
                        )
                    } else {
                        viewModel.navigateTo(ClinicScreen.MainCalendar)
                    }
                }

                is ClinicScreen.AddEditAppointment -> {
                    val existing = if (screen.appointmentId != null) {
                        activeAppointment ?: allAppointments.firstOrNull { it.id == screen.appointmentId }
                    } else null

                    AddEditAppointmentScreen(
                        existingAppointment = existing,
                        initialDateStr = screen.initialDate ?: DateUtils.toDateString(selectedDate),
                        initialStartTime = screen.initialStartTime,
                        initialEndTime = screen.initialEndTime,
                        patientsList = allPatients,
                        proceduresList = allProcedures,
                        currentUser = currentUser,
                        prefillPatientId = screen.prefillPatientId,
                        prefillPatientName = screen.prefillPatientName,
                        prefillPatientPhone = screen.prefillPatientPhone,
                        prefillClassification = screen.prefillClassification,
                        onSave = { appointment ->
                            viewModel.saveAppointment(appointment)
                            viewModel.navigateTo(ClinicScreen.MainCalendar)
                        },
                        onCancel = { viewModel.navigateTo(ClinicScreen.MainCalendar) }
                    )
                }

                is ClinicScreen.PatientClassificationGuide -> {
                    PatientClassificationScreen(
                        currentClassification = PatientClassification.DISCIPLINED,
                        onSelectClassification = {
                            viewModel.navigateTo(ClinicScreen.MainCalendar)
                        },
                        onBack = { viewModel.navigateTo(ClinicScreen.MainCalendar) }
                    )
                }

                is ClinicScreen.ProcedureColors -> {
                    ProcedureColorsScreen(
                        procedures = allProcedures,
                        onAddProcedure = { viewModel.addProcedure(it) },
                        onUpdateProcedure = { viewModel.updateProcedure(it) },
                        onDeleteProcedure = { viewModel.deleteProcedure(it) },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.WhatsAppTemplates -> {
                    WhatsAppTemplatesScreen(
                        templates = allTemplates,
                        onSaveTemplate = { viewModel.saveWhatsAppTemplate(it) },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.NotionCalendarSync -> {
                    NotionCalendarSyncScreen(
                        appointments = allAppointments,
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.WorkingHoursSettings -> {
                    WorkingHoursSettingsScreen(
                        currentStartHour = workStartHour,
                        currentEndHour = workEndHour,
                        onSaveHours = { start, end ->
                            viewModel.setWorkingHours(start, end)
                        },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.FirebaseSync -> {
                    FirebaseSyncScreen(
                        isSyncing = isFirebaseSyncing,
                        syncStatusMessage = firebaseSyncStatus,
                        onBackupToFirebase = { viewModel.backupToFirebase() },
                        onRestoreFromFirebase = { viewModel.restoreFromFirebase() },
                        onTestConnection = { onResult ->
                            viewModel.testFirebaseConnection(context, onResult)
                        },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.UserManagement -> {
                    UserManagementScreen(
                        currentUser = currentUser,
                        allUsers = allUsers,
                        onSelectCurrentUser = { viewModel.selectCurrentUser(it) },
                        onAddUser = { viewModel.addUser(it) },
                        onUpdateUser = { viewModel.updateUser(it) },
                        onDeleteUser = { viewModel.deleteUser(it) },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }

                is ClinicScreen.WhatsAppReminderSettings -> {
                    val template = allTemplates.firstOrNull { it.category == WhatsAppTemplateCategory.ONE_HOUR_BEFORE }
                    WhatsAppReminderSettingsScreen(
                        currentTemplate = template,
                        onSaveTemplate = { viewModel.saveWhatsAppTemplate(it) },
                        onBack = { viewModel.navigateTo(ClinicScreen.Settings) }
                    )
                }
            }

            // Filter Sheet Overlay
            if (isFilterSheetVisible) {
                FilterAppointmentsSheet(
                    initialFilter = filterState,
                    procedures = allProcedures,
                    onApply = { newFilter -> viewModel.applyFilter(newFilter) },
                    onReset = { viewModel.resetFilter() },
                    onClose = { viewModel.isFilterSheetVisible.value = false }
                )
            }
        }
    }
}
