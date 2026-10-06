package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object ReminderNotificationHelper {

    const val CHANNEL_ID = "clinic_appointment_reminders"
    const val CHANNEL_NAME = "تنبيهات مواعيد المرضى (قبل ساعة)"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "إرسال تنبيهات تلقائية لموظف العيادة والمريض قبل الموعد بساعة واحدة"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOneHourReminderNotification(
        context: Context,
        appointmentId: Long,
        patientName: String,
        patientPhone: String,
        timeFormatted12: String,
        customMessage: String
    ) {
        createNotificationChannel(context)

        // Intent to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct WhatsApp Intent
        val cleanPhone = patientPhone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.startsWith("0")) "20" + cleanPhone.substring(1) else cleanPhone
        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(customMessage)}")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val whatsappPendingIntent = PendingIntent.getActivity(
            context,
            (appointmentId + 10000).toInt(),
            whatsappIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ تذكير: موعد خلال ساعة لـ $patientName")
            .setContentText("الموعد الساعة $timeFormatted12 • انقر لإرسال تذكير واتساب للمريض الآن")
            .setStyle(NotificationCompat.BigTextStyle().bigText("موعد $patientName يبدأ خلال ساعة في تمام $timeFormatted12.\nنص التذكير: $customMessage"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_send,
                "إرسال واتساب للمريض 💬",
                whatsappPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(appointmentId.toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }
}
