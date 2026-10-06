package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val patientName = intent.getStringExtra("patient_name") ?: "المريض"
        val patientPhone = intent.getStringExtra("patient_phone") ?: ""
        val startTime = intent.getStringExtra("start_time") ?: ""
        val defaultMessage = intent.getStringExtra("custom_message") ?: "نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام $startTime"
        val whatsappApiUrl = intent.getStringExtra("whatsapp_api_url") ?: ""

        val time12 = TimeFormatter.format12Hour(startTime)

        // 1. Show local heads-up notification with WhatsApp action button
        ReminderNotificationHelper.showOneHourReminderNotification(
            context = context,
            appointmentId = appointmentId,
            patientName = patientName,
            patientPhone = patientPhone,
            timeFormatted12 = time12,
            customMessage = defaultMessage
        )

        // 2. If user configured a WhatsApp API Webhook/Cloud API URL, dispatch in background
        if (whatsappApiUrl.isNotBlank() && whatsappApiUrl.startsWith("http")) {
            CoroutineScope(Dispatchers.IO).launch {
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
        }
    }
}
