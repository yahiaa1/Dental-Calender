package com.example.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object ReminderNotificationHelper {

    const val CHANNEL_ID = "clinic_appointment_reminders"
    const val CHANNEL_NAME = "تنبيهات مواعيد المرضى (قبل ساعة)"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = "إرسال تنبيهات تلقائية قبل ميعاد الإجراء بساعة"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 350, 150, 350)
                    enableLights(true)
                    lightColor = android.graphics.Color.CYAN
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Generates a circular avatar bitmap matching modern messaging notifications
     * as shown in the lockscreen screenshot (e.g. Matt / Rebecca style avatar circle).
     */
    fun createAvatarBitmap(name: String): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Palette of pleasant, modern avatar colors
        val colors = listOf(
            0xFFE91E63.toInt(), // Rose / Pink
            0xFF673AB7.toInt(), // Deep Purple
            0xFF00897B.toInt(), // Teal / Clinic
            0xFF1E88E5.toInt(), // Blue
            0xFFFF7043.toInt(), // Coral
            0xFF8E24AA.toInt(), // Purple
            0xFF43A047.toInt()  // Emerald
        )
        val charCode = name.firstOrNull()?.code ?: 0
        val bgColor = colors[Math.floorMod(charCode, colors.size)]

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // Initial letter
        val initial = name.trim().firstOrNull()?.toString()?.uppercase() ?: "م"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * 0.48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val yPos = (radius - ((textPaint.descent() + textPaint.ascent()) / 2))
        canvas.drawText(initial, radius, yPos, textPaint)

        return bitmap
    }

    fun showOneHourReminderNotification(
        context: Context,
        appointmentId: Long,
        patientName: String,
        patientPhone: String = "",
        procedureName: String = "كشف / استشارة",
        price: Double = 0.0,
        timeFormatted12: String = "",
        customMessage: String = ""
    ) {
        createNotificationChannel(context)

        // Format price
        val priceFormatted = if (price > 0) "${price.toInt()} ج.م" else "0 ج.م"

        // Intent to open app directly on appointment
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("appointment_id", appointmentId)
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
        val whatsappMsg = if (customMessage.isNotBlank()) customMessage else "مرحباً $patientName، نذكرك بموعدك في العيادة بعد ساعة في تمام $timeFormatted12."
        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(whatsappMsg)}")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val whatsappPendingIntent = PendingIntent.getActivity(
            context,
            (appointmentId + 10000).toInt(),
            whatsappIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val avatar = createAvatarBitmap(patientName)

        val contentText = "الإجراء: $procedureName • الحساب: $priceFormatted"
        val bigText = buildString {
            append("نوع الإجراء: ").append(procedureName).append("\n")
            append("الحساب: ").append(priceFormatted).append("\n")
            if (timeFormatted12.isNotBlank()) {
                append("الموعد: في تمام الساعة ").append(timeFormatted12)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(avatar)
            .setContentTitle(patientName) // Style matches screenshot: Name bold at top
            .setContentText(contentText)
            .setSubText("تذكير موعد (قبل ساعة)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(patientName)
                    .setSummaryText("موعد بعد ساعة")
                    .bigText(bigText)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Shows full content even on locked screen!
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
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
