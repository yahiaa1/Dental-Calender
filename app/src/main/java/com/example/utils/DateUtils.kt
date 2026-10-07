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
     * Returns the days of the week containing the given date, starting from Saturday to Friday.
     * Optionally filters out the configured day off if hideDayOff is true.
     */
    fun getWeekDays(
        centerDate: LocalDate,
        hideDayOff: Boolean = false,
        dayOff: DayOfWeek? = DayOfWeek.FRIDAY
    ): List<LocalDate> {
        // Saturday = DayOfWeek.SATURDAY (value 6). (6 + 1) % 7 = 0
        // Sunday = DayOfWeek.SUNDAY (value 7). (7 + 1) % 7 = 1
        // Friday = DayOfWeek.FRIDAY (value 5). (5 + 1) % 7 = 6
        val daysFromSaturday = (centerDate.dayOfWeek.value + 1) % 7
        val saturday = centerDate.minusDays(daysFromSaturday.toLong())
        val allDays = (0..6).map { saturday.plusDays(it.toLong()) }
        return if (hideDayOff && dayOff != null) {
            allDays.filter { it.dayOfWeek != dayOff }
        } else {
            allDays
        }
    }

    data class MonthCalendarDay(
        val date: LocalDate,
        val isCurrentMonth: Boolean,
        val isSelected: Boolean,
        val isToday: Boolean
    )

    /**
     * Generates a grid of days for a monthly calendar view starting from Saturday.
     * Optionally filters out the configured day off if hideDayOff is true.
     */
    fun getMonthDaysGrid(
        yearMonth: LocalDate,
        selectedDate: LocalDate,
        hideDayOff: Boolean = false,
        dayOff: DayOfWeek? = DayOfWeek.FRIDAY
    ): List<MonthCalendarDay> {
        val allWeekDays = listOf(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        val activeDays = if (hideDayOff && dayOff != null) {
            allWeekDays.filter { it != dayOff }
        } else {
            allWeekDays
        }
        val numCols = activeDays.size // 6 if dayOff hidden, 7 otherwise

        val firstOfMonth = yearMonth.withDayOfMonth(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now()

        // Find the first active day on or after firstOfMonth
        var firstActiveDate = firstOfMonth
        while (hideDayOff && dayOff != null && firstActiveDate.dayOfWeek == dayOff) {
            firstActiveDate = firstActiveDate.plusDays(1)
        }

        // Which column in activeDays does firstActiveDate belong to?
        val startCol = activeDays.indexOf(firstActiveDate.dayOfWeek).coerceAtLeast(0)

        val list = mutableListOf<MonthCalendarDay>()

        // Preceding active days from previous month to fill the first row before startCol
        var prevDate = firstActiveDate.minusDays(1)
        val precedingList = mutableListOf<MonthCalendarDay>()
        var countPrecedingNeeded = startCol
        while (countPrecedingNeeded > 0) {
            if (!hideDayOff || dayOff == null || prevDate.dayOfWeek != dayOff) {
                precedingList.add(
                    0,
                    MonthCalendarDay(
                        date = prevDate,
                        isCurrentMonth = false,
                        isSelected = prevDate == selectedDate,
                        isToday = prevDate == today
                    )
                )
                countPrecedingNeeded--
            }
            prevDate = prevDate.minusDays(1)
        }
        list.addAll(precedingList)

        // Days of current month (excluding dayOff if hideDayOff is true)
        for (day in 1..daysInMonth) {
            val date = yearMonth.withDayOfMonth(day)
            if (!hideDayOff || dayOff == null || date.dayOfWeek != dayOff) {
                list.add(
                    MonthCalendarDay(
                        date = date,
                        isCurrentMonth = true,
                        isSelected = date == selectedDate,
                        isToday = date == today
                    )
                )
            }
        }

        // Trailing active days from next month to fill grid to full rows
        val minCells = numCols * 5 // 30 cells for 6-day week, 35 for 7-day week
        var nextDate = yearMonth.withDayOfMonth(daysInMonth).plusDays(1)
        while (list.size % numCols != 0 || list.size < minCells) {
            if (!hideDayOff || dayOff == null || nextDate.dayOfWeek != dayOff) {
                list.add(
                    MonthCalendarDay(
                        date = nextDate,
                        isCurrentMonth = false,
                        isSelected = nextDate == selectedDate,
                        isToday = nextDate == today
                    )
                )
            }
            nextDate = nextDate.plusDays(1)
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
