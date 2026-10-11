import React, { useState, useEffect } from 'react';
import { X, Calendar as CalendarIcon, Clock, User, Phone, Stethoscope, DollarSign, Tag, FileText } from 'lucide-react';
import type { Appointment, Patient, ProcedureConfig, User as UserModel, PatientClassification } from '../types';
import { saveAppointment, savePatient, generateUniqueId } from '../services/clinicService';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  appointmentToEdit?: Appointment | null;
  defaultDate?: string;
  defaultStartTime?: string;
  defaultEndTime?: string;
  patients: Patient[];
  procedures: ProcedureConfig[];
  currentUser: UserModel;
}

export const AppointmentModal: React.FC<Props> = ({
  isOpen,
  onClose,
  appointmentToEdit,
  defaultDate,
  defaultStartTime = '16:00',
  defaultEndTime = '16:30',
  patients,
  procedures,
  currentUser
}) => {
  const [patientName, setPatientName] = useState('');
  const [patientPhone, setPatientPhone] = useState('');
  const [patientClassification, setPatientClassification] = useState<PatientClassification>('DISCIPLINED');
  const [procedureName, setProcedureName] = useState('');
  const [dateStr, setDateStr] = useState(defaultDate || new Date().toISOString().split('T')[0]);
  const [startTime, setStartTime] = useState(defaultStartTime);
  const [endTime, setEndTime] = useState(defaultEndTime);
  const [price, setPrice] = useState(0);
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [nameSuggestions, setNameSuggestions] = useState<Patient[]>([]);

  useEffect(() => {
    if (appointmentToEdit) {
      setPatientName(appointmentToEdit.patientName);
      setPatientPhone(appointmentToEdit.patientPhone);
      setPatientClassification(appointmentToEdit.patientClassification);
      setProcedureName(appointmentToEdit.procedureName);
      setDateStr(appointmentToEdit.dateStr);
      setStartTime(appointmentToEdit.startTime);
      setEndTime(appointmentToEdit.endTime);
      setPrice(appointmentToEdit.price);
      setNotes(appointmentToEdit.notes);
    } else {
      setPatientName('');
      setPatientPhone('');
      setPatientClassification('DISCIPLINED');
      setProcedureName(procedures.length > 0 ? procedures[0].name : 'كشف');
      setDateStr(defaultDate || new Date().toISOString().split('T')[0]);
      setStartTime(defaultStartTime || '16:00');
      setEndTime(defaultEndTime || '16:30');
      setPrice(procedures.length > 0 ? procedures[0].defaultPrice : 0);
      setNotes('');
    }
  }, [appointmentToEdit, isOpen, defaultDate, defaultStartTime, defaultEndTime, procedures]);

  if (!isOpen) return null;

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setPatientName(val);
    if (val.trim().length > 1) {
      const filtered = patients.filter(p => p.name.toLowerCase().includes(val.toLowerCase()));
      setNameSuggestions(filtered.slice(0, 5));
    } else {
      setNameSuggestions([]);
    }
  };

  const selectPatientSuggestion = (p: Patient) => {
    setPatientName(p.name);
    setPatientPhone(p.phone);
    setPatientClassification(p.classification);
    setNameSuggestions([]);
  };

  const handleProcedureChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const procName = e.target.value;
    setProcedureName(procName);
    const proc = procedures.find(p => p.name === procName);
    if (proc) {
      setPrice(proc.defaultPrice);
      try {
        const [h, m] = startTime.split(':').map(Number);
        const totalMin = h * 60 + m + proc.defaultDurationMinutes;
        const newH = Math.floor(totalMin / 60) % 24;
        const newM = totalMin % 60;
        setEndTime(`${newH.toString().padStart(2, '0')}:${newM.toString().padStart(2, '0')}`);
      } catch (err) {
        // ignore
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!patientName.trim()) {
      alert('يرجى كتابة اسم المريض');
      return;
    }
    setIsSubmitting(true);
    try {
      let existingPatient = patients.find(
        p => p.name.trim().toLowerCase() === patientName.trim().toLowerCase() ||
             (patientPhone.trim() && p.phone.trim() === patientPhone.trim())
      );

      let patientId: number;
      if (existingPatient) {
        patientId = existingPatient.id;
        if (existingPatient.classification !== patientClassification) {
          await savePatient({ ...existingPatient, classification: patientClassification });
        }
      } else {
        patientId = generateUniqueId();
        const newPatient: Patient = {
          id: patientId,
          name: patientName.trim(),
          phone: patientPhone.trim(),
          classification: patientClassification,
          notes: `تم تسجيله من الويب عند حجز: ${procedureName}`,
          createdAt: Date.now()
        };
        await savePatient(newPatient);
      }

      const isNew = !appointmentToEdit;
      const finalAppointment: Appointment = {
        id: appointmentToEdit ? appointmentToEdit.id : generateUniqueId(),
        patientId,
        patientName: patientName.trim(),
        patientPhone: patientPhone.trim(),
        procedureName: procedureName || 'كشف',
        chairNumber: 1, // Single chair for the clinic
        dateStr,
        startTime,
        endTime,
        price,
        status: appointmentToEdit ? appointmentToEdit.status : 'CONFIRMED',
        patientClassification,
        notes: notes.trim(),
        bookedByUserId: currentUser.id,
        bookedByUserName: currentUser.name,
        createdAt: appointmentToEdit ? appointmentToEdit.createdAt : Date.now()
      };

      await saveAppointment(finalAppointment, currentUser, isNew ? 'BOOKING' : 'EDIT');
      onClose();
    } catch (err: any) {
      console.error(err);
      alert('حدث خطأ أثناء حفظ الموعد: ' + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <CalendarIcon size={24} color="#00b4d8" />
            <span>{appointmentToEdit ? 'تعديل بيانات الموعد' : 'حجز موعد جديد في العيادة'}</span>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Patient Name with Auto-Suggest */}
          <div className="form-group full-width" style={{ position: 'relative' }}>
            <label className="form-label">
              <User size={15} style={{ display: 'inline', marginLeft: 4 }} /> اسم المريض *
            </label>
            <input
              type="text"
              className="custom-input"
              value={patientName}
              onChange={handleNameChange}
              placeholder="اكتب اسم المريض الثلاثي..."
              required
            />
            {nameSuggestions.length > 0 && (
              <div style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                background: '#1c2541',
                border: '1px solid rgba(255,255,255,0.15)',
                borderRadius: '8px',
                zIndex: 50,
                marginTop: 4,
                boxShadow: '0 8px 20px rgba(0,0,0,0.5)'
              }}>
                {nameSuggestions.map(p => (
                  <div
                    key={p.id}
                    onClick={() => selectPatientSuggestion(p)}
                    style={{
                      padding: '10px 14px',
                      cursor: 'pointer',
                      borderBottom: '1px solid rgba(255,255,255,0.05)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '0.9rem'
                    }}
                    onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.08)'}
                    onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                  >
                    <span>{p.name}</span>
                    <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>{p.phone || 'بدون هاتف'}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="form-grid">
            {/* Phone */}
            <div className="form-group">
              <label className="form-label">
                <Phone size={15} style={{ display: 'inline', marginLeft: 4 }} /> رقم الهاتف
              </label>
              <input
                type="tel"
                className="custom-input"
                value={patientPhone}
                onChange={e => setPatientPhone(e.target.value)}
                placeholder="01xxxxxxxxx"
              />
            </div>

            {/* Classification */}
            <div className="form-group">
              <label className="form-label">
                <Tag size={15} style={{ display: 'inline', marginLeft: 4 }} /> تصنيف المريض
              </label>
              <select
                className="custom-select"
                value={patientClassification}
                onChange={e => setPatientClassification(e.target.value as PatientClassification)}
              >
                <option value="DISCIPLINED">ملتزم</option>
                <option value="VIP">VIP</option>
                <option value="NORMAL">عادي</option>
                <option value="FREQUENT_CANCELLER">كثير الإلغاء</option>
                <option value="DELAYED">متأخر</option>
              </select>
            </div>

            {/* Procedure */}
            <div className="form-group">
              <label className="form-label">
                <Stethoscope size={15} style={{ display: 'inline', marginLeft: 4 }} /> الإجراء الطبي
              </label>
              <select
                className="custom-select"
                value={procedureName}
                onChange={handleProcedureChange}
              >
                {procedures.length > 0 ? (
                  procedures.map(p => (
                    <option key={p.id} value={p.name}>{p.name}</option>
                  ))
                ) : (
                  <>
                    <option value="كشف واستشارة">كشف واستشارة</option>
                    <option value="حشو تجميلي">حشو تجميلي</option>
                    <option value="علاج عصب وجذور">علاج عصب وجذور</option>
                    <option value="تنظيف وتلميع جير">تنظيف وتلميع جير</option>
                    <option value="خلع أسنان">خلع أسنان</option>
                    <option value="خلع ضرس عقل جراحي">خلع ضرس عقل جراحي</option>
                    <option value="تركيبات وزركون">تركيبات وزركون</option>
                    <option value="زراعة أسنان">زراعة أسنان</option>
                    <option value="تقويم أسنان">تقويم أسنان</option>
                    <option value="تبييض أسنان">تبييض أسنان</option>
                  </>
                )}
              </select>
            </div>

            {/* Date */}
            <div className="form-group">
              <label className="form-label">
                <CalendarIcon size={15} style={{ display: 'inline', marginLeft: 4 }} /> تاريخ الموعد
              </label>
              <input
                type="date"
                className="custom-input"
                value={dateStr}
                onChange={e => setDateStr(e.target.value)}
                required
              />
            </div>

            {/* Start Time */}
            <div className="form-group">
              <label className="form-label">
                <Clock size={15} style={{ display: 'inline', marginLeft: 4 }} /> وقت البدء
              </label>
              <input
                type="time"
                className="custom-input"
                value={startTime}
                onChange={e => setStartTime(e.target.value)}
                required
              />
            </div>

            {/* End Time */}
            <div className="form-group">
              <label className="form-label">
                <Clock size={15} style={{ display: 'inline', marginLeft: 4 }} /> وقت الانتهاء
              </label>
              <input
                type="time"
                className="custom-input"
                value={endTime}
                onChange={e => setEndTime(e.target.value)}
                required
              />
            </div>

            {/* Price */}
            <div className="form-group full-width">
              <label className="form-label">
                <DollarSign size={15} style={{ display: 'inline', marginLeft: 4 }} /> التكلفة المتوقعة (ج.م)
              </label>
              <input
                type="number"
                className="custom-input"
                value={price}
                onChange={e => setPrice(Number(e.target.value))}
                min="0"
              />
            </div>
          </div>

          {/* Notes */}
          <div className="form-group full-width">
            <label className="form-label">
              <FileText size={15} style={{ display: 'inline', marginLeft: 4 }} /> ملاحظات إضافية
            </label>
            <textarea
              className="custom-input"
              rows={3}
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="أي ملاحظات خاصة بالمريض أو الخطة العلاجية..."
            />
          </div>

          <div className="modal-footer">
            <button type="button" className="btn-secondary" onClick={onClose} disabled={isSubmitting}>
              إلغاء
            </button>
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'جاري الحفظ...' : (appointmentToEdit ? 'تعديل الموعد' : 'تأكيد الحجز')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
