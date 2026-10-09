package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.receiver.TodayAppointmentsWidgetProvider
import com.example.utils.AppointmentAlarmScheduler
import com.example.utils.ClinicActions
import com.example.utils.DateUtils
import com.example.utils.NotionCalendarHelper
import com.example.utils.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ClinicScreen {
    object MainCalendar : ClinicScreen()
    object PatientsList : ClinicScreen()
    object Reports : ClinicScreen()
    object Settings : ClinicScreen()
    data class PatientDetails(val patientId: Long) : ClinicScreen()
    data class AppointmentDetails(val appointmentId: Long) : ClinicScreen()
    data class AddEditAppointment(
        val appointmentId: Long? = null,
        val initialDate: String? = null,
        val initialStartTime: String = "10:00",
        val initialEndTime: String = "10:45",
        val prefillPatientId: Long? = null,
        val prefillPatientName: String? = null,
        val prefillPatientPhone: String? = null,
        val prefillClassification: PatientClassification? = null
    ) : ClinicScreen()
    data class PatientClassificationGuide(val patientId: Long? = null) : ClinicScreen()
    object ProcedureColors : ClinicScreen()
    object WhatsAppTemplates : ClinicScreen()
    object NotionCalendarSync : ClinicScreen()
    object WorkingHoursSettings : ClinicScreen()
    object FirebaseSync : ClinicScreen()
    object UserManagement : ClinicScreen()
    object WhatsAppReminderSettings : ClinicScreen()
    object ActionLogs : ClinicScreen()
}

data class FilterState(
    val searchQuery: String = "",
    val date: LocalDate? = null,
    val procedure: String? = null,
    val status: AppointmentStatus? = null,
    val classification: PatientClassification? = null
)

class ClinicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClinicRepository
    private val authPrefs = application.getSharedPreferences("clinic_auth_prefs", Context.MODE_PRIVATE)

    // Check whether the app currently has a genuinely authenticated user session
    private fun checkIsAuthenticated(): Boolean {
        // Enforce session versioning: if auth_version < 2, purge any legacy/stale default admin session from older APKs
        val authVersion = authPrefs.getInt("auth_version", 0)
        if (authVersion < 2) {
            authPrefs.edit()
                .putInt("auth_version", 2)
                .putBoolean("is_logged_in", false)
                .remove("logged_in_username")
                .remove("logged_in_user_id")
                .remove("logged_in_user_name")
                .remove("logged_in_user_phone")
                .remove("logged_in_user_role")
                .apply()
            return false
        }

        val isLoggedIn = authPrefs.getBoolean("is_logged_in", false)
        val savedUsername = authPrefs.getString("logged_in_username", null)
        if (!isLoggedIn || savedUsername.isNullOrBlank()) {
            return false
        }
        return true
    }

    // User Authentication State: Strict check ensuring app always opens on LoginScreen on fresh install or update
    val isAuthenticated = MutableStateFlow(checkIsAuthenticated())

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ClinicRepository(
            database.patientDao(),
            database.appointmentDao(),
            database.procedureConfigDao(),
            database.whatsappTemplateDao(),
            database.userDao(),
            database.actionLogDao()
        )

        // Ensure fixed admin user exists in DB:
        // "اليوزر بتاع الadmin يكون ثابت ك admin والباسورد 1234655"
        viewModelScope.launch(Dispatchers.IO) {
            val adminUser = repository.getUserByUsername("admin")
            if (adminUser == null) {
                val defaultAdmin = UserEntity(
                    id = 1,
                    name = "د. محمد (أدمن)",
                    phone = "01000000001",
                    role = UserRole.ADMIN,
                    username = "admin",
                    password = "1234655"
                )
                repository.insertUser(defaultAdmin)
                FirebaseSyncManager.autoSyncUser(defaultAdmin)
            }

            // Restore all users from Cloud Firestore to local DB immediately on app launch
            FirebaseSyncManager.syncUsersFromFirebase(repository)

            // Restore saved logged-in user if available and authenticated
            val isLoggedIn = checkIsAuthenticated()
            val savedUsername = authPrefs.getString("logged_in_username", null)
            if (isLoggedIn && !savedUsername.isNullOrBlank()) {
                val user = repository.getUserByUsername(savedUsername)
                if (user != null) {
                    currentUser.value = user
                }
            }
        }

        // Realtime automatic sync across all clinic devices for entities via Firebase Firestore:
        // Display toggles (hideDayOff / hideCancelled) are NOT synced remotely so each user/device is independent
        FirebaseSyncManager.startAllRealtimeListeners(
            application,
            repository,
            onSettingsChanged = { remoteStart, remoteEnd, remoteDayOff ->
                workStartHour.value = remoteStart
                workEndHour.value = remoteEnd
                dayOff.value = remoteDayOff
                settingsPrefs.edit()
                    .putInt("work_start_hour", remoteStart)
                    .putInt("work_end_hour", remoteEnd)
                    .putString("day_off", remoteDayOff?.name ?: "NONE")
                    .apply()
            }
        )

        // Automatically ensure upcoming appointments have alarms scheduled for 1 hour before
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.allAppointments.collect { list ->
                    AppointmentAlarmScheduler.scheduleAllUpcoming(application, list)
                }
            } catch (e: Exception) {
                // Ignore safely
            }
        }
    }

    // Default to current date
    val selectedDate = MutableStateFlow(LocalDate.now())
    val calendarViewMode = MutableStateFlow(CalendarViewMode.THREE_DAYS)
    val currentScreen = MutableStateFlow<ClinicScreen>(ClinicScreen.MainCalendar)
    val bottomNavIndex = MutableStateFlow(0) // 0=Calendar, 1=Patients, 2=Reports, 3=Settings

    private val settingsPrefs = application.getSharedPreferences("clinic_settings_prefs", Context.MODE_PRIVATE)

    // Clinic Working Hours configuration: Persisted across app restarts
    val workStartHour = MutableStateFlow(settingsPrefs.getInt("work_start_hour", 9)) // Default 09:00
    val workEndHour = MutableStateFlow(settingsPrefs.getInt("work_end_hour", 21))    // Default 21:00

    // Zoom level: minimum boundary is 80f, max 160f
    val hourHeightDp = MutableStateFlow(85f)

    // Option to hide cancelled appointments from calendar view - Individual per device preference
    val hideCancelledFromCalendar = MutableStateFlow(settingsPrefs.getBoolean("hide_cancelled", true))

    // Day Off configuration: Default is FRIDAY (Shared clinic schedule)
    val dayOff = MutableStateFlow<DayOfWeek?>(
        settingsPrefs.getString("day_off", "FRIDAY")?.let {
            if (it == "NONE") null else try { DayOfWeek.valueOf(it) } catch (e: Exception) { DayOfWeek.FRIDAY }
        } ?: DayOfWeek.FRIDAY
    )

    // Option to hide/show the day off from calendar view - Individual per device preference
    val hideDayOff = MutableStateFlow(settingsPrefs.getBoolean("hide_day_off", false))

    fun setDayOff(day: DayOfWeek?) {
        dayOff.value = day
        settingsPrefs.edit().putString("day_off", day?.name ?: "NONE").apply()
        syncClinicSettings()
    }

    fun toggleHideDayOff() {
        val next = !hideDayOff.value
        hideDayOff.value = next
        settingsPrefs.edit().putBoolean("hide_day_off", next).apply()
        if (next && dayOff.value != null && selectedDate.value.dayOfWeek == dayOff.value) {
            selectedDate.value = selectedDate.value.plusDays(1)
        }
        // Independent local device preference - Do not sync to Firestore
    }

    fun setHideDayOff(hide: Boolean) {
        hideDayOff.value = hide
        settingsPrefs.edit().putBoolean("hide_day_off", hide).apply()
        if (hide && dayOff.value != null && selectedDate.value.dayOfWeek == dayOff.value) {
            selectedDate.value = selectedDate.value.plusDays(1)
        }
        // Independent local device preference - Do not sync to Firestore
    }

    private fun syncClinicSettings() {
        FirebaseSyncManager.autoSyncSettings(
            workStartHour = workStartHour.value,
            workEndHour = workEndHour.value,
            dayOff = dayOff.value
        )
    }

    // Reports selected date
    val reportsSelectedDate = MutableStateFlow(LocalDate.now())

    // Firebase Sync State
    val isFirebaseSyncing = MutableStateFlow(false)
    val firebaseSyncStatus = MutableStateFlow<String?>(null)

    // Filter Sheet state
    val isFilterSheetVisible = MutableStateFlow(false)
    val filterState = MutableStateFlow(FilterState())

    // Currently selected appointment and patient for details
    val activeAppointment = MutableStateFlow<AppointmentEntity?>(null)
    val activePatient = MutableStateFlow<PatientEntity?>(null)

    // User & Staff Management State
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun getInitialUser(): UserEntity {
        val isLoggedIn = checkIsAuthenticated()
        if (isLoggedIn) {
            val savedUsername = authPrefs.getString("logged_in_username", null)
            if (!savedUsername.isNullOrBlank()) {
                val savedId = authPrefs.getLong("logged_in_user_id", 0L)
                val savedName = authPrefs.getString("logged_in_user_name", savedUsername) ?: savedUsername
                val savedPhone = authPrefs.getString("logged_in_user_phone", "") ?: ""
                val savedRoleStr = authPrefs.getString("logged_in_user_role", UserRole.STAFF.name)
                val role = try {
                    UserRole.valueOf(savedRoleStr ?: UserRole.STAFF.name)
                } catch (e: Exception) {
                    UserRole.STAFF
                }
                return UserEntity(
                    id = savedId,
                    name = savedName,
                    phone = savedPhone,
                    role = role,
                    username = savedUsername,
                    password = ""
                )
            }
        }
        // Guest placeholder when unauthenticated - Never default to Admin without explicit login!
        return UserEntity(0, "حساب زائر", "", UserRole.STAFF, username = "", password = "")
    }

    val currentUser = MutableStateFlow(getInitialUser())

    // Data streams from Room (Kept hot in memory for instantaneous rendering)
    val allAppointments: StateFlow<List<AppointmentEntity>> = repository.allAppointments
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPatients: StateFlow<List<PatientEntity>> = repository.allPatients
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allProcedures: StateFlow<List<ProcedureConfigEntity>> = repository.allProcedures
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTemplates: StateFlow<List<WhatsAppTemplateEntity>> = repository.allTemplates
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allActionLogs: StateFlow<List<ActionLogEntity>> = repository.allActionLogs
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val calendarVisibleAppointments: StateFlow<List<AppointmentEntity>> = combine(
        allAppointments,
        hideCancelledFromCalendar,
        filterState
    ) { appointments, hideCancelled, filter ->
        appointments.filter { app ->
            if (hideCancelled && (app.status == AppointmentStatus.CANCELLED || app.status == AppointmentStatus.NO_SHOW)) {
                return@filter false
            }

            val matchQuery = if (filter.searchQuery.isNotBlank()) {
                app.patientName.contains(filter.searchQuery, ignoreCase = true) ||
                        app.patientPhone.contains(filter.searchQuery)
            } else true

            val matchProcedure = if (!filter.procedure.isNullOrBlank()) app.procedureName == filter.procedure else true
            val matchStatus = if (filter.status != null) app.status == filter.status else true
            val matchClass = if (filter.classification != null) app.patientClassification == filter.classification else true

            matchQuery && matchProcedure && matchStatus && matchClass
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthAppointmentDots: StateFlow<Map<String, List<AppointmentStatus>>> = calendarVisibleAppointments
        .map { list -> list.groupBy({ it.dateStr }, { it.status }) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val procedureColorsMap: StateFlow<Map<String, String>> = allProcedures
        .map { list -> list.associate { it.name to it.colorHex } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Login Method:
    suspend fun attemptLogin(username: String, pass: String): UserEntity? {
        val cleanUsername = username.trim()
        val cleanPass = pass.trim()

        // 1. Fixed Admin account verification
        if (cleanUsername.equals("admin", ignoreCase = true) && cleanPass == "1234655") {
            var admin = repository.getUserByUsername("admin")
            if (admin == null) {
                val newAdmin = UserEntity(
                    id = 1,
                    name = "د. محمد (أدمن)",
                    phone = "01000000001",
                    role = UserRole.ADMIN,
                    username = "admin",
                    password = "1234655"
                )
                repository.insertUser(newAdmin)
                FirebaseSyncManager.autoSyncUser(newAdmin)
                admin = newAdmin
            }
            selectCurrentUser(admin)
            isAuthenticated.value = true
            return admin
        }

        // 2. Custom staff accounts verification
        var user = repository.getUserByUsername(cleanUsername)
        if (user == null) {
            // If user is not yet loaded into local Room (e.g. freshly installed app), sync from Firebase first
            try {
                FirebaseSyncManager.syncUsersFromFirebase(repository)
                user = repository.getUserByUsername(cleanUsername)
            } catch (e: Exception) {
                // Ignore safely
            }
        }
        if (user != null && user.password == cleanPass) {
            selectCurrentUser(user)
            isAuthenticated.value = true
            return user
        }
        return null
    }

    fun logout() {
        isAuthenticated.value = false
        authPrefs.edit()
            .putInt("auth_version", 2)
            .putBoolean("is_logged_in", false)
            .remove("logged_in_username")
            .remove("logged_in_user_id")
            .remove("logged_in_user_name")
            .remove("logged_in_user_phone")
            .remove("logged_in_user_role")
            .apply()
        currentUser.value = UserEntity(0, "حساب زائر", "", UserRole.STAFF, username = "", password = "")
        currentScreen.value = ClinicScreen.MainCalendar
        bottomNavIndex.value = 0
    }

    fun setSelectedDate(date: LocalDate) {
        val adjusted = if (hideDayOff.value && dayOff.value != null && date.dayOfWeek == dayOff.value) {
            date.plusDays(1)
        } else {
            date
        }
        selectedDate.value = adjusted
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        calendarViewMode.value = mode
    }

    fun navigateTo(screen: ClinicScreen) {
        currentScreen.value = screen
    }

    fun setBottomNavIndex(index: Int) {
        bottomNavIndex.value = index
        when (index) {
            0 -> currentScreen.value = ClinicScreen.MainCalendar
            1 -> currentScreen.value = ClinicScreen.PatientsList
            2 -> currentScreen.value = ClinicScreen.Reports
            3 -> currentScreen.value = ClinicScreen.Settings
        }
    }

    fun selectCurrentUser(user: UserEntity) {
        currentUser.value = user
        authPrefs.edit()
            .putInt("auth_version", 2)
            .putBoolean("is_logged_in", true)
            .putString("logged_in_username", user.username)
            .putLong("logged_in_user_id", user.id)
            .putString("logged_in_user_name", user.name)
            .putString("logged_in_user_phone", user.phone)
            .putString("logged_in_user_role", user.role.name)
            .apply()
    }

    fun setAuthenticated(authenticated: Boolean) {
        isAuthenticated.value = authenticated
        authPrefs.edit()
            .putInt("auth_version", 2)
            .putBoolean("is_logged_in", authenticated)
            .apply()
    }

    fun addUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserByUsername(user.username.trim().lowercase())
            val finalId = when {
                existing != null -> existing.id
                user.id == 0L -> FirebaseSyncManager.generateUniqueId()
                else -> user.id
            }
            val finalUser = user.copy(
                id = finalId,
                username = user.username.trim().lowercase()
            )
            FirebaseSyncManager.markLocalUpdate("user", finalId)
            repository.insertUser(finalUser)
            FirebaseSyncManager.autoSyncUser(finalUser)
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseSyncManager.markLocalUpdate("user", user.id)
            repository.updateUser(user)
            FirebaseSyncManager.autoSyncUser(user)
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUserById(userId)
            FirebaseSyncManager.autoDeleteUser(userId)
        }
    }

    fun setZoom(height: Float) {
        hourHeightDp.value = height.coerceIn(80f, 160f)
    }

    fun setWorkingHours(start: Int, end: Int) {
        if (start < end) {
            workStartHour.value = start
            workEndHour.value = end
            settingsPrefs.edit()
                .putInt("work_start_hour", start)
                .putInt("work_end_hour", end)
                .apply()
            syncClinicSettings()
        }
    }

    fun toggleHideCancelled() {
        val next = !hideCancelledFromCalendar.value
        hideCancelledFromCalendar.value = next
        settingsPrefs.edit().putBoolean("hide_cancelled", next).apply()
        // Independent local device preference - Do not sync to Firestore
    }

    fun setReportsSelectedDate(date: LocalDate) {
        reportsSelectedDate.value = date
    }

    fun openAppointmentDetails(appointment: AppointmentEntity) {
        activeAppointment.value = appointment
        currentScreen.value = ClinicScreen.AppointmentDetails(appointment.id)
    }

    fun openPatientDetails(patient: PatientEntity) {
        activePatient.value = patient
        currentScreen.value = ClinicScreen.PatientDetails(patient.id)
    }

    fun openAddAppointment(
        date: String? = null,
        startTime: String = "10:00",
        endTime: String = "10:45",
        prefillPatient: PatientEntity? = null
    ) {
        activeAppointment.value = null
        currentScreen.value = ClinicScreen.AddEditAppointment(
            initialDate = date ?: DateUtils.toDateString(selectedDate.value),
            initialStartTime = startTime,
            initialEndTime = endTime,
            prefillPatientId = prefillPatient?.id,
            prefillPatientName = prefillPatient?.name,
            prefillPatientPhone = prefillPatient?.phone,
            prefillClassification = prefillPatient?.classification
        )
    }

    fun openEditAppointment(appointment: AppointmentEntity) {
        activeAppointment.value = appointment
        currentScreen.value = ClinicScreen.AddEditAppointment(
            appointmentId = appointment.id,
            initialDate = appointment.dateStr,
            initialStartTime = appointment.startTime,
            initialEndTime = appointment.endTime
        )
    }

    fun saveAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPatients = repository.allPatients.first()
            val existingPatient = currentPatients.firstOrNull {
                it.name.trim().equals(appointment.patientName.trim(), ignoreCase = true) ||
                        (appointment.patientPhone.isNotBlank() && it.phone.trim() == appointment.patientPhone.trim())
            }

            val finalPatientId = if (existingPatient != null) {
                if (existingPatient.classification != appointment.patientClassification) {
                    val updatedP = existingPatient.copy(classification = appointment.patientClassification)
                    FirebaseSyncManager.markLocalUpdate("patient", updatedP.id)
                    repository.updatePatient(updatedP)
                    FirebaseSyncManager.autoSyncPatient(updatedP)
                }
                existingPatient.id
            } else {
                val newPatientId = FirebaseSyncManager.generateUniqueId()
                val newP = PatientEntity(
                    id = newPatientId,
                    name = appointment.patientName.trim(),
                    phone = appointment.patientPhone.trim(),
                    classification = appointment.patientClassification,
                    notes = "تم تسجيله تلقائياً عند حجز الموعد: ${appointment.procedureName}"
                )
                FirebaseSyncManager.markLocalUpdate("patient", newPatientId)
                repository.insertPatient(newP)
                FirebaseSyncManager.autoSyncPatient(newP)
                newPatientId
            }

            val isNew = appointment.id == 0L
            val finalAppId = if (isNew) FirebaseSyncManager.generateUniqueId() else appointment.id
            val finalSavedApp = appointment.copy(id = finalAppId, patientId = finalPatientId)

            FirebaseSyncManager.markLocalUpdate("appointment", finalAppId)
            if (isNew) {
                repository.insertAppointment(finalSavedApp)
            } else {
                repository.updateAppointment(finalSavedApp)
            }
            activeAppointment.value = finalSavedApp

            if (isNew) {
                logAction(
                    actionType = ActionLogType.BOOKING,
                    appointment = finalSavedApp,
                    details = "قام ${currentUser.value.name} بحجز موعد جديد للمريض ${finalSavedApp.patientName} (${finalSavedApp.procedureName}) يوم ${finalSavedApp.dateStr} الساعة ${TimeFormatter.format12Hour(finalSavedApp.startTime)}",
                    extraInfo = "حجز جديد"
                )
            } else {
                logAction(
                    actionType = ActionLogType.EDIT,
                    appointment = finalSavedApp,
                    details = "قام ${currentUser.value.name} بتعديل بيانات موعد المريض ${finalSavedApp.patientName} (${finalSavedApp.procedureName}) ليوم ${finalSavedApp.dateStr} الساعة ${TimeFormatter.format12Hour(finalSavedApp.startTime)}",
                    extraInfo = "تعديل موعد"
                )
            }

            FirebaseSyncManager.autoSyncAppointment(finalSavedApp)

            val reminderTemplate = repository.getTemplateByCategory(WhatsAppTemplateCategory.ONE_HOUR_BEFORE)
            val templateText = reminderTemplate?.messageText
                ?: "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}."
            val time12 = TimeFormatter.format12Hour(finalSavedApp.startTime)
            val reminderMessage = templateText
                .replace("{اسم المريض}", finalSavedApp.patientName)
                .replace("{وقت الموعد}", time12)
                .replace("{تاريخ الموعد}", finalSavedApp.dateStr)
                .replace("{اسم العيادة}", "عيادة الأسنان")

            AppointmentAlarmScheduler.scheduleOneHourReminder(
                context = getApplication(),
                appointment = finalSavedApp,
                customMessage = reminderMessage
            )
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun moveAppointment(
        appointment: AppointmentEntity,
        newDate: String,
        newStartTime: String,
        newEndTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = appointment.copy(
                dateStr = newDate,
                startTime = newStartTime,
                endTime = newEndTime
            )
            FirebaseSyncManager.markLocalUpdate("appointment", updated.id)
            repository.updateAppointment(updated)
            activeAppointment.value = updated

            logAction(
                actionType = ActionLogType.MOVE,
                appointment = updated,
                details = "قام ${currentUser.value.name} بنقل موعد المريض ${appointment.patientName} من تاريخ ${appointment.dateStr} (${TimeFormatter.format12Hour(appointment.startTime)}) إلى تاريخ $newDate (${TimeFormatter.format12Hour(newStartTime)})",
                extraInfo = "من ${appointment.dateStr} إلى $newDate"
            )

            FirebaseSyncManager.autoSyncAppointment(updated)

            AppointmentAlarmScheduler.cancelReminder(getApplication(), updated.id)
            val reminderTemplate = repository.getTemplateByCategory(WhatsAppTemplateCategory.ONE_HOUR_BEFORE)
            val templateText = reminderTemplate?.messageText
                ?: "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}."
            val time12 = TimeFormatter.format12Hour(updated.startTime)
            val reminderMessage = templateText
                .replace("{اسم المريض}", updated.patientName)
                .replace("{وقت الموعد}", time12)
                .replace("{تاريخ الموعد}", updated.dateStr)
                .replace("{اسم العيادة}", "عيادة الأسنان")

            AppointmentAlarmScheduler.scheduleOneHourReminder(
                context = getApplication(),
                appointment = updated,
                customMessage = reminderMessage
            )
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun updateAppointmentStatus(appointmentId: Long, newStatus: AppointmentStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldApp = repository.getAppointmentById(appointmentId)
            FirebaseSyncManager.markLocalUpdate("appointment", appointmentId)
            repository.updateAppointmentStatus(appointmentId, newStatus)
            val updated = repository.getAppointmentById(appointmentId)
            if (updated != null) {
                if (activeAppointment.value?.id == appointmentId) {
                    activeAppointment.value = updated
                }
                FirebaseSyncManager.autoSyncAppointment(updated)

                val oldStatusName = oldApp?.status?.label ?: ""
                logAction(
                    actionType = ActionLogType.STATUS_CHANGE,
                    appointment = updated,
                    details = "قام ${currentUser.value.name} بتغيير حالة موعد المريض ${updated.patientName} من '$oldStatusName' إلى '${newStatus.label}'",
                    extraInfo = "الحالة: ${newStatus.label}"
                )
            }
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            logAction(
                actionType = ActionLogType.DELETE,
                appointment = appointment,
                details = "قام ${currentUser.value.name} بحذف/إلغاء موعد المريض ${appointment.patientName} المحدد في تاريخ ${appointment.dateStr} (${TimeFormatter.format12Hour(appointment.startTime)})",
                extraInfo = "إلغاء الموعد"
            )
            AppointmentAlarmScheduler.cancelReminder(getApplication(), appointment.id)
            repository.deleteAppointment(appointment)
            repository.deleteAppointmentById(appointment.id)
            FirebaseSyncManager.autoDeleteAppointment(appointment.id)
            if (activeAppointment.value?.id == appointment.id) {
                activeAppointment.value = null
            }
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    private suspend fun logAction(
        actionType: ActionLogType,
        appointment: AppointmentEntity,
        details: String,
        extraInfo: String = ""
    ) {
        try {
            val user = currentUser.value
            val now = LocalDateTime.now()
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeStr = now.format(timeFormatter)
            val dateStr = now.format(dateFormatter)

            val logId = FirebaseSyncManager.generateUniqueId()
            val log = ActionLogEntity(
                id = logId,
                dateStr = dateStr,
                timeStr = timeStr,
                actionType = actionType,
                employeeId = user.id,
                employeeName = user.name,
                employeeRole = user.role.title,
                patientName = appointment.patientName,
                patientPhone = appointment.patientPhone,
                appointmentId = appointment.id,
                appointmentDate = appointment.dateStr,
                appointmentTime = appointment.startTime,
                procedureName = appointment.procedureName,
                details = details,
                extraInfo = extraInfo
            )
            repository.insertActionLog(log)
            FirebaseSyncManager.autoSyncActionLog(log)
        } catch (e: Exception) {
            // Guard safely
        }
    }

    fun clearActionLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllActionLogs()
        }
    }

    fun deleteActionLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteActionLogById(id)
            FirebaseSyncManager.autoDeleteActionLog(id)
        }
    }

    fun savePatient(patient: PatientEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNew = patient.id == 0L
            val finalId = if (isNew) FirebaseSyncManager.generateUniqueId() else patient.id
            val finalPatient = patient.copy(id = finalId)
            FirebaseSyncManager.markLocalUpdate("patient", finalId)
            if (isNew) {
                repository.insertPatient(finalPatient)
            } else {
                repository.updatePatient(finalPatient)
            }
            FirebaseSyncManager.autoSyncPatient(finalPatient)
            onComplete?.invoke(finalId)
        }
    }

    fun updatePatient(patient: PatientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseSyncManager.markLocalUpdate("patient", patient.id)
            repository.updatePatient(patient)
            FirebaseSyncManager.autoSyncPatient(patient)
            if (activePatient.value?.id == patient.id) {
                activePatient.value = patient
            }
        }
    }

    fun deletePatient(patient: PatientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePatient(patient)
            FirebaseSyncManager.autoDeletePatient(patient.id)
            if (activePatient.value?.id == patient.id) {
                activePatient.value = null
            }
        }
    }

    fun addProcedure(procedure: ProcedureConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNew = procedure.id == 0L
            val finalId = if (isNew) FirebaseSyncManager.generateUniqueId() else procedure.id
            val finalProc = procedure.copy(id = finalId)
            FirebaseSyncManager.markLocalUpdate("procedure", finalId)
            if (isNew) {
                repository.insertProcedure(finalProc)
            } else {
                repository.updateProcedure(finalProc)
            }
            FirebaseSyncManager.autoSyncProcedure(finalProc)
        }
    }

    fun updateProcedure(procedure: ProcedureConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseSyncManager.markLocalUpdate("procedure", procedure.id)
            repository.updateProcedure(procedure)
            FirebaseSyncManager.autoSyncProcedure(procedure)
        }
    }

    fun deleteProcedure(procedureId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProcedureById(procedureId)
            FirebaseSyncManager.autoDeleteProcedure(procedureId)
        }
    }

    fun updatePatientClassification(patientId: Long, classification: PatientClassification) {
        viewModelScope.launch(Dispatchers.IO) {
            val patient = repository.getPatientById(patientId)
            if (patient != null) {
                val updated = patient.copy(classification = classification)
                repository.updatePatient(updated)
                FirebaseSyncManager.autoSyncPatient(updated)
                if (activePatient.value?.id == patientId) {
                    activePatient.value = updated
                }
            }
            activeAppointment.value?.let { app ->
                if (app.patientId == patientId) {
                    val updated = app.copy(patientClassification = classification)
                    repository.updateAppointment(updated)
                    FirebaseSyncManager.autoSyncAppointment(updated)
                    activeAppointment.value = updated
                }
            }
        }
    }

    fun clearAllAppointments() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAppointments()
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun clearAllPatients() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPatients()
        }
    }

    fun updateProcedureColor(procedureName: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProcedureColor(procedureName, colorHex)
            val updated = repository.allProcedures.first().firstOrNull { it.name == procedureName }
            if (updated != null) {
                FirebaseSyncManager.autoSyncProcedure(updated)
            }
        }
    }

    fun saveWhatsAppTemplate(template: WhatsAppTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getTemplateByCategory(template.category)
            val finalId = if (template.id > 0L) {
                template.id
            } else if (existing != null && existing.id > 0L) {
                existing.id
            } else {
                FirebaseSyncManager.generateUniqueId()
            }
            val finalTemplate = template.copy(id = finalId)
            repository.saveTemplate(finalTemplate)
            FirebaseSyncManager.autoSyncWhatsAppTemplate(finalTemplate)
        }
    }

    fun getDefaultTemplateText(category: WhatsAppTemplateCategory): String {
        return when (category) {
            WhatsAppTemplateCategory.ONE_HOUR_BEFORE ->
                "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}. نحن في انتظار زيارتك!"
            WhatsAppTemplateCategory.BEFORE_APPOINTMENT ->
                "مرحبا {اسم المريض} 🌟\nنذكرك بموعدك في {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nفي حالة عدم الحضور يرجى إبلاغنا مسبقاً.\nشكراً لك 🌟"
            WhatsAppTemplateCategory.AFTER_TREATMENT ->
                "أهلاً {اسم المريض} 🌸\nنتمنى لك دوام الصحة والعافية بعد زيارتك لعيادة {اسم العيادة} اليوم.\nيرجى اتباع إرشادات الطبيب والتواصل معنا في حال حدوث أي استفسار.\nمع تمنياتنا لك بابتسامة مشرقة! ✨"
            WhatsAppTemplateCategory.CONFIRMATION ->
                "مرحباً {اسم المريض} ✅\nتم تأكيد حجز موعدك بنجاح في عيادة {اسم العيادة}.\nالتاريخ: {تاريخ الموعد}\nالوقت: {وقت الموعد}\nنتشرف بزيارتك ويسعدنا تقديم أفضل رعاية لك."
            WhatsAppTemplateCategory.CANCELLATION ->
                "عزيزنا {اسم المريض} ⚠️\nنود إفادتك بأنه تم إلغاء موعدك المحدد بتاريخ {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nلتحديد موعد بديل يناسبك، نسعد بتواصلك معنا في أي وقت."
        }
    }

    fun formatWhatsAppMessage(templateText: String, appointment: AppointmentEntity): String {
        val dateFormatted = DateUtils.formatArabicFullDate(DateUtils.parseDate(appointment.dateStr))
        val time12 = TimeFormatter.format12Hour(appointment.startTime)
        return templateText
            .replace("{اسم المريض}", appointment.patientName)
            .replace("{تاريخ الموعد}", dateFormatted)
            .replace("{وقت الموعد}", time12)
            .replace("{اسم العيادة}", "عيادة الأسنان")
            .replace("{اسم الإجراء}", appointment.procedureName)
            .replace("{نوع الإجراء}", appointment.procedureName)
    }

    fun sendWhatsAppTemplate(
        context: Context,
        appointment: AppointmentEntity,
        category: WhatsAppTemplateCategory
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val template = repository.getTemplateByCategory(category)
            val templateText = template?.messageText?.takeIf { it.isNotBlank() } ?: getDefaultTemplateText(category)
            val message = formatWhatsAppMessage(templateText, appointment)
            launch(Dispatchers.Main) {
                ClinicActions.openWhatsApp(context, appointment.patientPhone, message)
            }
        }
    }

    fun sendOneHourWhatsAppReminder(context: Context, appointment: AppointmentEntity) {
        sendWhatsAppTemplate(context, appointment, WhatsAppTemplateCategory.ONE_HOUR_BEFORE)
    }

    fun sendAppointmentWhatsApp(
        context: Context,
        appointment: AppointmentEntity,
        category: WhatsAppTemplateCategory = WhatsAppTemplateCategory.BEFORE_APPOINTMENT
    ) {
        sendWhatsAppTemplate(context, appointment, category)
    }

    fun sendPostTreatmentWhatsApp(context: Context, appointment: AppointmentEntity) {
        sendWhatsAppTemplate(context, appointment, WhatsAppTemplateCategory.AFTER_TREATMENT)
    }

    fun sendPatientWhatsApp(context: Context, patient: PatientEntity) {
        val message = "مرحباً ${patient.name} 🌟، نتواصل معك من عيادة الأسنان. يسعدنا تقديم أفضل رعاية لك."
        ClinicActions.openWhatsApp(context, patient.phone, message)
    }

    fun exportNotionCalendar(context: Context) {
        NotionCalendarHelper.exportAndShareCalendar(context, allAppointments.value)
    }

    fun openNotionCalendar(context: Context) {
        NotionCalendarHelper.openNotionCalendar(context)
    }

    fun backupToFirebase() {
        viewModelScope.launch(Dispatchers.IO) {
            isFirebaseSyncing.value = true
            firebaseSyncStatus.value = "جارِ رفع البيانات إلى Firebase Firestore..."
            val result = FirebaseSyncManager.backupDataToFirebase(
                patients = allPatients.value,
                appointments = allAppointments.value,
                users = allUsers.value,
                procedures = allProcedures.value,
                actionLogs = allActionLogs.value,
                templates = allTemplates.value
            )
            isFirebaseSyncing.value = false
            result.onSuccess { msg ->
                firebaseSyncStatus.value = msg
            }.onFailure { err ->
                firebaseSyncStatus.value = "فشلت المزامنة: ${err.localizedMessage ?: "تحقق من الاتصال بالإنترنت"}"
            }
        }
    }

    fun restoreFromFirebase() {
        viewModelScope.launch(Dispatchers.IO) {
            isFirebaseSyncing.value = true
            firebaseSyncStatus.value = "جارِ استرجاع البيانات من Firebase Firestore..."
            val result = FirebaseSyncManager.restoreDataFromFirebase()
            isFirebaseSyncing.value = false
            result.onSuccess { data ->
                for (p in data.patients) repository.insertPatient(p)
                for (a in data.appointments) repository.insertAppointment(a)
                for (u in data.users) repository.insertUser(u)
                for (pr in data.procedures) repository.insertProcedure(pr)
                for (l in data.actionLogs) repository.insertActionLog(l)
                for (t in data.templates) repository.updateTemplate(t)
                TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
                firebaseSyncStatus.value = "تم استرجاع البيانات بنجاح: ${data.patients.size} مريض، ${data.appointments.size} موعد، ${data.users.size} موظف، ${data.procedures.size} إجراء، ${data.actionLogs.size} سجل نشاط!"
            }.onFailure { err ->
                firebaseSyncStatus.value = "فشل الاسترجاع: ${err.localizedMessage ?: "تحقق من الاتصال بالإنترنت"}"
            }
        }
    }

    fun testFirebaseConnection(context: Context, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            isFirebaseSyncing.value = true
            firebaseSyncStatus.value = "جارِ فحص واختبار الاتصال الفعلي بـ Firebase..."
            val result = FirebaseSyncManager.testFirebaseConnection(context)
            isFirebaseSyncing.value = false
            result.onSuccess { msg ->
                firebaseSyncStatus.value = msg
                withContext(Dispatchers.Main) { onResult?.invoke(true, msg) }
            }.onFailure { err ->
                val errorMsg = "فشل الاتصال: ${err.localizedMessage ?: "تحقق من الإنترنت والإعدادات"}"
                firebaseSyncStatus.value = errorMsg
                withContext(Dispatchers.Main) { onResult?.invoke(false, errorMsg) }
            }
        }
    }

    fun applyFilter(filter: FilterState) {
        filterState.value = filter
        isFilterSheetVisible.value = false
    }

    fun resetFilter() {
        filterState.value = FilterState()
        isFilterSheetVisible.value = false
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseSyncManager.stopAllRealtimeListeners()
    }
}
