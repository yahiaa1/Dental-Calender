package com.example.receiver

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.AppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.utils.DateUtils
import com.example.utils.TimeFormatter
import java.time.LocalDate

class TodayAppointmentsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayAppointmentsRemoteViewsFactory(applicationContext)
    }
}

class TodayAppointmentsRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var appointments: List<AppointmentEntity> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val db = AppDatabase.getDatabase(context)
            val today = LocalDate.now()
            appointments = db.appointmentDao().getAllAppointmentsSync()
                .filter { DateUtils.isSameDay(it.dateStr, today) }
        } catch (e: Exception) {
            e.printStackTrace()
            appointments = emptyList()
        }
    }

    override fun onDestroy() {
        appointments = emptyList()
    }

    override fun getCount(): Int = appointments.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in appointments.indices) return null
        val item = appointments[position]

        val views = RemoteViews(context.packageName, R.layout.widget_appointment_item)

        // 1. Patient Name
        views.setTextViewText(R.id.widget_item_patient_name, item.patientName)

        // 2. Procedure and Time range: e.g. "كشف • 10:00 ص - 11:45 ص"
        val timeRange = TimeFormatter.formatTimeRange12(item.startTime, item.endTime)
        views.setTextViewText(R.id.widget_item_procedure_time, "${item.procedureName} • $timeRange")

        // 3. Date and Booked by staff: e.g. "📅 2026-10-08   👤 حجز: د. محمد (أدمن)"
        views.setTextViewText(R.id.widget_item_details_footer, "📅 ${item.dateStr}   👤 حجز: ${item.bookedByUserName}")

        // 4. Price: e.g. "500 ج.م"
        views.setTextViewText(R.id.widget_item_price, "${item.price.toInt()} ج.م")

        // 5. Status Badge Pill matching screenshot
        val (statusText, bgRes, textColor) = when (item.status) {
            AppointmentStatus.UNCONFIRMED -> Triple("⌛ لم يتم التاكيد", R.drawable.widget_status_unconfirmed_bg, Color.parseColor("#B45309"))
            AppointmentStatus.CONFIRMED -> Triple("✓ مؤكد", R.drawable.widget_status_confirmed_bg, Color.parseColor("#15803D"))
            AppointmentStatus.IN_TREATMENT -> Triple("⚡ جار العلاج", R.drawable.widget_status_intreatment_bg, Color.parseColor("#0369A1"))
            AppointmentStatus.COMPLETED -> Triple("✔ مكتمل", R.drawable.widget_status_completed_bg, Color.parseColor("#047857"))
            AppointmentStatus.CANCELLED -> Triple("✕ ملغي", R.drawable.widget_status_cancelled_bg, Color.parseColor("#B91C1C"))
            AppointmentStatus.NO_SHOW -> Triple("⚠ لم يحضر", R.drawable.widget_status_noshow_bg, Color.parseColor("#4B5563"))
        }

        views.setTextViewText(R.id.widget_item_status, statusText)
        views.setInt(R.id.widget_item_status, "setBackgroundResource", bgRes)
        views.setTextColor(R.id.widget_item_status, textColor)

        // Fill-in Intent for opening appointment details when clicked
        val fillInIntent = Intent().apply {
            putExtra("appointment_id", item.id)
            putExtra("date_str", item.dateStr)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in appointments.indices) appointments[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
