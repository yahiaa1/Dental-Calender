import {
  collection,
  doc,
  setDoc,
  deleteDoc,
  onSnapshot,
  getDocs,
  query,
  where
} from 'firebase/firestore';
import { db } from '../firebase';
import type {
  Appointment,
  Patient,
  User,
  ProcedureConfig,
  WhatsAppTemplate,
  ActionLog,
  ClinicSettings
} from '../types';

export const generateUniqueId = (): number => {
  return Date.now() * 1000 + Math.floor(Math.random() * 1000);
};

export const normalizeArabicNumerals = (str?: any): string => {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/[٠۰]/g, '0')
    .replace(/[١۱]/g, '1')
    .replace(/[٢۲]/g, '2')
    .replace(/[٣۳]/g, '3')
    .replace(/[٤۴]/g, '4')
    .replace(/[٥۵]/g, '5')
    .replace(/[٦۶]/g, '6')
    .replace(/[٧۷]/g, '7')
    .replace(/[٨۸]/g, '8')
    .replace(/[٩۹]/g, '9');
};

export const normalizeTime = (timeStr?: any): string => {
  if (!timeStr) return '00:00';
  const clean = normalizeArabicNumerals(timeStr).trim();
  const parts = clean.split(':').map(Number);
  if (parts.length >= 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
    return `${String(parts[0]).padStart(2, '0')}:${String(parts[1]).padStart(2, '0')}`;
  }
  return clean || '00:00';
};

export const normalizeDate = (dateStr?: any): string => {
  if (!dateStr) return '';
  return normalizeArabicNumerals(dateStr).trim();
};

// ==================== APPOINTMENTS ====================
export const subscribeAppointments = (callback: (appointments: Appointment[]) => void) => {
  const colRef = collection(db, 'clinic_appointments');
  return onSnapshot(colRef, (snapshot) => {
    const list: Appointment[] = [];
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      const id = typeof data.id === 'number' ? data.id : Number(docSnap.id) || 0;
      list.push({
        id,
        patientId: data.patientId || 0,
        patientName: data.patientName || '',
        patientPhone: data.patientPhone || '',
        procedureName: data.procedureName || '',
        chairNumber: data.chairNumber || 1,
        dateStr: normalizeDate(data.dateStr),
        startTime: normalizeTime(data.startTime),
        endTime: normalizeTime(data.endTime),
        price: Number(data.price) || 0,
        status: data.status || 'UNCONFIRMED',
        patientClassification: data.patientClassification || 'DISCIPLINED',
        notes: data.notes || '',
        bookedByUserId: data.bookedByUserId || 1,
        bookedByUserName: data.bookedByUserName || 'د. محمد (أدمن)',
        createdAt: data.createdAt || Date.now(),
        updatedAt: data.updatedAt
      });
    });
    callback(list);
  }, (err) => {
    console.error('Error listening to appointments:', err);
  });
};

export const saveAppointment = async (
  appointment: Appointment,
  currentUser: User,
  actionType: 'BOOKING' | 'EDIT' = 'BOOKING'
) => {
  const idStr = appointment.id.toString();
  const docRef = doc(db, 'clinic_appointments', idStr);
  const now = Date.now();
  const data = {
    ...appointment,
    dateStr: normalizeDate(appointment.dateStr),
    startTime: normalizeTime(appointment.startTime),
    endTime: normalizeTime(appointment.endTime),
    price: Number(appointment.price) || 0,
    updatedAt: now
  };
  await setDoc(docRef, data, { merge: true });

  // Log action
  await logAction({
    id: generateUniqueId(),
    appointmentId: appointment.id,
    actionType,
    userName: currentUser.name,
    userRole: currentUser.role,
    timestamp: now,
    details: actionType === 'BOOKING'
      ? `قام ${currentUser.name} بحجز موعد جديد للمريض ${appointment.patientName} (${appointment.procedureName}) يوم ${data.dateStr} الساعة ${data.startTime}`
      : `قام ${currentUser.name} بتعديل موعد المريض ${appointment.patientName} ليوم ${data.dateStr} الساعة ${data.startTime}`,
    extraInfo: actionType === 'BOOKING' ? 'حجز جديد من الويب' : 'تعديل موعد من الويب'
  });
};

export const updateAppointmentStatus = async (
  appointment: Appointment,
  newStatus: Appointment['status'],
  currentUser: User
) => {
  const idStr = appointment.id.toString();
  const docRef = doc(db, 'clinic_appointments', idStr);
  const now = Date.now();
  await setDoc(docRef, { status: newStatus, updatedAt: now }, { merge: true });

  const statusLabels: Record<string, string> = {
    CONFIRMED: 'مؤكد',
    UNCONFIRMED: 'لم يتم التاكيد',
    IN_TREATMENT: 'جار العلاج',
    COMPLETED: 'مكتمل',
    ATTENDED: 'تم الحضور',
    CANCELLED: 'ملغي',
    NO_SHOW: 'لم يحضر'
  };

  await logAction({
    id: generateUniqueId(),
    appointmentId: appointment.id,
    actionType: 'STATUS_CHANGE',
    userName: currentUser.name,
    userRole: currentUser.role,
    timestamp: now,
    details: `قام ${currentUser.name} بتغيير حالة موعد ${appointment.patientName} إلى '${statusLabels[newStatus] || newStatus}'`,
    extraInfo: `الحالة: ${statusLabels[newStatus] || newStatus}`
  });
};

export const moveAppointment = async (
  appointment: Appointment,
  newDate: string,
  newStartTime: string,
  newEndTime: string,
  currentUser: User
) => {
  const idStr = appointment.id.toString();
  const docRef = doc(db, 'clinic_appointments', idStr);
  const now = Date.now();
  const cleanDate = normalizeDate(newDate);
  const cleanStart = normalizeTime(newStartTime);
  const cleanEnd = normalizeTime(newEndTime);

  const updatedApp: Appointment = {
    ...appointment,
    dateStr: cleanDate,
    startTime: cleanStart,
    endTime: cleanEnd,
    updatedAt: now
  };
  await setDoc(docRef, updatedApp, { merge: true });

  await logAction({
    id: generateUniqueId(),
    appointmentId: appointment.id,
    actionType: 'MOVE',
    userName: currentUser.name,
    userRole: currentUser.role,
    timestamp: now,
    details: `قام ${currentUser.name} بنقل موعد المريض ${appointment.patientName} من تاريخ ${appointment.dateStr} (${appointment.startTime}) إلى تاريخ ${newDate} (${newStartTime})`,
    extraInfo: `من ${appointment.dateStr} إلى ${newDate}`
  });
};

export const deleteAppointment = async (
  appointment: Appointment,
  currentUser: User
) => {
  const idStr = appointment.id.toString();
  const docRef = doc(db, 'clinic_appointments', idStr);
  await deleteDoc(docRef);

  await logAction({
    id: generateUniqueId(),
    appointmentId: appointment.id,
    actionType: 'DELETE',
    userName: currentUser.name,
    userRole: currentUser.role,
    timestamp: Date.now(),
    details: `قام ${currentUser.name} بحذف موعد المريض ${appointment.patientName} (${appointment.dateStr} ${appointment.startTime})`,
    extraInfo: 'حذف موعد من الويب'
  });
};

// ==================== PATIENTS ====================
export const subscribePatients = (callback: (patients: Patient[]) => void) => {
  const colRef = collection(db, 'clinic_patients');
  return onSnapshot(colRef, (snapshot) => {
    const list: Patient[] = [];
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      const id = typeof data.id === 'number' ? data.id : Number(docSnap.id) || 0;
      list.push({
        id,
        name: data.name || '',
        phone: data.phone || '',
        classification: data.classification || 'DISCIPLINED',
        notes: data.notes || '',
        createdAt: data.createdAt || Date.now(),
        updatedAt: data.updatedAt
      });
    });
    callback(list);
  }, (err) => {
    console.error('Error listening to patients:', err);
  });
};

export const savePatient = async (patient: Patient, currentUser?: User) => {
  const idStr = patient.id.toString();
  const docRef = doc(db, 'clinic_patients', idStr);
  const now = Date.now();
  await setDoc(docRef, { ...patient, updatedAt: now }, { merge: true });

  // Sync patient updates to their existing appointments in Firestore
  try {
    const seenIds = new Set<string>();
    const updates: Promise<void>[] = [];

    const q1 = query(collection(db, 'clinic_appointments'), where('patientId', '==', patient.id));
    const snap1 = await getDocs(q1);
    snap1.docs.forEach(docSnap => {
      seenIds.add(docSnap.id);
      updates.push(
        setDoc(docSnap.ref, {
          patientName: patient.name,
          patientPhone: patient.phone,
          patientClassification: patient.classification,
          updatedAt: now
        }, { merge: true })
      );
    });

    if (patient.phone) {
      const q2 = query(collection(db, 'clinic_appointments'), where('patientPhone', '==', patient.phone));
      const snap2 = await getDocs(q2);
      snap2.docs.forEach(docSnap => {
        if (!seenIds.has(docSnap.id)) {
          seenIds.add(docSnap.id);
          updates.push(
            setDoc(docSnap.ref, {
              patientName: patient.name,
              patientPhone: patient.phone,
              patientClassification: patient.classification,
              updatedAt: now
            }, { merge: true })
          );
        }
      });
    }

    await Promise.all(updates);
  } catch (e) {
    console.warn('Syncing appointments for patient updated:', e);
  }

  if (currentUser) {
    await logAction({
      id: generateUniqueId(),
      appointmentId: 0,
      actionType: 'EDIT',
      userName: currentUser.name,
      userRole: currentUser.role,
      timestamp: now,
      details: `قام ${currentUser.name} بتحديث بيانات ملف المريض ${patient.name}`,
      extraInfo: 'تعديل ملف مريض من الويب'
    });
  }
};

export const deletePatient = async (patient: Patient, currentUser: User) => {
  const idStr = patient.id.toString();
  const docRef = doc(db, 'clinic_patients', idStr);
  await deleteDoc(docRef);

  await logAction({
    id: generateUniqueId(),
    appointmentId: 0,
    actionType: 'DELETE',
    userName: currentUser.name,
    userRole: currentUser.role,
    timestamp: Date.now(),
    details: `قام ${currentUser.name} بحذف ملف المريض ${patient.name}`,
    extraInfo: 'حذف مريض من الويب'
  });
};

// ==================== PROCEDURES ====================
export const subscribeProcedures = (callback: (procedures: ProcedureConfig[]) => void) => {
  const colRef = collection(db, 'clinic_procedures');
  return onSnapshot(colRef, (snapshot) => {
    const list: ProcedureConfig[] = [];
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      const id = typeof data.id === 'number' ? data.id : Number(docSnap.id) || 0;
      list.push({
        id,
        name: data.name || '',
        defaultDurationMinutes: data.defaultDurationMinutes || 30,
        defaultPrice: Number(data.defaultPrice) || 0,
        colorHex: data.colorHex || '#0ea5e9'
      });
    });
    callback(list);
  }, (err) => {
    console.error('Error listening to procedures:', err);
  });
};

// ==================== WHATSAPP TEMPLATES ====================
export const subscribeTemplates = (callback: (templates: WhatsAppTemplate[]) => void) => {
  const colRef = collection(db, 'clinic_whatsapp_templates');
  return onSnapshot(colRef, (snapshot) => {
    const list: WhatsAppTemplate[] = [];
    snapshot.forEach((docSnap) => {
      const data = docSnap.data();
      const id = typeof data.id === 'number' ? data.id : Number(docSnap.id) || 0;
      list.push({
        id,
        title: data.title || '',
        category: (data.category || docSnap.id) as any,
        messageText: data.messageText || '',
        updatedAt: data.updatedAt
      });
    });
    callback(list);
  }, (err) => {
    console.error('Error listening to templates:', err);
  });
};

export const saveTemplate = async (template: WhatsAppTemplate) => {
  // Key by category name so Android and Web stay 100% in sync
  const docRefCategory = doc(db, 'clinic_whatsapp_templates', template.category);
  const now = Date.now();
  const payload = {
    id: template.id,
    title: template.title,
    category: template.category,
    messageText: template.messageText,
    updatedAt: now
  };
  await setDoc(docRefCategory, payload, { merge: true });
  if (template.id > 0) {
    const docRefId = doc(db, 'clinic_whatsapp_templates', template.id.toString());
    await setDoc(docRefId, payload, { merge: true });
  }
};

// ==================== SETTINGS ====================
export const subscribeSettings = (callback: (settings: ClinicSettings) => void) => {
  const docRef = doc(db, 'clinic_settings', 'general_settings');
  return onSnapshot(docRef, (docSnap) => {
    if (docSnap.exists()) {
      const data = docSnap.data();
      callback({
        workStartHour: data.workStartHour ?? 14,
        workEndHour: data.workEndHour ?? 24,
        dayOff: data.dayOff ?? 'FRIDAY'
      });
    } else {
      callback({
        workStartHour: 14,
        workEndHour: 24,
        dayOff: 'FRIDAY'
      });
    }
  }, (err) => {
    console.error('Error listening to settings:', err);
  });
};

export const saveSettings = async (settings: ClinicSettings) => {
  const docRef = doc(db, 'clinic_settings', 'general_settings');
  await setDoc(docRef, settings, { merge: true });
};

// ==================== USERS & AUTH ====================
export const fetchUsers = async (): Promise<User[]> => {
  const colRef = collection(db, 'clinic_users');
  const snap = await getDocs(colRef);
  const list: User[] = [];
  snap.forEach((d) => {
    const data = d.data();
    list.push({
      id: typeof data.id === 'number' ? data.id : Number(d.id) || 1,
      name: data.name || '',
      phone: data.phone || '',
      role: data.role || 'DOCTOR',
      username: data.username || '',
      password: data.password || ''
    });
  });
  return list;
};

// ==================== ACTION LOGS ====================
export const logAction = async (log: ActionLog) => {
  try {
    const docRef = doc(db, 'clinic_action_logs', log.id.toString());
    await setDoc(docRef, log, { merge: true });
  } catch (e) {
    console.warn('Could not save action log:', e);
  }
};
