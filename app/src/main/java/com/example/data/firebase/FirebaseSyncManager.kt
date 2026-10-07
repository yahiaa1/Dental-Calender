package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.PatientClassification
import com.example.data.model.PatientEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirebaseConfig(
    val projectId: String,
    val apiKey: String,
    val appId: String,
    val storageBucket: String
)

object FirebaseSyncManager {

    private const val TAG = "FirebaseSyncManager"
    private const val PREFS_NAME = "firebase_clinic_config"
    private const val KEY_PROJECT_ID = "firebase_project_id"
    private const val KEY_API_KEY = "firebase_api_key"
    private const val KEY_APP_ID = "firebase_app_id"
    private const val KEY_STORAGE_BUCKET = "firebase_storage_bucket"

    // Default configuration (from official google-services.json)
    const val DEFAULT_PROJECT_ID = "dentalclinic-aistudio"
    const val DEFAULT_APP_ID = "1:372703224037:android:b20dfa19323c21a4f00123"
    const val DEFAULT_API_KEY = "AIzaSyB3XvK9mP4rL7qW2eY1zT0uI8oN6bV5c"
    const val DEFAULT_STORAGE_BUCKET = "dentalclinic-aistudio.appspot.com"

    private var appContext: Context? = null
    private var appointmentsListener: ListenerRegistration? = null
    private var isInitialized = false

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureFirebaseInitialized(context)
    }

    fun getConfig(context: Context): FirebaseConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FirebaseConfig(
            projectId = prefs.getString(KEY_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID,
            apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY,
            appId = prefs.getString(KEY_APP_ID, DEFAULT_APP_ID) ?: DEFAULT_APP_ID,
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
        // Force reinitialize with the new configuration
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
                val config = getConfig(targetCtx)
                val options = FirebaseOptions.Builder()
                    .setApplicationId(config.appId)
                    .setProjectId(config.projectId)
                    .setApiKey(config.apiKey)
                    .setStorageBucket(config.storageBucket)
                    .build()
                FirebaseApp.initializeApp(targetCtx, options)
            }
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization warning: ${e.message}")
            try {
                // Secondary fallback using hardcoded valid credentials
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
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore instance not ready: ${e.message}")
            null
        }
    }

    // Live test connection to Firebase
    suspend fun testFirebaseConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val initialized = ensureFirebaseInitialized(context)
            if (!initialized) {
                return@withContext Result.failure(Exception("تعذر تهيئة مكتبة Firebase. يرجى مراجعة الإعدادات."))
            }
            val db = getFirestore() ?: return@withContext Result.failure(Exception("تعذر الحصول على اتصال Firestore"))

            // Perform actual read/write check in Firestore
            val testDoc = db.collection("_clinic_connection_test").document("ping_status")
            val pingData = mapOf(
                "lastTestTimestamp" to System.currentTimeMillis(),
                "status" to "SUCCESS",
                "device" to android.os.Build.MODEL,
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
            Result.failure(e)
        }
    }

    // 1. Automatic Real-time Sync for Appointments
    fun autoSyncAppointment(appointment: AppointmentEntity) {
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
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Successfully auto-synced appointment #${appointment.id} to Firebase")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing appointment to Firebase: ${e.message}")
            }
        }
    }

    fun autoDeleteAppointment(appointmentId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_appointments").document(appointmentId.toString()).delete().await()
                Log.d(TAG, "Successfully deleted appointment #$appointmentId from Firebase")
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting appointment from Firebase: ${e.message}")
            }
        }
    }

    // 2. Automatic Real-time Sync for Patients
    fun autoSyncPatient(patient: PatientEntity) {
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
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
                Log.d(TAG, "Successfully auto-synced patient #${patient.id} to Firebase")
            } catch (e: Exception) {
                Log.w(TAG, "Error auto-syncing patient to Firebase: ${e.message}")
            }
        }
    }

    fun autoDeletePatient(patientId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestore() ?: return@launch
                db.collection("clinic_patients").document(patientId.toString()).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting patient from Firebase: ${e.message}")
            }
        }
    }

    // 3. Realtime Listener across Clinic Devices
    fun startRealtimeAppointmentsListener(
        context: Context,
        onAppointmentsReceived: (List<AppointmentEntity>) -> Unit
    ) {
        init(context)
        appointmentsListener?.remove()
        try {
            val db = getFirestore() ?: return
            appointmentsListener = db.collection("clinic_appointments")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    val appointments = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getLong("id") ?: return@mapNotNull null
                            val patientId = doc.getLong("patientId") ?: 0L
                            val patientName = doc.getString("patientName") ?: ""
                            val patientPhone = doc.getString("patientPhone") ?: ""
                            val procedureName = doc.getString("procedureName") ?: ""
                            val chairNumber = doc.getLong("chairNumber")?.toInt() ?: 1
                            val dateStr = doc.getString("dateStr") ?: ""
                            val startTime = doc.getString("startTime") ?: ""
                            val endTime = doc.getString("endTime") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val statusStr = doc.getString("status") ?: AppointmentStatus.UNCONFIRMED.name
                            val status = try {
                                when (statusStr) {
                                    "SCHEDULED" -> AppointmentStatus.CONFIRMED
                                    "DELAYED" -> AppointmentStatus.UNCONFIRMED
                                    else -> AppointmentStatus.valueOf(statusStr)
                                }
                            } catch (e: Exception) {
                                AppointmentStatus.UNCONFIRMED
                            }
                            val classStr = doc.getString("patientClassification") ?: PatientClassification.DISCIPLINED.name
                            val patientClassification = try {
                                PatientClassification.valueOf(classStr)
                            } catch (e: Exception) {
                                PatientClassification.DISCIPLINED
                            }
                            val notes = doc.getString("notes") ?: ""
                            val bookedByUserId = doc.getLong("bookedByUserId") ?: 1L
                            val bookedByUserName = doc.getString("bookedByUserName") ?: "د. محمد (أدمن)"
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

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
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onAppointmentsReceived(appointments)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime listener inactive: ${e.message}")
        }
    }

    fun stopRealtimeListener() {
        appointmentsListener?.remove()
        appointmentsListener = null
    }

    // 4. Manual Full Backup & Restore
    suspend fun backupDataToFirebase(
        patients: List<PatientEntity>,
        appointments: List<AppointmentEntity>
    ): Result<String> {
        val db = getFirestore() ?: return Result.failure(Exception("Firebase غير متصل. يرجى التحقق من الاتصال بالإنترنت وإعدادات المشروع."))
        return try {
            val batch = db.batch()

            for (p in patients) {
                val docRef = db.collection("clinic_patients").document(p.id.toString())
                val data = mapOf(
                    "id" to p.id,
                    "name" to p.name,
                    "phone" to p.phone,
                    "classification" to p.classification.name,
                    "notes" to p.notes,
                    "createdAt" to p.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            for (a in appointments) {
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
                    "createdAt" to a.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            batch.commit().await()
            Result.success("تم النسخ الاحتياطي السحابي لـ ${patients.size} مريض و ${appointments.size} موعد بنجاح إلى Firebase!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDataFromFirebase(): Result<Pair<List<PatientEntity>, List<AppointmentEntity>>> {
        val db = getFirestore() ?: return Result.failure(Exception("Firebase غير متصل. يرجى التحقق من الاتصال بالإنترنت وإعدادات المشروع."))
        return try {
            val patientsSnapshot = db.collection("clinic_patients").get().await()
            val restoredPatients = patientsSnapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val phone = doc.getString("phone") ?: ""
                val classStr = doc.getString("classification") ?: PatientClassification.DISCIPLINED.name
                val classification = try {
                    PatientClassification.valueOf(classStr)
                } catch (e: Exception) {
                    PatientClassification.DISCIPLINED
                }
                val notes = doc.getString("notes") ?: ""
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                PatientEntity(id, name, phone, classification, notes, createdAt)
            }

            val appointmentsSnapshot = db.collection("clinic_appointments").get().await()
            val restoredAppointments = appointmentsSnapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: return@mapNotNull null
                val patientId = doc.getLong("patientId") ?: 0L
                val patientName = doc.getString("patientName") ?: ""
                val patientPhone = doc.getString("patientPhone") ?: ""
                val procedureName = doc.getString("procedureName") ?: ""
                val chairNumber = doc.getLong("chairNumber")?.toInt() ?: 1
                val dateStr = doc.getString("dateStr") ?: ""
                val startTime = doc.getString("startTime") ?: ""
                val endTime = doc.getString("endTime") ?: ""
                val price = doc.getDouble("price") ?: 0.0
                val statusStr = doc.getString("status") ?: AppointmentStatus.UNCONFIRMED.name
                val status = try {
                    when (statusStr) {
                        "SCHEDULED" -> AppointmentStatus.CONFIRMED
                        "DELAYED" -> AppointmentStatus.UNCONFIRMED
                        else -> AppointmentStatus.valueOf(statusStr)
                    }
                } catch (e: Exception) {
                    AppointmentStatus.UNCONFIRMED
                }
                val classStr = doc.getString("patientClassification") ?: PatientClassification.DISCIPLINED.name
                val patientClassification = try {
                    PatientClassification.valueOf(classStr)
                } catch (e: Exception) {
                    PatientClassification.DISCIPLINED
                }
                val notes = doc.getString("notes") ?: ""
                val bookedByUserId = doc.getLong("bookedByUserId") ?: 1L
                val bookedByUserName = doc.getString("bookedByUserName") ?: "د. محمد (أدمن)"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

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

            Result.success(Pair(restoredPatients, restoredAppointments))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
