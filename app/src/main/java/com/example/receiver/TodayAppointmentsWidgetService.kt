package com.example.receiver

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.AppointmentEntity
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
        // Initial setup
    }

    override fun onDataSetChanged() {
        try {
            val db = AppDatabase.getDatabase(context)
            val todayStr = DateUtils.toDateString(LocalDate.now())
            appointments = db.appointmentDao().getAppointmentsForDateSync(todayStr)
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
        views.setTextViewText(R.id.widget_item_patient_name, item.patientName)
        views.setTextViewText(R.id.widget_item_procedure, "• ${item.procedureName}")
        views.setTextViewText(R.id.widget_item_booked_by, "👤 حجز: ${item.bookedByUserName}")
        views.setTextViewText(R.id.widget_item_time, TimeFormatter.format12Hour(item.startTime))
        views.setTextViewText(R.id.widget_item_status, item.status.label)

        // Item click Intent
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
