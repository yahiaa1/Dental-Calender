package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

// Restored to the original beloved 5 statuses as explicitly requested:
// "رجع الحالة لنفس النظام القديم"
enum class AppointmentStatus(val label: String, val colorHex: String) {
    UNCONFIRMED("لم يتم التاكيد", "#FFA000"), // Amber / Yellow
    CONFIRMED("مؤكد", "#1E88E5"),             // Blue
    IN_TREATMENT("جار العلاج", "#8E24AA"),     // Purple
    COMPLETED("مكتمل", "#43A047"),             // Green
    CANCELLED("ملغي", "#E53935"),              // Red
    NO_SHOW("لم يحضر", "#757575");             // Gray

    fun toComposeColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF1E88E5)
        }
    }
}

enum class PatientClassification(val title: String, val description: String, val colorHex: String) {
    DISCIPLINED("منضبط", "يحضر في مواعيده بانتظام", "#2E7D32"),
    OFTEN_LATE("متأخر غالباً", "يتأخر دائماً عن مواعيده", "#F57F17"),
    FREQUENT_ABSENT("كثير الغياب", "يتغيب عن المواعيد ويلغي", "#D32F2F"),
    NEW("جديد", "مريض جديد لأول مرة", "#455A64");

    fun toComposeColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF2E7D32)
        }
    }
}

// 3 days instead of 4 days as requested: "بلاش عرض 4 ايام كفاية 3 ايام بس بدل 4"
enum class CalendarViewMode(val label: String) {
    DAY("يوم"),
    THREE_DAYS("3 أيام"),
    WEEK("أسبوع"),
    MONTH("شهر")
}

enum class WhatsAppTemplateCategory(val label: String) {
    ONE_HOUR_BEFORE("قبل الموعد بساعة"),
    BEFORE_APPOINTMENT("قبل الموعد"),
    AFTER_TREATMENT("بعد العلاج"),
    CONFIRMATION("تأكيد الحجز"),
    CANCELLATION("إلغاء الموعد")
}

enum class UserRole(val title: String) {
    ADMIN("طبيب / أدمن"),
    STAFF("موظف استقبال / مساعد")
}

enum class ActionLogType(val label: String, val colorHex: String) {
    BOOKING("حجز جديد", "#2E7D32"),
    MOVE("نقل موعد", "#1E88E5"),
    STATUS_CHANGE("تغيير حالة", "#E65100"),
    EDIT("تعديل تفاصيل", "#6A1B9A"),
    DELETE("إلغاء / حذف", "#C62828");

    fun toComposeColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF1E88E5)
        }
    }
}
