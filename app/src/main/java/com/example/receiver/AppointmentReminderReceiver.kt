package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppointmentStatus
import com.example.utils.ReminderNotificationHelper
import com.example.utils.TimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appointmentId = intent.getLongExtra("appointment_id", 0L)
        if (appointmentId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verify against Room DB that the appointment is still valid and not cancelled
                val db = AppDatabase.getDatabase(context)
                val appointment = db.appointmentDao().getAppointmentById(appointmentId)

                // If appointment was cancelled, deleted or marked no-show, immediately dismiss and abort
                if (appointment == null ||
                    appointment.status == AppointmentStatus.CANCELLED ||
                    appointment.status == AppointmentStatus.NO_SHOW) {
                    ReminderNotificationHelper.dismissNotification(context, appointmentId)
                    return@launch
                }

                val patientName = appointment.patientName.ifBlank { intent.getStringExtra("patient_name") ?: "المريض" }
                val patientPhone = appointment.patientPhone.ifBlank { intent.getStringExtra("patient_phone") ?: "" }
                val procedureName = appointment.procedureName.ifBlank { intent.getStringExtra("procedure_name") ?: "كشف / استشارة" }
                val price = if (appointment.price > 0) appointment.price else intent.getDoubleExtra("price", 0.0)
                val startTime = appointment.startTime.ifBlank { intent.getStringExtra("start_time") ?: "" }
                val defaultMessage = intent.getStringExtra("custom_message") ?: "نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام $startTime"
                val whatsappApiUrl = intent.getStringExtra("whatsapp_api_url") ?: ""

                val time12 = TimeFormatter.format12Hour(startTime)

                // Wake screen if phone is locked so notification appears immediately
                try {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    @Suppress("DEPRECATION")
                    val wakeLock = powerManager?.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "DentalClinic:ReminderWakeLock"
                    )
                    wakeLock?.acquire(3000L)
                } catch (e: Exception) {
                    // Ignore if permission not allowed
                }

                // 1. Show local heads-up lockscreen notification with avatar, procedure & price
                ReminderNotificationHelper.showOneHourReminderNotification(
                    context = context,
                    appointmentId = appointmentId,
                    patientName = patientName,
                    patientPhone = patientPhone,
                    procedureName = procedureName,
                    price = price,
                    timeFormatted12 = time12,
                    customMessage = defaultMessage
                )

                // 2. If user configured a WhatsApp API Webhook/Cloud API URL, dispatch in background
                if (whatsappApiUrl.isNotBlank() && whatsappApiUrl.startsWith("http")) {
                    try {
                        val url = URL(whatsappApiUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true

                        val jsonPayload = """
                            {
                                "to": "$patientPhone",
                                "patient": "$patientName",
                                "procedure": "$procedureName",
                                "price": $price,
                                "time": "$time12",
                                "message": "$defaultMessage"
                            }
                        """.trimIndent()

                        conn.outputStream.use { os ->
                            os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                        }
                        val code = conn.responseCode
                        conn.disconnect()
                    } catch (e: Exception) {
                        // API call failure logged silently
                    }
                }
            } catch (e: Exception) {
                // Ignore safely
            } finally {
                pendingResult.finish()
            }
        }
    }
}
