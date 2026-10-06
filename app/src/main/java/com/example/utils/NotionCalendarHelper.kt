package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppointmentEntity
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NotionCalendarHelper {

    fun generateICalendarContent(
        appointments: List<AppointmentEntity>,
        clinicName: String = "عيادة الأسنان"
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//DentalClinic//NotionCalendarSync//AR")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        sb.appendLine("X-WR-CALNAME:$clinicName")
        sb.appendLine("X-WR-TIMEZONE:Africa/Cairo")

        for (app in appointments) {
            val dateClean = app.dateStr.replace("-", "")
            val startClean = app.startTime.replace(":", "") + "00"
            val endClean = app.endTime.replace(":", "") + "00"
            val uid = "dental-app-${app.id}-${app.dateStr}@clinic.com"

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:$uid")
            sb.appendLine("DTSTAMP:${dateClean}T000000Z")
            sb.appendLine("DTSTART:${dateClean}T$startClean")
            sb.appendLine("DTEND:${dateClean}T$endClean")
            sb.appendLine("SUMMARY:${app.patientName} - ${app.procedureName}")
            sb.appendLine("DESCRIPTION:المريض: ${app.patientName}\\nالهاتف: ${app.patientPhone}\\nالإجراء: ${app.procedureName}\\nالحالة: ${app.status.label}\\nملاحظات: ${app.notes}")
            sb.appendLine("STATUS:CONFIRMED")
            sb.appendLine("END:VEVENT")
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    fun exportAndShareCalendar(context: Context, appointments: List<AppointmentEntity>) {
        try {
            val icsContent = generateICalendarContent(appointments)
            val cacheDir = File(context.cacheDir, "calendars")
            cacheDir.mkdirs()
            val file = File(cacheDir, "clinic_appointments.ics")
            val writer = FileWriter(file)
            writer.write(icsContent)
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقويم مواعيد عيادة الأسنان - Notion Calendar")
                putExtra(Intent.EXTRA_TEXT, "ملف تقويم مواعيد العيادة للربط والاستيراد مع Notion Calendar و Google Calendar")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة التقويم مع Notion / Calendar"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير التقويم: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openNotionCalendar(context: Context) {
        try {
            val url = "https://calendar.notion.so"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح Notion Calendar", Toast.LENGTH_SHORT).show()
        }
    }
}
