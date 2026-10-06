package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    private val ARABIC_MONTHS = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val ARABIC_DAYS = mapOf(
        DayOfWeek.SUNDAY to "الأحد",
        DayOfWeek.MONDAY to "الاثنين",
        DayOfWeek.TUESDAY to "الثلاثاء",
        DayOfWeek.WEDNESDAY to "الأربعاء",
        DayOfWeek.THURSDAY to "الخميس",
        DayOfWeek.FRIDAY to "الجمعة",
        DayOfWeek.SATURDAY to "السبت"
    )

    private val ARABIC_DAYS_SHORT = mapOf(
        DayOfWeek.SUNDAY to "أحد",
        DayOfWeek.MONDAY to "اثنين",
        DayOfWeek.TUESDAY to "ثلاثاء",
        DayOfWeek.WEDNESDAY to "أربعاء",
        DayOfWeek.THURSDAY to "خميس",
        DayOfWeek.FRIDAY to "جمعة",
        DayOfWeek.SATURDAY to "سبت"
    )

    fun getArabicMonthName(monthNumber: Int): String {
        return if (monthNumber in 1..12) ARABIC_MONTHS[monthNumber - 1] else ""
    }

    fun getArabicDayName(dayOfWeek: DayOfWeek): String {
        return ARABIC_DAYS[dayOfWeek] ?: ""
    }

    fun getArabicDayShortName(dayOfWeek: DayOfWeek): String {
        return ARABIC_DAYS_SHORT[dayOfWeek] ?: ""
    }

    fun formatArabicFullDate(date: LocalDate): String {
        val dayName = getArabicDayName(date.dayOfWeek)
        val dayOfMonth = date.dayOfMonth
        val monthName = getArabicMonthName(date.monthValue)
        val year = date.year
        return "$dayName $dayOfMonth $monthName $year"
    }

    fun formatArabicMonthYear(date: LocalDate): String {
        val monthName = getArabicMonthName(date.monthValue)
        val year = date.year
        return "$monthName $year"
    }

    /**
     * Exact Google Calendar month header format: "October" or "October 2026"
     */
    fun formatEnglishMonth(date: LocalDate): String {
        return date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * Day of week abbreviation matching Google Calendar screenshot: "Thu", "Fri", "Sat"
     */
    fun getEnglishDayShortName(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }

    fun toDateString(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD
    }

    fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    /**
     * Returns the 7 days of the week containing the given date, starting from Sunday.
     */
    fun getWeekDays(centerDate: LocalDate): List<LocalDate> {
        val daysFromSunday = (centerDate.dayOfWeek.value % 7) // Sunday=7->0, Monday=1, etc.
        val sunday = centerDate.minusDays(daysFromSunday.toLong())
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }

    data class MonthCalendarDay(
        val date: LocalDate,
        val isCurrentMonth: Boolean,
        val isSelected: Boolean,
        val isToday: Boolean
    )

    /**
     * Generates a 35 or 42 grid of days for a monthly calendar view starting from Sunday
     */
    fun getMonthDaysGrid(yearMonth: LocalDate, selectedDate: LocalDate): List<MonthCalendarDay> {
        val firstOfMonth = yearMonth.withDayOfMonth(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val startDayOfWeek = firstOfMonth.dayOfWeek.value % 7 // 0 for Sunday, 1 for Monday...

        val list = mutableListOf<MonthCalendarDay>()
        val today = LocalDate.now()

        // Days from previous month
        for (i in startDayOfWeek downTo 1) {
            val date = firstOfMonth.minusDays(i.toLong())
            list.add(MonthCalendarDay(date, isCurrentMonth = false, isSelected = date == selectedDate, isToday = date == today))
        }

        // Days of current month
        for (day in 1..daysInMonth) {
            val date = yearMonth.withDayOfMonth(day)
            list.add(MonthCalendarDay(date, isCurrentMonth = true, isSelected = date == selectedDate, isToday = date == today))
        }

        // Trailing days from next month to fill grid
        var nextMonthDate = yearMonth.withDayOfMonth(daysInMonth).plusDays(1)
        while (list.size % 7 != 0 || list.size < 35) {
            list.add(MonthCalendarDay(nextMonthDate, isCurrentMonth = false, isSelected = nextMonthDate == selectedDate, isToday = nextMonthDate == today))
            nextMonthDate = nextMonthDate.plusDays(1)
        }

        return list
    }
}

object ClinicActions {
    fun openWhatsApp(context: Context, rawPhone: String, messageText: String) {
        try {
            var phone = rawPhone.replace(Regex("[^0-9+]"), "")
            if (phone.startsWith("0")) {
                phone = "2$phone" // Default to Egypt code or clean number
            }
            val encodedMessage = Uri.encode(messageText)
            val uri = Uri.parse("https://wa.me/$phone?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Try without package if WA business or browser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun makeCall(context: Context, rawPhone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawPhone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح سجل الهاتف", Toast.LENGTH_SHORT).show()
        }
    }
}
