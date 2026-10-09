package com.example.data.firebase

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.receiver.TodayAppointmentsWidgetProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class FirebaseConfig(
    val projectId: String,
    val apiKey: String,
    val appId: String,
    val storageBucket: String
)

data class RestoredClinicData(
    val patients: List<PatientEntity>,
    val appointments: List<AppointmentEntity>,
    val users: List<UserEntity>,
    val procedures: List<ProcedureConfigEntity>,
    val actionLogs: List<ActionLogEntity>,
    val templates: List<WhatsAppTemplateEntity> = emptyList()
)

object FirebaseSyncManager {

    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_NAME = "firebase_clinic_config"
    private const val KEY_PROJECT_ID = "firebase_project_id"
    private const val KEY_API_KEY = "firebase_api_key"
    private const val KEY_APP_ID = "firebase_app_id"
    private const val KEY_STORAGE_BUCKET = "firebase_storage_bucket"

    // Default configuration (from official google-services.json: dental-calender-ef240)
    const val DEFAULT_PROJECT_ID = "dental-calender-ef240"
    const val DEFAULT_APP_ID = "1:931136384586:android:c3087ed03afcddea5f00b3"
    const val DEFAULT_API_KEY = "AIzaSyAwUFN0EVb99R7PUY_ZAZPblmx2yKAOQ5k"
    const val DEFAULT_STORAGE_BUCKET = "dental-calender-ef240.firebasestorage.app"

    private var appContext: Context? = null
    private var appointmentsListener: ListenerRegistration? = null
    private var patientsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var proceduresListener: ListenerRegistration? = null
    private var actionLogsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var templatesListener: ListenerRegistration? = null

    private var isInitialized = false
    private var isFirestoreConfigured = false

    // Realtime Sync State indicators
    private val _isRealtimeActive = MutableStateFlow(false)
    val isRealtimeActive = _isRealtimeActive.asStateFlow()

    private val _realtimeStatus = MutableStateFlow<String?>("المزامنة التلقائية جاهزة")
    val realtimeStatus = _realtimeStatus.asStateFlow()

    // Conflict Resolution & Echo Loop Prevention:
    // Tracks local modification timestamps by entity key (e.g. "appointment_123" -> 1740928374000L).
    // If an incoming remote snapshot has an older or pending write timestamp, it won't overwrite the local edit.
    private val lastUpdatedMap = ConcurrentHashMap<String, Long>()

    /**
     * Generates a globally unique 64-bit Long ID across all clinic devices.
     * Combines millisecond timestamp and a randomized suffix.
     * Prevents primary-key collisions between devices when creating records simultaneously or offline.
     */
    fun generateUniqueId(): Long {
        val timeMs = System.currentTimeMillis()
        val suffix = Random.nextLong(100L, 999L)
        return (timeMs * 1000L) + suffix
    }

    /**
     * Registers a local mutation timestamp to resolve concurrent editing conflicts and prevent echo loops.
     */
    fun markLocalUpdate(entityType: String, id: Long, timestamp: Long = System.currentTimeMillis()) {
        lastUpdatedMap["${entityType}_$id"] = timestamp
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureFirebaseInitialized(context)
    }

    fun getConfig(context: Context): FirebaseConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedProjectId = prefs.getString(KEY_PROJECT_ID, null)
        val savedAppId = prefs.getString(KEY_APP_ID, null)
        if (savedProjectId == null || savedProjectId == "dentalclinic-aistudio" || savedAppId != DEFAULT_APP_ID) {
            prefs.edit().clear().apply()
            return FirebaseConfig(
                projectId = DEFAULT_PROJECT_ID,
                apiKey = DEFAULT_API_KEY,
                appId = DEFAULT_APP_ID,
                storageBucket = DEFAULT_STORAGE_BUCKET
            )
        }
        return FirebaseConfig(
            projectId = savedProjectId,
            apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY,
            appId = savedAppId,
            storageBucket = prefs.getString(KEY_STORAGE_BUCKET, DEFAULT_STORAGE_BUCKET) ?: DEFAULT_STORAGE_BUCKET
        )
    }

    fun saveConfig(context: Context, config: FirebaseConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PROJECT_ID, config.projectId.trim())
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_APP_ID, config.appId.trim())
            .putString(KEY_STORAGE_BUCKET, config.storageBucket.trim())
            .apply()
        ensureFirebaseInitialized(context, forceReinit = true)
    }

    fun resetConfig(context: Context): FirebaseConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val defaultConfig = FirebaseConfig(
            projectId = DEFAULT_PROJECT_ID,
            apiKey = DEFAULT_API_KEY,
            appId = DEFAULT_APP_ID,
            storageBucket = DEFAULT_STORAGE_BUCKET
        )
        ensureFirebaseInitialized(context, forceReinit = true)
        return defaultConfig
    }

    fun ensureFirebaseInitialized(context: Context, forceReinit: Boolean = false): Boolean {
        if (isInitialized && !forceReinit) return true
        val targetCtx = context.applicationContext
        appContext = targetCtx

        return try {
            if (forceReinit && FirebaseApp.getApps(targetCtx).isNotEmpty()) {
                try {
                    FirebaseApp.getInstance().delete()
                } catch (delEx: Exception) {
                    Log.w(TAG, "Deleting old Firebase app warning: ${delEx.message}")
                }
            }

            if (FirebaseApp.getApps(targetCtx).isEmpty()) {
                val initializedFromGoogleServices = try {
                    FirebaseApp.initializeApp(targetCtx) != null
                } catch (e: Exception) {
                    false
                }
                if (!initializedFromGoogleServices) {
                    val config = getConfig(targetCtx)
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(config.appId)
                        .setProjectId(config.projectId)
                        .setApiKey(config.apiKey)
                        .setStorageBucket(config.storageBucket)
                        .build()
                    FirebaseApp.initializeApp(targetCtx, options)
                }
            }
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
            try {
                val fallbackOptions = FirebaseOptions.Builder()
                    .setApplicationId(DEFAULT_APP_ID)
                    .setProjectId(DEFAULT_PROJECT_ID)
                    .setApiKey(DEFAULT_API_KEY)
                    .setStorageBucket(DEFAULT_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(targetCtx, fallbackOptions)
                isInitialized = true
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Firebase fallback initialization failed: ${fallbackEx.message}")
                false
            }
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        val ctx = appContext
        if (ctx != null) {
            ensureFirebaseInitialized(ctx)
        }
        return try {
            val db = FirebaseFirestore.getInstance()
            if (!isFirestoreConfigured) {
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                    db.firestoreSettings = settings
                } catch (e: Exception) {
                    // Settings can only be configured once before any Firestore operation
                }
                isFirestoreConfigured = true
            }
            db
        } catch (e: Exception) {
            Log.w(TAG, "Firestore instance not ready: ${e.message}")
            null
        }
    }

    // Helper functions for safe extraction from Firestore DocumentSnapshots
    private fun DocumentSnapshot.safeLong(field: String): Long? {
        return (get(field) as? Number)?.toLong() ?: getString(field)?.toLongOrNull()
    }

    private fun DocumentSnapshot.safeDouble(field: String): Double? {
        return (get(field) as? Number)?.toDouble() ?: getString(field)?.toDoubleOrNull()
    }

    private fun DocumentSnapshot.safeInt(field: String): Int? {
        return (get(field) as? Number)?.toInt() ?: getString(field)?.toIntOrNull()
    }

    private fun DocumentSnapshot.safeString(field: String, default: String = ""): String {
        return getString(field) ?: (get(field)?.toString()) ?: default
    }

    // Live test connection to Firebase
    suspend fun testFirebaseConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val initialized = ensureFirebaseInitialized(context)
            if (!initialized) {
                return@withContext Result.failure(Exception("تعذر تهيئة مكتبة Firebase. يرجى مراجعة الإعدادات."))
            }
            val db = getFirestore() ?: return@withContext Result.failure(Exception("تعذر الحصول على اتصال Firestore"))

            val testDoc = db.collection("_clinic_connection_test").document("ping_status")
            val pingData = mapOf(
                "lastTestTimestamp" to System.currentTimeMillis(),
                "status" to "SUCCESS",
                "device" to Build.MODEL,
                "projectId" to getConfig(context).projectId
            )
            testDoc.set(pingData).await()
            val readBack = testDoc.get().await()
            if (readBack.exists()) {
                val proj = getConfig(context).projectId
                Result.success("تم الاتصال بنجاح وتأكيد القراءة والكتابة السحابية في Firebase Firestore!\nالمشروع: $proj")
            } else {
                Result.failure(Exception("تم إرسال الطلب لكن لم يتم التحقق من وجود المستند في الخادم."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test error", e)
            val msg = e.localizedMessage ?: e.message ?: ""
            val helpfulMsg = when {
                msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                    "قواعد الأمان في Firebase تمنع القراءة والكتابة (PERMISSION_DENIED).\n\n" +
                    "💡 الحل: راجع ملف firestore.rules في المشروع وقم بنشره على Firebase Console لتفعيل قواعد الأمان الرسمية الخاصة بالعيادة."

                msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("FAILED_PRECONDITION", ignoreCase = true) || msg.contains("database", ignoreCase = true) ->
                    "قاعدة البيانات Cloud Firestore غير مفعلة حتى الآن في حساب Firebase.\n\n" +
                    "💡 افتح Firestore Database في Firebase Console واضغط على 'Create Database'."

                msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("offline", ignoreCase = true) || msg.contains("UnknownHost", ignoreCase = true) ->
                    "تعذر الاتصال بسيرفر Firebase بسبب ضعف أو انقطاع الإنترنت.\n\n💡 التطبيق يحتفظ بالتعديلات محلياً وسيرفعها تلقائياً عند عودة الاتصال."

                else ->
                    "سبب عدم الاتصال: $msg"
            }
            Result.failure(Exception(helpfulMsg))
        }
    }

    // =========================================================================
    // Realtime Auto-Upload Functions (Invoked instantly on Add, Edit, Delete)
    // =========================================================================

    // 1. Appointments Auto-Sync
    fun autoSyncAppointment(appointment: AppointmentEntity) {
        val now = System.currentTimeMillis()
        markLocalUpdate("appointment", appointment.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_appointments").document(appointment.id.toString())
                val data = mapOf(
                    "id" to appointment.id,
                    "patientId" to appointment.patientId,
                    "patientName" to appointment.patientName,
                    "patientPhone" to appointment.patientPhone,
                    "procedureName" to appointment.procedureName,
                    "chairNumber" to appointment.chairNumber,
                    "dateStr" to appointment.dateStr,
                    "startTime" to appointment.startTime,
                    "endTime" to appointment.endTime,
                    "price" to appointment.price,
                    "status" to appointment.status.name,
                    "patientClassification" to appointment.patientClassification.name,
                    "notes" to appointment.notes,
                    "bookedByUserId" to appointment.bookedByUserId,
                    "bookedByUserName" to appointment.bookedByUserName,
                    "createdAt" to appointment.createdAt,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced appointment #${appointment.id} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing appointment #${appointment.id}: ${e.message}")
            }
        }
    }

    fun autoDeleteAppointment(appointmentId: Long) {
        markLocalUpdate("appointment", appointmentId, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_appointments").document(appointmentId.toString()).delete().await()
                Log.d(TAG, "Deleted appointment #$appointmentId from Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting appointment #$appointmentId from Firestore: ${e.message}")
            }
        }
    }

    // 2. Patients Auto-Sync
    fun autoSyncPatient(patient: PatientEntity) {
        val now = System.currentTimeMillis()
        markLocalUpdate("patient", patient.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_patients").document(patient.id.toString())
                val data = mapOf(
                    "id" to patient.id,
                    "name" to patient.name,
                    "phone" to patient.phone,
                    "classification" to patient.classification.name,
                    "notes" to patient.notes,
                    "createdAt" to patient.createdAt,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced patient #${patient.id} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing patient #${patient.id}: ${e.message}")
            }
        }
    }

    fun autoDeletePatient(patientId: Long) {
        markLocalUpdate("patient", patientId, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_patients").document(patientId.toString()).delete().await()
                Log.d(TAG, "Deleted patient #$patientId from Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting patient #$patientId from Firestore: ${e.message}")
            }
        }
    }

    // 3. Staff / Users Auto-Sync
    fun autoSyncUser(user: UserEntity) {
        val now = System.currentTimeMillis()
        markLocalUpdate("user", user.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_users").document(user.id.toString())
                val data = mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "phone" to user.phone,
                    "role" to user.role.name,
                    "username" to user.username,
                    "password" to user.password,
                    "deviceIdentifier" to user.deviceIdentifier,
                    "createdAt" to user.createdAt,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced user #${user.id} (${user.username}) to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing user #${user.id}: ${e.message}")
            }
        }
    }

    fun autoDeleteUser(userId: Long) {
        markLocalUpdate("user", userId, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_users").document(userId.toString()).delete().await()
                Log.d(TAG, "Deleted user #$userId from Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting user #$userId from Firestore: ${e.message}")
            }
        }
    }

    // 4. Procedures Configuration Auto-Sync
    fun autoSyncProcedure(procedure: ProcedureConfigEntity) {
        val now = System.currentTimeMillis()
        markLocalUpdate("procedure", procedure.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_procedures").document(procedure.id.toString())
                val data = mapOf(
                    "id" to procedure.id,
                    "name" to procedure.name,
                    "colorHex" to procedure.colorHex,
                    "defaultDurationMinutes" to procedure.defaultDurationMinutes,
                    "defaultPrice" to procedure.defaultPrice,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced procedure #${procedure.id} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing procedure #${procedure.id}: ${e.message}")
            }
        }
    }

    fun autoDeleteProcedure(procedureId: Long) {
        markLocalUpdate("procedure", procedureId, System.currentTimeMillis())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_procedures").document(procedureId.toString()).delete().await()
                Log.d(TAG, "Deleted procedure #$procedureId from Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting procedure #$procedureId from Firestore: ${e.message}")
            }
        }
    }

    // 5. Action Logs Auto-Sync
    fun autoSyncActionLog(log: ActionLogEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_action_logs").document(log.id.toString())
                val data = mapOf(
                    "id" to log.id,
                    "timestamp" to log.timestamp,
                    "dateStr" to log.dateStr,
                    "timeStr" to log.timeStr,
                    "actionType" to log.actionType.name,
                    "employeeId" to log.employeeId,
                    "employeeName" to log.employeeName,
                    "employeeRole" to log.employeeRole,
                    "patientName" to log.patientName,
                    "patientPhone" to log.patientPhone,
                    "appointmentId" to log.appointmentId,
                    "appointmentDate" to log.appointmentDate,
                    "appointmentTime" to log.appointmentTime,
                    "procedureName" to log.procedureName,
                    "details" to log.details,
                    "extraInfo" to log.extraInfo
                )
                docRef.set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing action log: ${e.message}")
            }
        }
    }

    fun autoDeleteActionLog(logId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_action_logs").document(logId.toString()).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting action log: ${e.message}")
            }
        }
    }

    // 6. WhatsApp Templates Auto-Sync
    fun autoSyncWhatsAppTemplate(template: WhatsAppTemplateEntity) {
        val now = System.currentTimeMillis()
        markLocalUpdate("template", template.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_whatsapp_templates").document(template.id.toString())
                val data = mapOf(
                    "id" to template.id,
                    "category" to template.category.name,
                    "title" to template.title,
                    "messageText" to template.messageText,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced WhatsApp template #${template.id} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing WhatsApp template: ${e.message}")
            }
        }
    }

    // 7. Clinic Global Settings Auto-Sync (Working hours, day off)
    fun autoSyncSettings(
        workStartHour: Int,
        workEndHour: Int,
        dayOff: DayOfWeek?,
        hideDayOff: Boolean,
        hideCancelled: Boolean
    ) {
        val now = System.currentTimeMillis()
        markLocalUpdate("settings", 1L, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                val docRef = db.collection("clinic_settings").document("general_settings")
                val data = mapOf(
                    "workStartHour" to workStartHour,
                    "workEndHour" to workEndHour,
                    "dayOff" to (dayOff?.name ?: "NONE"),
                    "hideDayOff" to hideDayOff,
                    "hideCancelled" to hideCancelled,
                    "updatedAt" to now
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced clinic settings to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing settings: ${e.message}")
            }
        }
    }

    // =========================================================================
    // Realtime Snapshot Listeners (Bi-directional Remote-to-Local Sync)
    // =========================================================================

    @Synchronized
    fun startAllRealtimeListeners(
        context: Context,
        repository: ClinicRepository,
        onSettingsChanged: ((workStart: Int, workEnd: Int, dayOff: DayOfWeek?, hideDayOff: Boolean, hideCancelled: Boolean) -> Unit)? = null
    ) {
        init(context)
        stopAllRealtimeListeners()
        val db = getFirestore() ?: run {
            _isRealtimeActive.value = false
            _realtimeStatus.value = "في انتظار الاتصال بـ Firebase..."
            return
        }

        _isRealtimeActive.value = true
        _realtimeStatus.value = "المزامنة اللحظية نشطة"

        // A. Appointments Realtime Listener
        try {
            appointmentsListener = db.collection("clinic_appointments")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Appointments snapshot listener error: ${error.message}")
                        _realtimeStatus.value = "تنبيه مزامنة المواعيد: ${error.localizedMessage}"
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document

                                // 1. Echo Loop Prevention: Skip processing our own uncommitted pending writes
                                if (doc.metadata.hasPendingWrites()) {
                                    continue
                                }

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue

                                if (dc.type == DocumentChange.Type.REMOVED) {
                                    repository.deleteAppointmentById(id)
                                    lastUpdatedMap.remove("appointment_$id")
                                } else {
                                    val remoteUpdatedAt = doc.safeLong("updatedAt") ?: doc.safeLong("createdAt") ?: 0L
                                    val localTime = lastUpdatedMap["appointment_$id"] ?: 0L

                                    // 2. Conflict Resolution: If local edit is strictly newer than remote update, ignore
                                    if (remoteUpdatedAt > 0 && localTime > 0 && remoteUpdatedAt < localTime) {
                                        continue
                                    }
                                    lastUpdatedMap["appointment_$id"] = remoteUpdatedAt

                                    val patientId = doc.safeLong("patientId") ?: 0L
                                    val patientName = doc.safeString("patientName")
                                    val patientPhone = doc.safeString("patientPhone")
                                    val procedureName = doc.safeString("procedureName")
                                    val chairNumber = doc.safeInt("chairNumber") ?: 1
                                    val dateStr = doc.safeString("dateStr")
                                    val startTime = doc.safeString("startTime")
                                    val endTime = doc.safeString("endTime")
                                    val price = doc.safeDouble("price") ?: 0.0

                                    val statusStr = doc.safeString("status", AppointmentStatus.UNCONFIRMED.name)
                                    val status = try {
                                        when (statusStr) {
                                            "SCHEDULED" -> AppointmentStatus.CONFIRMED
                                            "DELAYED" -> AppointmentStatus.UNCONFIRMED
                                            else -> AppointmentStatus.valueOf(statusStr)
                                        }
                                    } catch (e: Exception) {
                                        AppointmentStatus.UNCONFIRMED
                                    }

                                    val classStr = doc.safeString("patientClassification", PatientClassification.DISCIPLINED.name)
                                    val patientClassification = try {
                                        PatientClassification.valueOf(classStr)
                                    } catch (e: Exception) {
                                        PatientClassification.DISCIPLINED
                                    }

                                    val notes = doc.safeString("notes")
                                    val bookedByUserId = doc.safeLong("bookedByUserId") ?: 1L
                                    val bookedByUserName = doc.safeString("bookedByUserName", "د. محمد (أدمن)")
                                    val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                                    val app = AppointmentEntity(
                                        id = id,
                                        patientId = patientId,
                                        patientName = patientName,
                                        patientPhone = patientPhone,
                                        procedureName = procedureName,
                                        chairNumber = chairNumber,
                                        dateStr = dateStr,
                                        startTime = startTime,
                                        endTime = endTime,
                                        price = price,
                                        status = status,
                                        patientClassification = patientClassification,
                                        notes = notes,
                                        bookedByUserId = bookedByUserId,
                                        bookedByUserName = bookedByUserName,
                                        createdAt = createdAt
                                    )
                                    repository.insertAppointment(app)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing appointment change: ${e.message}")
                            }
                        }
                        // Refresh home screen widget when remote appointments arrive
                        appContext?.let { TodayAppointmentsWidgetProvider.notifyDataChanged(it) }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Appointments listener registration error: ${e.message}")
        }

        // B. Patients Realtime Listener
        try {
            patientsListener = db.collection("clinic_patients")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                if (doc.metadata.hasPendingWrites()) continue

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue

                                if (dc.type == DocumentChange.Type.REMOVED) {
                                    val existing = repository.getPatientById(id)
                                    if (existing != null) repository.deletePatient(existing)
                                    lastUpdatedMap.remove("patient_$id")
                                } else {
                                    val remoteUpdatedAt = doc.safeLong("updatedAt") ?: doc.safeLong("createdAt") ?: 0L
                                    val localTime = lastUpdatedMap["patient_$id"] ?: 0L
                                    if (remoteUpdatedAt > 0 && localTime > 0 && remoteUpdatedAt < localTime) continue
                                    lastUpdatedMap["patient_$id"] = remoteUpdatedAt

                                    val name = doc.safeString("name")
                                    val phone = doc.safeString("phone")
                                    val classStr = doc.safeString("classification", PatientClassification.DISCIPLINED.name)
                                    val classification = try { PatientClassification.valueOf(classStr) } catch (e: Exception) { PatientClassification.DISCIPLINED }
                                    val notes = doc.safeString("notes")
                                    val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                                    val patient = PatientEntity(id, name, phone, classification, notes, createdAt)
                                    repository.insertPatient(patient)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing patient change: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Patients listener error: ${e.message}")
        }

        // C. Staff / Users Realtime Listener
        try {
            usersListener = db.collection("clinic_users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                if (doc.metadata.hasPendingWrites()) continue

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue

                                if (dc.type == DocumentChange.Type.REMOVED) {
                                    repository.deleteUserById(id)
                                    lastUpdatedMap.remove("user_$id")
                                } else {
                                    val remoteUpdatedAt = doc.safeLong("updatedAt") ?: doc.safeLong("createdAt") ?: 0L
                                    val localTime = lastUpdatedMap["user_$id"] ?: 0L
                                    if (remoteUpdatedAt > 0 && localTime > 0 && remoteUpdatedAt < localTime) continue
                                    lastUpdatedMap["user_$id"] = remoteUpdatedAt

                                    val name = doc.safeString("name")
                                    val phone = doc.safeString("phone")
                                    val roleStr = doc.safeString("role", UserRole.STAFF.name)
                                    val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STAFF }
                                    val username = doc.safeString("username")
                                    val password = doc.safeString("password")
                                    val deviceIdentifier = doc.safeString("deviceIdentifier")
                                    val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                                    val user = UserEntity(
                                        id = id,
                                        name = name,
                                        phone = phone,
                                        role = role,
                                        username = username,
                                        password = password,
                                        deviceIdentifier = deviceIdentifier,
                                        createdAt = createdAt
                                    )
                                    repository.insertUser(user)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing user change: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Users listener error: ${e.message}")
        }

        // D. Procedures Config Realtime Listener
        try {
            proceduresListener = db.collection("clinic_procedures")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                if (doc.metadata.hasPendingWrites()) continue

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue

                                if (dc.type == DocumentChange.Type.REMOVED) {
                                    repository.deleteProcedureById(id)
                                    lastUpdatedMap.remove("procedure_$id")
                                } else {
                                    val remoteUpdatedAt = doc.safeLong("updatedAt") ?: 0L
                                    val localTime = lastUpdatedMap["procedure_$id"] ?: 0L
                                    if (remoteUpdatedAt > 0 && localTime > 0 && remoteUpdatedAt < localTime) continue
                                    lastUpdatedMap["procedure_$id"] = remoteUpdatedAt

                                    val name = doc.safeString("name")
                                    val colorHex = doc.safeString("colorHex", "#00796B")
                                    val defaultDuration = doc.safeInt("defaultDurationMinutes") ?: 30
                                    val defaultPrice = doc.safeDouble("defaultPrice") ?: 0.0

                                    val proc = ProcedureConfigEntity(
                                        id = id,
                                        name = name,
                                        colorHex = colorHex,
                                        defaultDurationMinutes = defaultDuration,
                                        defaultPrice = defaultPrice
                                    )
                                    repository.insertProcedure(proc)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing procedure change: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Procedures listener error: ${e.message}")
        }

        // E. Action Logs Realtime Listener
        try {
            actionLogsListener = db.collection("clinic_action_logs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                if (doc.metadata.hasPendingWrites()) continue

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue

                                if (dc.type == DocumentChange.Type.REMOVED) {
                                    repository.deleteActionLogById(id)
                                } else {
                                    val timestamp = doc.safeLong("timestamp") ?: System.currentTimeMillis()
                                    val dateStr = doc.safeString("dateStr")
                                    val timeStr = doc.safeString("timeStr")
                                    val typeStr = doc.safeString("actionType", ActionLogType.BOOKING.name)
                                    val actionType = try { ActionLogType.valueOf(typeStr) } catch (e: Exception) { ActionLogType.BOOKING }
                                    val employeeId = doc.safeLong("employeeId") ?: 0L
                                    val employeeName = doc.safeString("employeeName")
                                    val employeeRole = doc.safeString("employeeRole")
                                    val patientName = doc.safeString("patientName")
                                    val patientPhone = doc.safeString("patientPhone")
                                    val appointmentId = doc.safeLong("appointmentId") ?: 0L
                                    val appointmentDate = doc.safeString("appointmentDate")
                                    val appointmentTime = doc.safeString("appointmentTime")
                                    val procedureName = doc.safeString("procedureName")
                                    val details = doc.safeString("details")
                                    val extraInfo = doc.safeString("extraInfo")

                                    val log = ActionLogEntity(
                                        id = id,
                                        timestamp = timestamp,
                                        dateStr = dateStr,
                                        timeStr = timeStr,
                                        actionType = actionType,
                                        employeeId = employeeId,
                                        employeeName = employeeName,
                                        employeeRole = employeeRole,
                                        patientName = patientName,
                                        patientPhone = patientPhone,
                                        appointmentId = appointmentId,
                                        appointmentDate = appointmentDate,
                                        appointmentTime = appointmentTime,
                                        procedureName = procedureName,
                                        details = details,
                                        extraInfo = extraInfo
                                    )
                                    repository.insertActionLog(log)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing action log change: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Action logs listener error: ${e.message}")
        }

        // F. WhatsApp Templates Realtime Listener
        try {
            templatesListener = db.collection("clinic_whatsapp_templates")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                if (doc.metadata.hasPendingWrites()) continue

                                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: continue
                                if (dc.type != DocumentChange.Type.REMOVED) {
                                    val categoryStr = doc.safeString("category", WhatsAppTemplateCategory.BEFORE_APPOINTMENT.name)
                                    val category = try { WhatsAppTemplateCategory.valueOf(categoryStr) } catch (e: Exception) { WhatsAppTemplateCategory.BEFORE_APPOINTMENT }
                                    val title = doc.safeString("title")
                                    val messageText = doc.safeString("messageText")

                                    val t = WhatsAppTemplateEntity(id = id, category = category, title = title, messageText = messageText)
                                    repository.updateTemplate(t)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing template change: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Templates listener error: ${e.message}")
        }

        // G. Clinic Settings Realtime Listener (Working Hours, Day Off)
        try {
            settingsListener = db.collection("clinic_settings").document("general_settings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                    val remoteUpdatedAt = snapshot.safeLong("updatedAt") ?: 0L
                    val localTime = lastUpdatedMap["settings_1"] ?: 0L
                    if (remoteUpdatedAt > 0 && localTime > 0 && remoteUpdatedAt < localTime) return@addSnapshotListener
                    lastUpdatedMap["settings_1"] = remoteUpdatedAt

                    val start = snapshot.safeInt("workStartHour") ?: 9
                    val end = snapshot.safeInt("workEndHour") ?: 21
                    val dayOffStr = snapshot.safeString("dayOff", "FRIDAY")
                    val dayOff = if (dayOffStr == "NONE") null else try { DayOfWeek.valueOf(dayOffStr) } catch (e: Exception) { DayOfWeek.FRIDAY }
                    val hideDayOff = snapshot.getBoolean("hideDayOff") ?: false
                    val hideCancelled = snapshot.getBoolean("hideCancelled") ?: true

                    onSettingsChanged?.invoke(start, end, dayOff, hideDayOff, hideCancelled)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Settings listener error: ${e.message}")
        }
    }

    @Synchronized
    fun stopAllRealtimeListeners() {
        appointmentsListener?.remove()
        appointmentsListener = null

        patientsListener?.remove()
        patientsListener = null

        usersListener?.remove()
        usersListener = null

        proceduresListener?.remove()
        proceduresListener = null

        actionLogsListener?.remove()
        actionLogsListener = null

        templatesListener?.remove()
        templatesListener = null

        settingsListener?.remove()
        settingsListener = null

        _isRealtimeActive.value = false
    }

    // =========================================================================
    // Manual Full Backup & Restore (Preserved & Enhanced)
    // =========================================================================

    suspend fun backupDataToFirebase(
        patients: List<PatientEntity>,
        appointments: List<AppointmentEntity>,
        users: List<UserEntity> = emptyList(),
        procedures: List<ProcedureConfigEntity> = emptyList(),
        actionLogs: List<ActionLogEntity> = emptyList(),
        templates: List<WhatsAppTemplateEntity> = emptyList()
    ): Result<String> {
        val db = getFirestore() ?: return Result.failure(Exception("Firebase غير متصل. يرجى التحقق من الاتصال بالإنترنت وإعدادات المشروع."))
        return try {
            val batch = db.batch()
            val now = System.currentTimeMillis()

            for (p in patients) {
                markLocalUpdate("patient", p.id, now)
                val docRef = db.collection("clinic_patients").document(p.id.toString())
                val data = mapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "phone" to p.phone,
                    "classification" to p.classification.name,
                    "notes" to p.notes,
                    "createdAt" to p.createdAt,
                    "updatedAt" to now
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (a in appointments) {
                markLocalUpdate("appointment", a.id, now)
                val docRef = db.collection("clinic_appointments").document(a.id.toString())
                val data = mapOf(
                    "id" to a.id,
                    "patientId" to a.patientId,
                    "patientName" to a.patientName,
                    "patientPhone" to a.patientPhone,
                    "procedureName" to a.procedureName,
                    "chairNumber" to a.chairNumber,
                    "dateStr" to a.dateStr,
                    "startTime" to a.startTime,
                    "endTime" to a.endTime,
                    "price" to a.price,
                    "status" to a.status.name,
                    "patientClassification" to a.patientClassification.name,
                    "notes" to a.notes,
                    "bookedByUserId" to a.bookedByUserId,
                    "bookedByUserName" to a.bookedByUserName,
                    "createdAt" to a.createdAt,
                    "updatedAt" to now
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (u in users) {
                markLocalUpdate("user", u.id, now)
                val docRef = db.collection("clinic_users").document(u.id.toString())
                val data = mapOf(
                    "id" to u.id,
                    "name" to u.name,
                    "phone" to u.phone,
                    "role" to u.role.name,
                    "username" to u.username,
                    "password" to u.password,
                    "deviceIdentifier" to u.deviceIdentifier,
                    "createdAt" to u.createdAt,
                    "updatedAt" to now
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (pr in procedures) {
                markLocalUpdate("procedure", pr.id, now)
                val docRef = db.collection("clinic_procedures").document(pr.id.toString())
                val data = mapOf(
                    "id" to pr.id,
                    "name" to pr.name,
                    "colorHex" to pr.colorHex,
                    "defaultDurationMinutes" to pr.defaultDurationMinutes,
                    "defaultPrice" to pr.defaultPrice,
                    "updatedAt" to now
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (l in actionLogs) {
                val docRef = db.collection("clinic_action_logs").document(l.id.toString())
                val data = mapOf(
                    "id" to l.id,
                    "timestamp" to l.timestamp,
                    "dateStr" to l.dateStr,
                    "timeStr" to l.timeStr,
                    "actionType" to l.actionType.name,
                    "employeeId" to l.employeeId,
                    "employeeName" to l.employeeName,
                    "employeeRole" to l.employeeRole,
                    "patientName" to l.patientName,
                    "patientPhone" to l.patientPhone,
                    "appointmentId" to l.appointmentId,
                    "appointmentDate" to l.appointmentDate,
                    "appointmentTime" to l.appointmentTime,
                    "procedureName" to l.procedureName,
                    "details" to l.details,
                    "extraInfo" to l.extraInfo
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (t in templates) {
                val docRef = db.collection("clinic_whatsapp_templates").document(t.id.toString())
                val data = mapOf(
                    "id" to t.id,
                    "category" to t.category.name,
                    "title" to t.title,
                    "messageText" to t.messageText,
                    "updatedAt" to now
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Result.success("تم النسخ الاحتياطي السحابي لجميع بيانات العيادة بنجاح إلى Firebase Firestore!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDataFromFirebase(): Result<RestoredClinicData> {
        val db = getFirestore() ?: return Result.failure(Exception("Firebase غير متصل. يرجى التحقق من الاتصال بالإنترنت وإعدادات المشروع."))
        return try {
            val patientsSnapshot = db.collection("clinic_patients").get().await()
            val restoredPatients = patientsSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.safeString("name")
                val phone = doc.safeString("phone")
                val classStr = doc.safeString("classification", PatientClassification.DISCIPLINED.name)
                val classification = try {
                    PatientClassification.valueOf(classStr)
                } catch (e: Exception) {
                    PatientClassification.DISCIPLINED
                }
                val notes = doc.safeString("notes")
                val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                PatientEntity(id, name, phone, classification, notes, createdAt)
            }

            val appointmentsSnapshot = db.collection("clinic_appointments").get().await()
            val restoredAppointments = appointmentsSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val patientId = doc.safeLong("patientId") ?: 0L
                val patientName = doc.safeString("patientName")
                val patientPhone = doc.safeString("patientPhone")
                val procedureName = doc.safeString("procedureName")
                val chairNumber = doc.safeInt("chairNumber") ?: 1
                val dateStr = doc.safeString("dateStr")
                val startTime = doc.safeString("startTime")
                val endTime = doc.safeString("endTime")
                val price = doc.safeDouble("price") ?: 0.0
                val statusStr = doc.safeString("status", AppointmentStatus.UNCONFIRMED.name)
                val status = try {
                    when (statusStr) {
                        "SCHEDULED" -> AppointmentStatus.CONFIRMED
                        "DELAYED" -> AppointmentStatus.UNCONFIRMED
                        else -> AppointmentStatus.valueOf(statusStr)
                    }
                } catch (e: Exception) {
                    AppointmentStatus.UNCONFIRMED
                }
                val classStr = doc.safeString("patientClassification", PatientClassification.DISCIPLINED.name)
                val patientClassification = try {
                    PatientClassification.valueOf(classStr)
                } catch (e: Exception) {
                    PatientClassification.DISCIPLINED
                }
                val notes = doc.safeString("notes")
                val bookedByUserId = doc.safeLong("bookedByUserId") ?: 1L
                val bookedByUserName = doc.safeString("bookedByUserName", "د. محمد (أدمن)")
                val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()

                AppointmentEntity(
                    id = id,
                    patientId = patientId,
                    patientName = patientName,
                    patientPhone = patientPhone,
                    procedureName = procedureName,
                    chairNumber = chairNumber,
                    dateStr = dateStr,
                    startTime = startTime,
                    endTime = endTime,
                    price = price,
                    status = status,
                    patientClassification = patientClassification,
                    notes = notes,
                    bookedByUserId = bookedByUserId,
                    bookedByUserName = bookedByUserName,
                    createdAt = createdAt
                )
            }

            val usersSnapshot = db.collection("clinic_users").get().await()
            val restoredUsers = usersSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.safeString("name")
                val phone = doc.safeString("phone")
                val roleStr = doc.safeString("role", UserRole.STAFF.name)
                val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STAFF }
                val username = doc.safeString("username")
                val password = doc.safeString("password")
                val deviceIdentifier = doc.safeString("deviceIdentifier")
                val createdAt = doc.safeLong("createdAt") ?: System.currentTimeMillis()
                UserEntity(id, name, phone, role, username, password, deviceIdentifier, createdAt)
            }

            val proceduresSnapshot = db.collection("clinic_procedures").get().await()
            val restoredProcedures = proceduresSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.safeString("name")
                val colorHex = doc.safeString("colorHex", "#00796B")
                val defaultDuration = doc.safeInt("defaultDurationMinutes") ?: 30
                val defaultPrice = doc.safeDouble("defaultPrice") ?: 0.0
                ProcedureConfigEntity(id, name, colorHex, defaultDuration, defaultPrice)
            }

            val logsSnapshot = db.collection("clinic_action_logs").get().await()
            val restoredLogs = logsSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val timestamp = doc.safeLong("timestamp") ?: System.currentTimeMillis()
                val dateStr = doc.safeString("dateStr")
                val timeStr = doc.safeString("timeStr")
                val typeStr = doc.safeString("actionType", ActionLogType.BOOKING.name)
                val actionType = try { ActionLogType.valueOf(typeStr) } catch (e: Exception) { ActionLogType.BOOKING }
                val employeeId = doc.safeLong("employeeId") ?: 0L
                val employeeName = doc.safeString("employeeName")
                val employeeRole = doc.safeString("employeeRole")
                val patientName = doc.safeString("patientName")
                val patientPhone = doc.safeString("patientPhone")
                val appointmentId = doc.safeLong("appointmentId") ?: 0L
                val appointmentDate = doc.safeString("appointmentDate")
                val appointmentTime = doc.safeString("appointmentTime")
                val procedureName = doc.safeString("procedureName")
                val details = doc.safeString("details")
                val extraInfo = doc.safeString("extraInfo")

                ActionLogEntity(
                    id = id,
                    timestamp = timestamp,
                    dateStr = dateStr,
                    timeStr = timeStr,
                    actionType = actionType,
                    employeeId = employeeId,
                    employeeName = employeeName,
                    employeeRole = employeeRole,
                    patientName = patientName,
                    patientPhone = patientPhone,
                    appointmentId = appointmentId,
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime,
                    procedureName = procedureName,
                    details = details,
                    extraInfo = extraInfo
                )
            }

            val templatesSnapshot = db.collection("clinic_whatsapp_templates").get().await()
            val restoredTemplates = templatesSnapshot.documents.mapNotNull { doc ->
                val id = doc.safeLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val categoryStr = doc.safeString("category", WhatsAppTemplateCategory.BEFORE_APPOINTMENT.name)
                val category = try { WhatsAppTemplateCategory.valueOf(categoryStr) } catch (e: Exception) { WhatsAppTemplateCategory.BEFORE_APPOINTMENT }
                val title = doc.safeString("title")
                val messageText = doc.safeString("messageText")
                WhatsAppTemplateEntity(id = id, category = category, title = title, messageText = messageText)
            }

            Result.success(
                RestoredClinicData(
                    patients = restoredPatients,
                    appointments = restoredAppointments,
                    users = restoredUsers,
                    procedures = restoredProcedures,
                    actionLogs = restoredLogs,
                    templates = restoredTemplates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
