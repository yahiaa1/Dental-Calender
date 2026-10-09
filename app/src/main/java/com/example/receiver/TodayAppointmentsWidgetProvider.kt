package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.utils.DateUtils
import java.time.LocalDate

class TodayAppointmentsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_WIDGET ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            notifyDataChanged(context)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.UPDATE_TODAY_WIDGET"

        fun notifyDataChanged(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, TodayAppointmentsWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_appointments_list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_appointments)

            val today = LocalDate.now()
            var count = 0
            var totalPrice = 0.0

            try {
                val db = AppDatabase.getDatabase(context)
                val allApps = db.appointmentDao().getAllAppointmentsSync()
                val todayApps = allApps.filter { DateUtils.isSameDay(it.dateStr, today) }
                val validTodayApps = todayApps.filter { it.status != com.example.data.model.AppointmentStatus.CANCELLED && it.status != com.example.data.model.AppointmentStatus.NO_SHOW }
                count = validTodayApps.size
                totalPrice = validTodayApps.sumOf { it.price }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Header matching user screenshot
            views.setTextViewText(R.id.widget_title, "تفاصيل المواعيد ($count)")
            views.setTextViewText(R.id.widget_total_revenue, "إجمالي: ${totalPrice.toInt()} ج.م")

            // Empty view binding
            views.setEmptyView(R.id.widget_appointments_list, R.id.widget_empty_view)

            // RemoteViews service adapter
            val serviceIntent = Intent(context, TodayAppointmentsWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.fromParts("content", appWidgetId.toString(), null)
            }
            views.setRemoteAdapter(R.id.widget_appointments_list, serviceIntent)

            // Open app on clicking header or empty view
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

            // PendingIntent template for clicking on appointment items
            val clickItemIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickItemPendingIntent = PendingIntent.getActivity(
                context,
                3,
                clickItemIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_appointments_list, clickItemPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
