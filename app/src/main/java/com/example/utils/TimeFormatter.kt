package com.example.utils

object TimeFormatter {

    /**
     * Converts a 24-hour time string like "09:00" or "14:30" or "21:00"
     * into a beautiful Arabic 12-hour format string like "09:00 ص" or "02:30 م".
     */
    fun format12Hour(time24: String): String {
        return try {
            val parts = time24.trim().split(":")
            if (parts.size < 2) return time24
            val hour24 = parts[0].toInt()
            val minute = parts[1].toInt()

            val isPm = hour24 >= 12
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val period = if (isPm) "م" else "ص"
            String.format("%02d:%02d %s", hour12, minute, period)
        } catch (e: Exception) {
            time24
        }
    }

    /**
     * Formats an hour integer (e.g. 9 or 14 or 21) to 12-hour label for the calendar timeline:
     * 9 -> "09:00 ص"
     * 12 -> "12:00 م"
     * 14 -> "02:00 م"
     * 21 -> "09:00 م"
     */
    fun formatHourLabel12(hour24: Int): String {
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val period = if (isPm) "م" else "ص"
        return String.format("%02d:00 %s", hour12, period)
    }

    /**
     * Exact Google Calendar format like in screenshot: "8 am", "9 am", "10 am", "11 am", "12 pm"
     */
    fun formatGoogleHourLabel(hour24: Int): String {
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val period = if (isPm) "pm" else "am"
        return "$hour12 $period"
    }

    /**
     * Formats a time range in 12-hour format:
     * "10:00", "10:45" -> "10:00 ص - 10:45 ص"
     */
    fun formatTimeRange12(startTime24: String, endTime24: String): String {
        return "${format12Hour(startTime24)} - ${format12Hour(endTime24)}"
    }
}
