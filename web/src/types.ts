export type AppointmentStatus = 'CONFIRMED' | 'UNCONFIRMED' | 'ATTENDED' | 'COMPLETED' | 'IN_TREATMENT' | 'CANCELLED' | 'NO_SHOW';

export type PatientClassification = 'VIP' | 'DISCIPLINED' | 'NORMAL' | 'FREQUENT_CANCELLER' | 'DELAYED';

export type UserRole = 'ADMIN' | 'DOCTOR' | 'SECRETARY' | 'NURSE';

export type WhatsAppCategory = 'ONE_HOUR_BEFORE' | 'BEFORE_APPOINTMENT' | 'AFTER_TREATMENT' | 'CONFIRMATION' | 'CANCELLATION';

export interface Appointment {
  id: number;
  patientId: number;
  patientName: string;
  patientPhone: string;
  procedureName: string;
  chairNumber: number;
  dateStr: string; // YYYY-MM-DD
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  price: number;
  status: AppointmentStatus;
  patientClassification: PatientClassification;
  notes: string;
  bookedByUserId: number;
  bookedByUserName: string;
  createdAt: number;
  updatedAt?: number;
}

export interface Patient {
  id: number;
  name: string;
  phone: string;
  classification: PatientClassification;
  notes: string;
  createdAt: number;
  updatedAt?: number;
}

export interface User {
  id: number;
  name: string;
  phone: string;
  role: UserRole;
  username: string;
  password?: string;
  createdAt?: number;
}

export interface ProcedureConfig {
  id: number;
  name: string;
  defaultDurationMinutes: number;
  defaultPrice: number;
  colorHex: string;
}

export interface WhatsAppTemplate {
  id: number;
  title: string;
  category: WhatsAppCategory;
  messageText: string;
  updatedAt?: number;
}

export interface ActionLog {
  id: number;
  appointmentId: number;
  actionType: 'BOOKING' | 'EDIT' | 'DELETE' | 'STATUS_CHANGE' | 'MOVE';
  userName: string;
  userRole: string;
  timestamp: number;
  details: string;
  extraInfo: string;
}

export interface ClinicSettings {
  workStartHour: number;
  workEndHour: number;
  dayOff: string;
}
