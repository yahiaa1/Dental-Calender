package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppConverters {
    @TypeConverter
    fun fromAppointmentStatus(status: AppointmentStatus?): String {
        return status?.name ?: AppointmentStatus.UNCONFIRMED.name
    }

    @TypeConverter
    fun toAppointmentStatus(value: String?): AppointmentStatus {
        if (value == null) return AppointmentStatus.UNCONFIRMED
        return try {
            when (value) {
                "SCHEDULED" -> AppointmentStatus.CONFIRMED
                "DELAYED" -> AppointmentStatus.UNCONFIRMED
                else -> AppointmentStatus.valueOf(value)
            }
        } catch (e: Exception) {
            AppointmentStatus.UNCONFIRMED
        }
    }

    @TypeConverter
    fun fromPatientClassification(classification: PatientClassification?): String {
        return classification?.name ?: PatientClassification.DISCIPLINED.name
    }

    @TypeConverter
    fun toPatientClassification(value: String?): PatientClassification {
        if (value == null) return PatientClassification.DISCIPLINED
        return try {
            PatientClassification.valueOf(value)
        } catch (e: Exception) {
            PatientClassification.DISCIPLINED
        }
    }
}

@Database(
    entities = [
        PatientEntity::class,
        AppointmentEntity::class,
        ProcedureConfigEntity::class,
        WhatsAppTemplateEntity::class,
        UserEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun procedureConfigDao(): ProcedureConfigDao
    abstract fun whatsappTemplateDao(): WhatsAppTemplateDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dental_clinic_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialConfigsOnly(database)
                }
            }
        }
    }
}

suspend fun populateInitialConfigsOnly(db: AppDatabase) {
    // 1. Initial Admin User only (admin / 1234655)
    val adminUser = UserEntity(
        id = 1,
        name = "د. محمد (أدمن)",
        phone = "01000000001",
        role = UserRole.ADMIN,
        username = "admin",
        password = "1234655"
    )
    db.userDao().insertUser(adminUser)

    // 2. Procedures setup
    val procedures = listOf(
        ProcedureConfigEntity(1, "كشف", "#4CAF50", 30, 200.0),
        ProcedureConfigEntity(2, "حشو", "#2196F3", 45, 450.0),
        ProcedureConfigEntity(3, "تنظيف الأسنان", "#E91E63", 40, 350.0),
        ProcedureConfigEntity(4, "علاج عصب", "#FF9800", 60, 800.0),
        ProcedureConfigEntity(5, "تركيبات", "#9C27B0", 60, 1200.0),
        ProcedureConfigEntity(6, "جراحة", "#FF5722", 60, 1500.0),
        ProcedureConfigEntity(7, "متابعة", "#607D8B", 20, 100.0)
    )
    db.procedureConfigDao().insertProcedures(procedures)

    // 3. WhatsApp Templates setup
    val templates = listOf(
        WhatsAppTemplateEntity(
            1,
            WhatsAppTemplateCategory.ONE_HOUR_BEFORE,
            "تذكير قبل الموعد بساعة",
            "مرحباً {اسم المريض} ⏰ نذكرك بموعدك في عيادة الأسنان بعد ساعة في تمام {وقت الموعد}. نحن في انتظار زيارتك!"
        ),
        WhatsAppTemplateEntity(
            2,
            WhatsAppTemplateCategory.BEFORE_APPOINTMENT,
            "تذكير قبل الموعد",
            "مرحبا {اسم المريض} 🌟\nنذكرك بموعدك في {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nفي حالة عدم الحضور يرجى إبلاغنا مسبقاً.\nشكراً لك 🌟"
        ),
        WhatsAppTemplateEntity(
            3,
            WhatsAppTemplateCategory.AFTER_TREATMENT,
            "متابعة بعد العلاج",
            "أهلاً {اسم المريض} 🌸\nنتمنى لك دوام الصحة والعافية بعد زيارتك لعيادة {اسم العيادة} اليوم.\nيرجى اتباع إرشادات الطبيب والتواصل معنا في حال حدوث أي استفسار.\nمع تمنياتنا لك بابتسامة مشرقة! ✨"
        ),
        WhatsAppTemplateEntity(
            4,
            WhatsAppTemplateCategory.CONFIRMATION,
            "تأكيد الحجز",
            "مرحباً {اسم المريض} ✅\nتم تأكيد حجز موعدك بنجاح في عيادة {اسم العيادة}.\nالتاريخ: {تاريخ الموعد}\nالوقت: {وقت الموعد}\nنتشرف بزيارتك ويسعدنا تقديم أفضل رعاية لك."
        ),
        WhatsAppTemplateEntity(
            5,
            WhatsAppTemplateCategory.CANCELLATION,
            "إلغاء الموعد",
            "عزيزنا {اسم المريض} ⚠️\nنود إفادتك بأنه تم إلغاء موعدك المحدد بتاريخ {تاريخ الموعد} الساعة {وقت الموعد} في عيادة {اسم العيادة}.\nلتحديد موعد بديل يناسبك، نسعد بتواصلك معنا في أي وقت."
        )
    )
    db.whatsappTemplateDao().insertTemplates(templates)
}
