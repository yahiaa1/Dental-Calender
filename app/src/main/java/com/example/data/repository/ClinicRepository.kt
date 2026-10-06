package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ClinicRepository(
    private val patientDao: PatientDao,
    private val appointmentDao: AppointmentDao,
    private val procedureConfigDao: ProcedureConfigDao,
    private val whatsappTemplateDao: WhatsAppTemplateDao,
    private val userDao: UserDao
) {
    // Users & Staff
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    suspend fun getUserById(id: Long) = userDao.getUserById(id)
    suspend fun getUserByUsername(username: String) = userDao.getUserByUsername(username)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)
    suspend fun deleteUserById(id: Long) = userDao.deleteUserById(id)

    // Patients
    val allPatients: Flow<List<PatientEntity>> = patientDao.getAllPatients()
    suspend fun getPatientById(id: Long) = patientDao.getPatientById(id)
    fun searchPatients(query: String) = patientDao.searchPatients(query)
    suspend fun insertPatient(patient: PatientEntity) = patientDao.insertPatient(patient)
    suspend fun updatePatient(patient: PatientEntity) = patientDao.updatePatient(patient)
    suspend fun deletePatient(patient: PatientEntity) = patientDao.deletePatient(patient)
    suspend fun clearAllPatients() = patientDao.clearAllPatients()

    // Appointments
    val allAppointments: Flow<List<AppointmentEntity>> = appointmentDao.getAllAppointments()
    fun getAppointmentsForDate(dateStr: String) = appointmentDao.getAppointmentsForDate(dateStr)
    fun getAppointmentsForDateRange(startDateStr: String, endDateStr: String) =
        appointmentDao.getAppointmentsForDateRange(startDateStr, endDateStr)
    suspend fun getAppointmentById(id: Long) = appointmentDao.getAppointmentById(id)
    fun getAppointmentsForPatient(patientId: Long) = appointmentDao.getAppointmentsForPatient(patientId)
    suspend fun insertAppointment(appointment: AppointmentEntity) = appointmentDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: AppointmentEntity) = appointmentDao.updateAppointment(appointment)
    suspend fun updateAppointmentStatus(id: Long, status: AppointmentStatus) =
        appointmentDao.updateAppointmentStatus(id, status)
    suspend fun deleteAppointment(appointment: AppointmentEntity) = appointmentDao.deleteAppointment(appointment)
    suspend fun deleteAppointmentById(id: Long) = appointmentDao.deleteAppointmentById(id)
    suspend fun clearAllAppointments() = appointmentDao.clearAllAppointments()

    // Procedures
    val allProcedures: Flow<List<ProcedureConfigEntity>> = procedureConfigDao.getAllProcedures()
    suspend fun updateProcedureColor(name: String, colorHex: String) =
        procedureConfigDao.updateProcedureColor(name, colorHex)
    suspend fun insertProcedure(procedure: ProcedureConfigEntity) =
        procedureConfigDao.insertProcedure(procedure)
    suspend fun updateProcedure(procedure: ProcedureConfigEntity) =
        procedureConfigDao.updateProcedure(procedure)
    suspend fun deleteProcedureById(id: Long) = procedureConfigDao.deleteProcedureById(id)

    // WhatsApp Templates
    val allTemplates: Flow<List<WhatsAppTemplateEntity>> = whatsappTemplateDao.getAllTemplates()
    suspend fun getTemplateByCategory(category: WhatsAppTemplateCategory) =
        whatsappTemplateDao.getTemplateByCategory(category)
    suspend fun updateTemplate(template: WhatsAppTemplateEntity) =
        whatsappTemplateDao.updateTemplate(template)
}
