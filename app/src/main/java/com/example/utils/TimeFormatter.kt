package com.example.utils

object TimeFormatter {

    /**
     * Converts Arabic-Indic digits to Western digits (0-9)
     */
    fun normalizeArabicDigits(input: String): String {
        return input
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
    }

    /**
     * Parses any time representation (24-hour, 12-hour with AM/PM or ص/م, Arabic numerals, etc.)
     * into total minutes from start of day (0..1439).
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        try {
            val normalized = normalizeArabicDigits(timeStr).trim()
            val isPm = normalized.contains("م") || normalized.contains("pm", ignoreCase = true)
            val isAm = normalized.contains("ص") || normalized.contains("am", ignoreCase = true)
            val digitsOnly = normalized.replace(Regex("[^0-9:]"), "")
            val parts = digitsOnly.split(":")
            if (parts.isNotEmpty()) {
                var hour = parts[0].toIntOrNull() ?: 0
                val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

                if (isPm && hour < 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }
                return (hour.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
            }
        } catch (e: Exception) {
            // fallback
        }
        return 0
    }

    /**
     * Normalizes a time string into standard 24-hour HH:mm format (e.g. "09:30" or "16:00")
     */
    fun normalizeTime24(timeStr: String): String {
        if (timeStr.isBlank()) return "10:00"
        val totalMinutes = parseTimeToMinutes(timeStr)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return java.lang.String.format(java.util.Locale.US, "%02d:%02d", h, m)
    }

    /**
     * Converts a 24-hour time string like "09:00" or "14:30" or "21:00"
     * into a beautiful Arabic 12-hour format string like "09:00 ص" or "02:30 م".
     */
    fun format12Hour(time24: String): String {
        return try {
            val clean = normalizeArabicDigits(time24.trim())
            val parts = clean.split(":")
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
            java.lang.String.format(java.util.Locale.US, "%02d:%02d %s", hour12, minute, period)
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
        return java.lang.String.format(java.util.Locale.US, "%02d:00 %s", hour12, period)
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
