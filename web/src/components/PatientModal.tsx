import React, { useState, useEffect } from 'react';
import { X, User, Phone, Tag, FileText, Check, AlertCircle } from 'lucide-react';
import type { Patient, User as UserModel, PatientClassification } from '../types';
import { savePatient, generateUniqueId } from '../services/clinicService';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  patientToEdit?: Patient | null;
  currentUser: UserModel;
}

export const PatientModal: React.FC<Props> = ({
  isOpen,
  onClose,
  patientToEdit,
  currentUser
}) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [classification, setClassification] = useState<PatientClassification>('DISCIPLINED');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    if (patientToEdit) {
      setName(patientToEdit.name || '');
      setPhone(patientToEdit.phone || '');
      setClassification(patientToEdit.classification || 'DISCIPLINED');
      setNotes(patientToEdit.notes || '');
    } else {
      setName('');
      setPhone('');
      setClassification('DISCIPLINED');
      setNotes('');
    }
    setErrorMsg('');
  }, [patientToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorMsg('يرجى إدخال اسم المريض');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg('');

    try {
      const patientData: Patient = {
        id: patientToEdit?.id || generateUniqueId(),
        name: name.trim(),
        phone: phone.trim(),
        classification,
        notes: notes.trim(),
        createdAt: patientToEdit?.createdAt || Date.now()
      };

      await savePatient(patientData, currentUser);
      onClose();
    } catch (err: any) {
      setErrorMsg('حدث خطأ أثناء حفظ بيانات المريض: ' + err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const classificationsList: { key: PatientClassification; label: string; color: string; desc: string }[] = [
    { key: 'DISCIPLINED', label: 'منضبط ✅', color: '#10b981', desc: 'يحضر في مواعيده بانتظام' },
    { key: 'DELAYED', label: 'متأخر غالباً ⚠️', color: '#f59e0b', desc: 'يتأخر دائماً عن مواعيده' },
    { key: 'FREQUENT_CANCELLER', label: 'كثير الغياب ❌', color: '#ef4444', desc: 'يتغيب عن المواعيد ويلغي' },
    { key: 'VIP', label: '⭐ VIP', color: '#eab308', desc: 'مريض مميز وخدمة خاصة' },
    { key: 'NORMAL', label: 'عادي', color: '#64748b', desc: 'مريض عادي بدون تصنيف خاص' }
  ];

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: '520px' }} onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title">
            <User size={22} color="var(--primary)" />
            <span>{patientToEdit ? 'تعديل بيانات المريض' : 'إضافة ملف مريض جديد'}</span>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        {errorMsg && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.15)',
            border: '1px solid rgba(239, 68, 68, 0.4)',
            color: '#ef4444',
            padding: '10px 14px',
            borderRadius: '8px',
            marginBottom: '16px',
            fontSize: '0.88rem',
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <AlertCircle size={16} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Patient Name */}
          <div className="form-group">
            <label className="form-label">
              <User size={15} color="var(--primary)" /> اسم المريض بالكامل *
            </label>
            <input
              type="text"
              className="custom-input"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="مثال: أحمد محمد علي"
              required
              autoFocus
            />
          </div>

          {/* Patient Phone */}
          <div className="form-group">
            <label className="form-label">
              <Phone size={15} color="var(--primary)" /> رقم الهاتف (واتساب)
            </label>
            <input
              type="tel"
              className="custom-input"
              value={phone}
              onChange={e => setPhone(e.target.value)}
              placeholder="01XXXXXXXXX"
              style={{ direction: 'ltr', textAlign: 'right' }}
            />
          </div>

          {/* Patient Classification */}
          <div className="form-group">
            <label className="form-label">
              <Tag size={15} color="var(--primary)" /> تصنيف وسلوك المريض
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '8px' }}>
              {classificationsList.map(item => {
                const isSelected = classification === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    onClick={() => setClassification(item.key)}
                    style={{
                      background: isSelected ? 'rgba(0, 180, 216, 0.15)' : 'var(--bg-secondary)',
                      border: `1.5px solid ${isSelected ? item.color : 'var(--border-color)'}`,
                      borderRadius: '8px',
                      padding: '8px',
                      textAlign: 'center',
                      cursor: 'pointer',
                      fontFamily: 'inherit',
                      color: isSelected ? item.color : 'var(--text-primary)',
                      fontWeight: isSelected ? 800 : 600,
                      fontSize: '0.85rem',
                      transition: 'all 0.15s'
                    }}
                  >
                    <div>{item.label}</div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Medical Notes */}
          <div className="form-group">
            <label className="form-label">
              <FileText size={15} color="var(--primary)" /> ملاحظات طبية أو خاصة
            </label>
            <textarea
              className="custom-input"
              rows={3}
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="حساسية تجاه أدوية معينة، أمراض مزمنة، تنبيهات..."
              style={{ resize: 'vertical' }}
            />
          </div>

          {/* Footer Actions */}
          <div style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px',
            marginTop: '8px',
            paddingTop: '16px',
            borderTop: '1px solid var(--border-color)'
          }}>
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              إلغاء
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting}
              style={{ minWidth: '120px' }}
            >
              <Check size={16} />
              <span>{isSubmitting ? 'جاري الحفظ...' : 'حفظ البيانات'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
