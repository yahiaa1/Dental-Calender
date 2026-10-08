package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.receiver.AppointmentReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AppointmentAlarmScheduler {

    fun scheduleOneHourReminder(
        context: Context,
        appointment: AppointmentEntity,
        customMessage: String = "",
        whatsappApiUrl: String = ""
    ) {
        // Do not schedule for cancelled appointments
        if (appointment.status == AppointmentStatus.CANCELLED) return

        try {
            val date = LocalDate.parse(appointment.dateStr)
            val parts = appointment.startTime.split(":")
            val time = LocalTime.of(parts[0].toInt(), parts[1].toInt())
            val appointmentDateTime = LocalDateTime.of(date, time)

            // Trigger time: exactly 1 hour before appointment
            val triggerDateTime = appointmentDateTime.minusHours(1)
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Only schedule if trigger time is in future
            if (triggerMillis <= System.currentTimeMillis()) return

            val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
                putExtra("appointment_id", appointment.id)
                putExtra("patient_name", appointment.patientName)
                putExtra("patient_phone", appointment.patientPhone)
                putExtra("procedure_name", appointment.procedureName)
                putExtra("price", appointment.price)
                putExtra("start_time", appointment.startTime)
                putExtra("custom_message", customMessage)
                putExtra("whatsapp_api_url", whatsappApiUrl)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointment.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun scheduleAllUpcoming(context: Context, appointments: List<AppointmentEntity>) {
        appointments.forEach { app ->
            if (app.status != AppointmentStatus.CANCELLED) {
                scheduleOneHourReminder(
                    context = context,
                    appointment = app,
                    customMessage = "نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام ${TimeFormatter.format12Hour(app.startTime)}"
                )
            }
        }
    }

    fun cancelReminder(context: Context, appointmentId: Long) {
        try {
            val intent = Intent(context, AppointmentReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appointmentId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
