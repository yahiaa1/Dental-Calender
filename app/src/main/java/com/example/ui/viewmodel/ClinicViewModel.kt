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
import java.time.LocalDate

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

    // User Authentication State
    val isAuthenticated = MutableStateFlow(false)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ClinicRepository(
            database.patientDao(),
            database.appointmentDao(),
            database.procedureConfigDao(),
            database.whatsappTemplateDao(),
            database.userDao()
        )

        // Ensure fixed admin user exists in DB:
        // "اليوزر بتاع الadmin يكون ثابت ك admin والباسورد 1234655"
        viewModelScope.launch(Dispatchers.IO) {
            val adminUser = repository.getUserByUsername("admin")
            if (adminUser == null) {
                repository.insertUser(
                    UserEntity(
                        id = 1,
                        name = "د. محمد (أدمن)",
                        phone = "01000000001",
                        role = UserRole.ADMIN,
                        username = "admin",
                        password = "1234655"
                    )
                )
            }
            // Remove pre-registered sample accounts if present
            val defaultReception = repository.getUserByUsername("reception")
            if (defaultReception != null && (defaultReception.name.contains("موظف استقبال") || defaultReception.name.contains("أحمد"))) {
                repository.deleteUser(defaultReception)
            }
            val defaultNurse = repository.getUserByUsername("nurse")
            if (defaultNurse != null && (defaultNurse.name.contains("مساعد تمريض") || defaultNurse.name.contains("سارة"))) {
                repository.deleteUser(defaultNurse)
            }
        }

        // Realtime automatic sync across clinic devices via Firebase Firestore:
        FirebaseSyncManager.startRealtimeAppointmentsListener(application) { remoteAppointments ->
            viewModelScope.launch(Dispatchers.IO) {
                for (remoteApp in remoteAppointments) {
                    repository.insertAppointment(remoteApp)
                }
                TodayAppointmentsWidgetProvider.notifyDataChanged(application)
            }
        }
    }

    // Default to current date
    val selectedDate = MutableStateFlow(LocalDate.now())
    val calendarViewMode = MutableStateFlow(CalendarViewMode.THREE_DAYS)
    val currentScreen = MutableStateFlow<ClinicScreen>(ClinicScreen.MainCalendar)
    val bottomNavIndex = MutableStateFlow(0) // 0=Calendar, 1=Patients, 2=Reports, 3=Settings

    // Clinic Working Hours configuration
    val workStartHour = MutableStateFlow(9) // 09:00
    val workEndHour = MutableStateFlow(21)   // 21:00

    // Zoom level: minimum boundary is 80f, max 160f
    val hourHeightDp = MutableStateFlow(85f)

    // Option to hide cancelled appointments from calendar view
    val hideCancelledFromCalendar = MutableStateFlow(true)

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser = MutableStateFlow(
        UserEntity(1, "د. محمد (أدمن)", "01000000001", UserRole.ADMIN, username = "admin", password = "1234655")
    )

    // Data streams from Room
    val allAppointments: StateFlow<List<AppointmentEntity>> = repository.allAppointments
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPatients: StateFlow<List<PatientEntity>> = repository.allPatients
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProcedures: StateFlow<List<ProcedureConfigEntity>> = repository.allProcedures
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<WhatsAppTemplateEntity>> = repository.allTemplates
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarVisibleAppointments: StateFlow<List<AppointmentEntity>> = combine(
        allAppointments,
        hideCancelledFromCalendar,
        filterState
    ) { appointments, hideCancelled, filter ->
        appointments.filter { app ->
            if (hideCancelled && app.status == AppointmentStatus.NO_SHOW) {
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthAppointmentDots: StateFlow<Map<String, List<AppointmentStatus>>> = calendarVisibleAppointments
        .map { list -> list.groupBy({ it.dateStr }, { it.status }) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val procedureColorsMap: StateFlow<Map<String, String>> = allProcedures
        .map { list -> list.associate { it.name to it.colorHex } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Login Method:
    // "اليوزر بتاع الadmin يكون ثابت ك admin والباسورد 1234655"
    // "وانا ال احدد اليوزر والباسورد لكل شخص"
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
                admin = newAdmin
            }
            currentUser.value = admin
            isAuthenticated.value = true
            return admin
        }

        // 2. Custom staff accounts verification
        val user = repository.getUserByUsername(cleanUsername)
        if (user != null && user.password == cleanPass) {
            currentUser.value = user
            isAuthenticated.value = true
            return user
        }
        return null
    }

    fun logout() {
        isAuthenticated.value = false
        currentScreen.value = ClinicScreen.MainCalendar
        bottomNavIndex.value = 0
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
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
    }

    fun addUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUser(user)
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUser(user)
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUserById(userId)
        }
    }

    fun setZoom(height: Float) {
        hourHeightDp.value = height.coerceIn(80f, 160f)
    }

    fun setWorkingHours(start: Int, end: Int) {
        if (start < end) {
            workStartHour.value = start
            workEndHour.value = end
        }
    }

    fun toggleHideCancelled() {
        hideCancelledFromCalendar.value = !hideCancelledFromCalendar.value
    }

    fun setReportsSelectedDate(date: LocalDate) {
        reportsSelectedDate.value = date
    }

    fun openAppointmentDetails(appointment: AppointmentEntity) {
        activeAppointment.value = appointment
        currentScreen.value = ClinicScreen.AppointmentDetails(appointment.id)
    }

    // Patient Details navigation as requested:
    // "لما بشوف المريض من المرضى واضغط عليه بيجيب المواعيد علطول اني احجز مفروض اخش عليه واشوف بياناته"
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
                    repository.updatePatient(existingPatient.copy(classification = appointment.patientClassification))
                }
                existingPatient.id
            } else {
                repository.insertPatient(
                    PatientEntity(
                        id = 0,
                        name = appointment.patientName.trim(),
                        phone = appointment.patientPhone.trim(),
                        classification = appointment.patientClassification,
                        notes = "تم تسجيله تلقائياً عند حجز الموعد: ${appointment.procedureName}"
                    )
                )
            }

            val updatedApp = appointment.copy(patientId = finalPatientId)
            val generatedId = if (updatedApp.id == 0L) {
                repository.insertAppointment(updatedApp)
            } else {
                repository.updateAppointment(updatedApp)
                updatedApp.id
            }
            val finalSavedApp = updatedApp.copy(id = generatedId)
            activeAppointment.value = finalSavedApp

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
            repository.updateAppointment(updated)
            activeAppointment.value = updated
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
            repository.updateAppointmentStatus(appointmentId, newStatus)
            activeAppointment.value?.let {
                if (it.id == appointmentId) {
                    val updated = it.copy(status = newStatus)
                    activeAppointment.value = updated
                    FirebaseSyncManager.autoSyncAppointment(updated)
                }
            }
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            AppointmentAlarmScheduler.cancelReminder(getApplication(), appointment.id)
            repository.deleteAppointment(appointment)
            FirebaseSyncManager.autoDeleteAppointment(appointment.id)
            if (activeAppointment.value?.id == appointment.id) {
                activeAppointment.value = null
            }
            TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun savePatient(patient: PatientEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = if (patient.id == 0L) {
                repository.insertPatient(patient)
            } else {
                repository.updatePatient(patient)
                patient.id
            }
            FirebaseSyncManager.autoSyncPatient(patient.copy(id = id))
            onComplete?.invoke(id)
        }
    }

    fun updatePatient(patient: PatientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePatient(patient)
            FirebaseSyncManager.autoSyncPatient(patient)
            if (activePatient.value?.id == patient.id) {
                activePatient.value = patient
            }
        }
    }

    fun addProcedure(procedure: ProcedureConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProcedure(procedure)
        }
    }

    fun updateProcedure(procedure: ProcedureConfigEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProcedure(procedure)
        }
    }

    fun deleteProcedure(procedureId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProcedureById(procedureId)
        }
    }

    fun updatePatientClassification(patientId: Long, classification: PatientClassification) {
        viewModelScope.launch(Dispatchers.IO) {
            val patient = repository.getPatientById(patientId)
            if (patient != null) {
                val updated = patient.copy(classification = classification)
                repository.updatePatient(updated)
                if (activePatient.value?.id == patientId) {
                    activePatient.value = updated
                }
            }
            activeAppointment.value?.let { app ->
                if (app.patientId == patientId) {
                    val updated = app.copy(patientClassification = classification)
                    repository.updateAppointment(updated)
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
        }
    }

    fun saveWhatsAppTemplate(template: WhatsAppTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTemplate(template)
        }
    }

    fun sendOneHourWhatsAppReminder(context: Context, appointment: AppointmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val template = repository.getTemplateByCategory(WhatsAppTemplateCategory.ONE_HOUR_BEFORE)
            val templateText = template?.messageText
                ?: "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}. نحن بانتظارك!"
            val time12 = TimeFormatter.format12Hour(appointment.startTime)
            val message = templateText
                .replace("{اسم المريض}", appointment.patientName)
                .replace("{وقت الموعد}", time12)
                .replace("{تاريخ الموعد}", appointment.dateStr)
                .replace("{اسم العيادة}", "عيادة الأسنان")

            launch(Dispatchers.Main) {
                ClinicActions.openWhatsApp(context, appointment.patientPhone, message)
            }
        }
    }

    fun sendAppointmentWhatsApp(context: Context, appointment: AppointmentEntity, category: WhatsAppTemplateCategory = WhatsAppTemplateCategory.BEFORE_APPOINTMENT) {
        viewModelScope.launch(Dispatchers.IO) {
            val template = repository.getTemplateByCategory(category)
            val templateText = template?.messageText ?: "مرحبا {اسم المريض}، نذكرك بموعدك في {تاريخ الموعد} الساعة {وقت الموعد}."
            val dateFormatted = DateUtils.formatArabicFullDate(DateUtils.parseDate(appointment.dateStr))
            val time12 = TimeFormatter.format12Hour(appointment.startTime)
            val message = templateText
                .replace("{اسم المريض}", appointment.patientName)
                .replace("{تاريخ الموعد}", dateFormatted)
                .replace("{وقت الموعد}", time12)
                .replace("{اسم العيادة}", "عيادة الأسنان")

            launch(Dispatchers.Main) {
                ClinicActions.openWhatsApp(context, appointment.patientPhone, message)
            }
        }
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
                appointments = allAppointments.value
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
            result.onSuccess { (patients, appointments) ->
                for (p in patients) repository.insertPatient(p)
                for (a in appointments) repository.insertAppointment(a)
                TodayAppointmentsWidgetProvider.notifyDataChanged(getApplication())
                firebaseSyncStatus.value = "تم استرجاع ${patients.size} مريض و ${appointments.size} موعد بنجاح من Firebase!"
            }.onFailure { err ->
                firebaseSyncStatus.value = "فشل الاسترجاع: ${err.localizedMessage ?: "تحقق من الاتصال بالإنترنت"}"
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
}
