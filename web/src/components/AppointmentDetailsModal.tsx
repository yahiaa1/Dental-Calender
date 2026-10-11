import React, { useState } from 'react';
import {
  X,
  Phone,
  Calendar,
  Clock,
  DollarSign,
  User,
  Stethoscope,
  Trash2,
  Edit,
  Send,
  MessageSquare,
  Copy,
  Info,
  CheckCircle,
  FileText,
  Lock,
  AlarmClock
} from 'lucide-react';
import type { Appointment, User as UserModel, WhatsAppTemplate, PatientClassification, Patient } from '../types';
import { updateAppointmentStatus, deleteAppointment, savePatient } from '../services/clinicService';
import { PatientModal } from './PatientModal';

interface Props {
  appointment: Appointment | null;
  isOpen: boolean;
  onClose: () => void;
  onEdit: (appointment: Appointment) => void;
  currentUser: UserModel;
  templates: WhatsAppTemplate[];
  patients?: Patient[];
}

export const AppointmentDetailsModal: React.FC<Props> = ({
  appointment,
  isOpen,
  onClose,
  onEdit,
  currentUser,
  templates,
  patients = []
}) => {
  const [activeTab, setActiveTab] = useState<'DETAILS' | 'WHATSAPP'>('DETAILS');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [showPostTreatmentModal, setShowPostTreatmentModal] = useState(false);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [isEditPatientModalOpen, setIsEditPatientModalOpen] = useState(false);

  if (!isOpen || !appointment) return null;

  const canCancelOrDelete = currentUser.role === 'ADMIN' || appointment.bookedByUserId === currentUser.id;

  const handleStatusChange = async (newStatus: Appointment['status']) => {
    if ((newStatus === 'CANCELLED') && !canCancelOrDelete) {
      setShowPermissionModal(true);
      return;
    }

    const prevStatus = appointment.status;
    try {
      await updateAppointmentStatus(appointment, newStatus, currentUser);
      appointment.status = newStatus;
      if ((newStatus === 'COMPLETED' || newStatus === 'ATTENDED') && prevStatus !== 'COMPLETED' && prevStatus !== 'ATTENDED') {
        setShowPostTreatmentModal(true);
      }
    } catch (e: any) {
      alert('خطأ أثناء تغيير الحالة: ' + e.message);
    }
  };

  const handleToggleLate = async () => {
    const newClass: PatientClassification =
      appointment.patientClassification === 'DELAYED' ? 'DISCIPLINED' : 'DELAYED';
    try {
      await savePatient({
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        classification: newClass,
        notes: appointment.notes,
        createdAt: Date.now()
      });
      appointment.patientClassification = newClass;
      alert(`تم تحديث تصنيف المريض إلى: ${newClass === 'DELAYED' ? 'متأخر غالباً ⚠️' : 'منضبط ✅'}`);
    } catch (e: any) {
      alert('خطأ أثناء تحديث التصنيف: ' + e.message);
    }
  };

  const handleDelete = async () => {
    if (!canCancelOrDelete) {
      setShowPermissionModal(true);
      return;
    }
    if (window.confirm(`هل ترغب في حذف موعد المريض ${appointment.patientName} نهائياً وخصم إيراده؟`)) {
      try {
        await deleteAppointment(appointment, currentUser);
        onClose();
      } catch (e: any) {
        alert('خطأ أثناء الحذف: ' + e.message);
      }
    }
  };

  const handleCancel = async () => {
    if (!canCancelOrDelete) {
      setShowPermissionModal(true);
      return;
    }
    if (window.confirm(`هل تريد تحويل موعد ${appointment.patientName} إلى 'ملغي' واستبعاده من التقويم والإيرادات؟`)) {
      await handleStatusChange('CANCELLED');
      onClose();
    }
  };

  const format12Hour = (time24: string) => {
    if (!time24) return '';
    try {
      const clean = time24
        .replace(/[٠۰]/g, '0')
        .replace(/[١۱]/g, '1')
        .replace(/[٢۲]/g, '2')
        .replace(/[٣۳]/g, '3')
        .replace(/[٤۴]/g, '4')
        .replace(/[٥۵]/g, '5')
        .replace(/[٦۶]/g, '6')
        .replace(/[٧۷]/g, '7')
        .replace(/[٨۸]/g, '8')
        .replace(/[٩۹]/g, '9')
        .trim();
      const parts = clean.split(':').map(Number);
      if (parts.length < 2 || isNaN(parts[0]) || isNaN(parts[1])) {
        return time24;
      }
      const [h, m] = parts;
      const isPm = h >= 12;
      const h12 = h % 12 === 0 ? 12 : h % 12;
      return `${h12}:${m.toString().padStart(2, '0')} ${isPm ? 'م' : 'ص'}`;
    } catch {
      return time24 || '';
    }
  };

  const calculateDuration = () => {
    try {
      const cleanStart = (appointment.startTime || '')
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
      const cleanEnd = (appointment.endTime || '')
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
      const [h1, m1] = cleanStart.split(':').map(Number);
      const [h2, m2] = cleanEnd.split(':').map(Number);
      if (isNaN(h1) || isNaN(m1) || isNaN(h2) || isNaN(m2)) return 30;
      const dur = (h2 * 60 + m2) - (h1 * 60 + m1);
      return dur > 0 ? dur : 30;
    } catch {
      return 30;
    }
  };

  const formatArabicFullDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('ar-EG', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return dateStr;
    }
  };

  const getTemplateFormattedMessage = (cat: string) => {
    const tpl = templates.find(t => t.category === cat);
    let text = tpl?.messageText;
    if (!text) {
      const defaults: Record<string, string> = {
        CONFIRMATION: 'مرحباً {اسم المريض}، تم تأكيد حجز موعدك في {اسم العيادة} يوم {تاريخ الموعد} الساعة {وقت الموعد}. نتشرف بحضورك.',
        BEFORE_APPOINTMENT: 'مرحباً {اسم المريض}، نذكرك بموعدك القادم في {اسم العيادة} يوم {تاريخ الموعد} الساعة {وقت الموعد}. برجاء الحضور قبل الموعد بـ 10 دقائق.',
        ONE_HOUR_BEFORE: 'مرحباً {اسم المريض} ⏰ نذكرك بموعدك في {اسم العيادة} بعد ساعة في تمام {وقت الموعد}.',
        AFTER_TREATMENT: 'مرحباً {اسم المريض}، نتمنى لك الشفاء العاجل بعد جلستك اليوم في {اسم العيادة}. نرجو الالتزام بتعليمات الطبيب وتناول الأدوية.',
        CANCELLATION: 'مرحباً {اسم المريض}، تم إلغاء موعدك المحدد يوم {تاريخ الموعد} الساعة {وقت الموعد}. لتحديد موعد جديد يسعدنا تواصلك معنا.'
      };
      text = defaults[cat] || '';
    }

    return text
      .replace(/{اسم المريض}/g, appointment.patientName)
      .replace(/{وقت الموعد}/g, format12Hour(appointment.startTime))
      .replace(/{تاريخ الموعد}/g, appointment.dateStr)
      .replace(/{اسم العيادة}/g, 'عيادة الأسنان');
  };

  const sendWhatsApp = (cat: string) => {
    let phone = appointment.patientPhone.trim().replace(/\D/g, '');
    if (!phone) {
      alert('لا يوجد رقم هاتف مسجل لهذا المريض');
      return;
    }
    if (phone.startsWith('01')) phone = '2' + phone;
    else if (phone.startsWith('1')) phone = '20' + phone;

    const message = getTemplateFormattedMessage(cat);
    const encoded = encodeURIComponent(message);
    window.open(`https://web.whatsapp.com/send?phone=${phone}&text=${encoded}`, '_blank');
  };

  const copyMessage = (cat: string) => {
    const message = getTemplateFormattedMessage(cat);
    navigator.clipboard.writeText(message);
    setCopiedKey(cat);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  // Status options identical to Android App (Enums.kt)
  const statusOptions: { key: Appointment['status']; label: string; color: string }[] = [
    { key: 'UNCONFIRMED', label: 'لم يتم التاكيد', color: '#FFA000' },
    { key: 'CONFIRMED', label: 'مؤكد', color: '#1E88E5' },
    { key: 'IN_TREATMENT', label: 'جار العلاج', color: '#8E24AA' },
    { key: 'COMPLETED', label: 'مكتمل', color: '#43A047' },
    { key: 'NO_SHOW', label: 'لم يحضر', color: '#757575' },
    { key: 'CANCELLED', label: 'ملغي', color: '#E53935' }
  ];

  const templateCards = [
    { key: 'CONFIRMATION', title: 'رسالة تأكيد الحجز', icon: CheckCircle, color: '#10b981' },
    { key: 'BEFORE_APPOINTMENT', title: 'تذكير بموعد العيادة', icon: Calendar, color: '#00b4d8' },
    { key: 'ONE_HOUR_BEFORE', title: 'تذكير قبل الموعد بساعة ⏰', icon: Clock, color: '#f59e0b' },
    { key: 'AFTER_TREATMENT', title: 'تعليمات ما بعد العلاج 💊', icon: Stethoscope, color: '#2dd4bf' },
    { key: 'CANCELLATION', title: 'إشعار إلغاء الموعد', icon: X, color: '#ef4444' }
  ];

  const isCompleted = appointment.status === 'COMPLETED' || appointment.status === 'ATTENDED';

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-content" style={{ maxWidth: '680px' }} onClick={e => e.stopPropagation()}>
          {/* Top Header */}
          <div className="modal-header">
            <div className="modal-title">
              <Calendar size={22} color="#00b4d8" />
              <span>تفاصيل الموعد</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                className="btn-secondary"
                style={{ padding: '6px 14px', fontSize: '0.85rem' }}
                onClick={() => { onClose(); onEdit(appointment); }}
              >
                <Edit size={14} /> تعديل
              </button>
              <button className="modal-close-btn" onClick={onClose}>
                <X size={20} />
              </button>
            </div>
          </div>

          {/* Patient Header Card (Like Android App) */}
          <div style={{
            background: 'var(--bg-secondary)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            padding: '16px',
            marginBottom: '14px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                <div style={{
                  width: '50px',
                  height: '50px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #0284c7, #0369a1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  boxShadow: '0 4px 12px rgba(2,132,199,0.3)'
                }}>
                  <User size={26} />
                </div>

                <div>
                  <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                    {appointment.patientName}
                  </div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', direction: 'ltr', textAlign: 'right' }}>
                    {appointment.patientPhone || 'بدون رقم هاتف'}
                  </div>
                </div>
              </div>

              {/* Quick Actions */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <button
                  className="btn-secondary"
                  style={{ padding: '8px 12px', fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '6px' }}
                  onClick={() => setIsEditPatientModalOpen(true)}
                  title="تعديل بيانات وملف المريض"
                >
                  <Edit size={14} color="var(--primary)" />
                  <span>تعديل المريض</span>
                </button>

                {appointment.patientPhone && (
                  <>
                    <a
                      href={`tel:${appointment.patientPhone}`}
                      className="btn-secondary"
                      style={{ padding: '8px 12px' }}
                      title="اتصال هاتفي"
                    >
                      <Phone size={15} color="#00b4d8" />
                    </a>
                    <button
                      className="whatsapp-action-btn"
                      style={{ padding: '8px 14px', fontSize: '0.85rem' }}
                      onClick={() => isCompleted ? sendWhatsApp('AFTER_TREATMENT') : sendWhatsApp('BEFORE_APPOINTMENT')}
                    >
                      <MessageSquare size={16} /> {isCompleted ? 'واتساب (بعد العلاج)' : 'واتساب'}
                    </button>
                  </>
                )}
              </div>
            </div>

            {/* Behavior / Classification Row */}
            <div style={{
              marginTop: '12px',
              paddingTop: '12px',
              borderTop: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.85rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>سلوك المريض:</span>
                <span className="app-classification-pill">
                  {appointment.patientClassification === 'VIP' ? '⭐ VIP' :
                   appointment.patientClassification === 'DISCIPLINED' ? 'منضبط' :
                   appointment.patientClassification === 'FREQUENT_CANCELLER' ? '⚠️ كثير الإلغاء' :
                   appointment.patientClassification === 'DELAYED' ? 'متأخر غالباً' : 'عادي'}
                </span>
              </div>

              <button
                onClick={handleToggleLate}
                style={{
                  background: 'transparent',
                  border: 'none',
                  color: appointment.patientClassification === 'DELAYED' ? '#10b981' : '#f59e0b',
                  cursor: 'pointer',
                  fontFamily: 'inherit',
                  fontSize: '0.8rem',
                  fontWeight: 700
                }}
              >
                {appointment.patientClassification === 'DELAYED' ? 'تحديد كـ منضبط ✅' : 'تحديد كـ متأخر غالباً ⚠️'}
              </button>
            </div>
          </div>

          {/* Quick Action 1: 1-Hour Reminder Button (Like Android App) */}
          <div style={{
            background: 'rgba(37, 211, 102, 0.12)',
            border: '1px solid rgba(37, 211, 102, 0.3)',
            borderRadius: '12px',
            padding: '12px 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: '10px',
            gap: '12px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <AlarmClock size={22} color="#25d366" />
              <div>
                <div style={{ fontWeight: 800, fontSize: '0.92rem', color: 'var(--text-primary)' }}>
                  تنبيه قبل الموعد بساعة
                </div>
                <div style={{ fontSize: '0.78rem', color: '#10b981' }}>
                  إرسال تذكير واتساب للمريض قبل ساعته المحددة
                </div>
              </div>
            </div>
            <button
              className="whatsapp-action-btn"
              style={{ padding: '6px 14px', fontSize: '0.85rem', whiteSpace: 'nowrap' }}
              onClick={() => sendWhatsApp('ONE_HOUR_BEFORE')}
            >
              إرسال الآن 💬
            </button>
          </div>

          {/* Quick Action 2: Post-Treatment Message Button (When Treatment Completed) */}
          {isCompleted && (
            <div style={{
              background: 'rgba(37, 211, 102, 0.15)',
              border: '1.5px solid #25d366',
              borderRadius: '12px',
              padding: '12px 16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '12px',
              gap: '12px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <CheckCircle size={22} color="#25d366" />
                <div>
                  <div style={{ fontWeight: 800, fontSize: '0.92rem', color: 'var(--text-primary)' }}>
                    رسالة بعد العلاج 💊
                  </div>
                  <div style={{ fontSize: '0.78rem', color: '#10b981' }}>
                    إرسال رسالة متابعة بعد العلاج للمريض عبر واتساب
                  </div>
                </div>
              </div>
              <button
                className="whatsapp-action-btn"
                style={{ padding: '6px 14px', fontSize: '0.85rem', whiteSpace: 'nowrap' }}
                onClick={() => sendWhatsApp('AFTER_TREATMENT')}
              >
                إرسال الآن 💬
              </button>
            </div>
          )}

          {/* Tab Switcher (Details vs WhatsApp Templates) */}
          <div style={{
            display: 'flex',
            background: 'var(--bg-input)',
            padding: '4px',
            borderRadius: '10px',
            marginBottom: '16px'
          }}>
            <button
              onClick={() => setActiveTab('DETAILS')}
              style={{
                flex: 1,
                padding: '8px',
                border: 'none',
                borderRadius: '8px',
                background: activeTab === 'DETAILS' ? 'var(--primary)' : 'transparent',
                color: 'white',
                fontFamily: 'inherit',
                fontWeight: 700,
                fontSize: '0.9rem',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            >
              تفاصيل الموعد
            </button>
            <button
              onClick={() => setActiveTab('WHATSAPP')}
              style={{
                flex: 1,
                padding: '8px',
                border: 'none',
                borderRadius: '8px',
                background: activeTab === 'WHATSAPP' ? '#25d366' : 'transparent',
                color: activeTab === 'WHATSAPP' ? '#0b132b' : 'var(--text-primary)',
                fontFamily: 'inherit',
                fontWeight: 700,
                fontSize: '0.9rem',
                cursor: 'pointer',
                transition: 'all 0.2s',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px'
              }}
            >
              <MessageSquare size={16} /> رسائل الواتساب
            </button>
          </div>

          {activeTab === 'DETAILS' ? (
            <div>
              {/* Info Grid */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '12px',
                marginBottom: '16px'
              }}>
                <div style={{ background: 'var(--bg-secondary)', padding: '12px', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <Calendar size={13} /> تاريخ الموعد
                  </div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, marginTop: 4, color: 'var(--text-primary)' }}>
                    {formatArabicFullDate(appointment.dateStr)}
                  </div>
                </div>

                <div style={{ background: 'var(--bg-secondary)', padding: '12px', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <Clock size={13} /> التوقيت والمدة (نظام 12 ساعة)
                  </div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, marginTop: 4, color: 'var(--primary)' }}>
                    {format12Hour(appointment.startTime)} - {format12Hour(appointment.endTime)}
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginRight: 6 }}>({calculateDuration()} دقيقة)</span>
                  </div>
                </div>

                <div style={{ background: 'var(--bg-secondary)', padding: '12px', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <Stethoscope size={13} /> نوع الإجراء
                  </div>
                  <div style={{ fontSize: '1rem', fontWeight: 800, marginTop: 4, color: 'var(--primary)' }}>
                    {appointment.procedureName}
                  </div>
                </div>

                <div style={{ background: 'var(--bg-secondary)', padding: '12px', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <DollarSign size={13} /> التكلفة الإجمالية
                  </div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: 4, color: '#10b981' }}>
                    {appointment.price} ج.م
                  </div>
                </div>

                {/* Booking Employee Row Prominently Displayed */}
                <div style={{
                  background: 'var(--bg-secondary)',
                  padding: '12px 14px',
                  borderRadius: '10px',
                  border: '1px solid var(--border-color)',
                  gridColumn: '1 / -1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '8px',
                      background: 'rgba(0,180,216,0.15)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--primary)'
                    }}>
                      <User size={18} />
                    </div>
                    <div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                        الموظف المسؤول عن الحجز
                      </div>
                      <div style={{ fontSize: '0.98rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {appointment.bookedByUserName || 'د. محمد (أدمن)'}
                      </div>
                    </div>
                  </div>
                  <span style={{
                    fontSize: '0.75rem',
                    background: 'rgba(0,180,216,0.12)',
                    color: 'var(--primary)',
                    padding: '4px 10px',
                    borderRadius: '6px',
                    fontWeight: 700
                  }}>
                    {appointment.bookedByUserId === currentUser.id ? 'مسجل بواسطتك' : 'موظف مسجل'}
                  </span>
                </div>
              </div>

              {/* Quick Status Change Chips (Like Android App) */}
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '8px' }}>
                  تغيير حالة الموعد مباشرة:
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {statusOptions.map(st => {
                    const isSelected = appointment.status === st.key ||
                      (st.key === 'COMPLETED' && appointment.status === 'ATTENDED');
                    return (
                      <button
                        key={st.key}
                        onClick={() => handleStatusChange(st.key)}
                        style={{
                          padding: '8px 14px',
                          borderRadius: '8px',
                          border: `1.5px solid ${isSelected ? st.color : 'var(--border-color)'}`,
                          background: isSelected ? st.color : 'var(--bg-secondary)',
                          color: isSelected ? 'white' : 'var(--text-primary)',
                          fontFamily: 'inherit',
                          fontSize: '0.85rem',
                          fontWeight: isSelected ? 800 : 600,
                          cursor: 'pointer',
                          transition: 'all 0.2s',
                          boxShadow: isSelected ? '0 2px 8px rgba(0,0,0,0.2)' : 'none'
                        }}
                      >
                        {st.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Notes */}
              <div style={{
                background: 'var(--bg-secondary)',
                padding: '12px',
                borderRadius: '10px',
                border: '1px solid var(--border-color)',
                marginBottom: '16px',
                fontSize: '0.85rem'
              }}>
                <div style={{ color: 'var(--text-secondary)', fontWeight: 700, marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <FileText size={14} /> ملاحظات:
                </div>
                <div style={{ color: appointment.notes ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                  {appointment.notes || 'لا توجد ملاحظات مسجلة لهذا الموعد'}
                </div>
              </div>
            </div>
          ) : (
            /* WhatsApp Templates Tab */
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '420px', overflowY: 'auto' }}>
              <div style={{
                background: 'rgba(37,211,102,0.1)',
                border: '1px solid rgba(37,211,102,0.3)',
                borderRadius: '8px',
                padding: '10px 14px',
                fontSize: '0.85rem',
                color: '#4ade80',
                display: 'flex',
                alignItems: 'center',
                gap: 8
              }}>
                <Info size={16} />
                <span>يتم تعويض بيانات المريض وتاريخ الموعد تلقائياً في الرسائل قبل الإرسال.</span>
              </div>

              {templateCards.map(tpl => {
                const formattedMsg = getTemplateFormattedMessage(tpl.key);
                const isCopied = copiedKey === tpl.key;

                return (
                  <div
                    key={tpl.key}
                    style={{
                      background: 'var(--bg-secondary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: '10px',
                      padding: '14px'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <div style={{ fontWeight: 800, fontSize: '0.95rem', color: tpl.color }}>
                        {tpl.title}
                      </div>

                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          className="btn-secondary"
                          style={{ padding: '5px 10px', fontSize: '0.75rem' }}
                          onClick={() => copyMessage(tpl.key)}
                          title="نسخ الرسالة"
                        >
                          <Copy size={13} /> {isCopied ? 'تم النسخ!' : 'نسخ'}
                        </button>
                        <button
                          className="whatsapp-action-btn"
                          style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                          onClick={() => sendWhatsApp(tpl.key)}
                        >
                          <Send size={13} /> إرسال عبر واتساب 💬
                        </button>
                      </div>
                    </div>

                    <div style={{
                      background: 'var(--bg-input)',
                      padding: '10px 12px',
                      borderRadius: '6px',
                      fontSize: '0.85rem',
                      color: 'var(--text-primary)',
                      lineHeight: 1.5,
                      whiteSpace: 'pre-line'
                    }}>
                      {formattedMsg}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Footer Actions */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            paddingTop: '16px',
            borderTop: '1px solid var(--border-color)',
            marginTop: '12px'
          }}>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button className="btn-danger" onClick={handleCancel}>
                إلغاء الموعد
              </button>
              <button className="btn-secondary" style={{ color: '#ef4444' }} onClick={handleDelete}>
                <Trash2 size={15} /> حذف نهائي
              </button>
            </div>

            <button className="btn-secondary" onClick={onClose}>
              إغلاق
            </button>
          </div>
        </div>
      </div>

      {/* Post-Treatment Popup Dialog (Triggered on COMPLETED like Android App) */}
      {showPostTreatmentModal && (
        <div className="modal-overlay" style={{ zIndex: 1200 }}>
          <div className="modal-content" style={{ maxWidth: '440px', textAlign: 'center', padding: '24px' }} onClick={e => e.stopPropagation()}>
            <div style={{
              width: '60px',
              height: '60px',
              borderRadius: '50%',
              background: 'rgba(37,211,102,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              color: '#25d366'
            }}>
              <CheckCircle size={36} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '8px', color: 'var(--text-primary)' }}>
              اكتمال الموعد ✅
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '20px' }}>
              اكتمل العلاج للمريض <b>'{appointment.patientName}'</b>.<br />
              هل ترغب في إرسال رسالة <b>(قالب بعد العلاج)</b> عبر واتساب الآن أم إرسالها لاحقاً؟
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
              <button
                className="whatsapp-action-btn"
                style={{ padding: '10px 20px', fontWeight: 800, fontSize: '0.95rem' }}
                onClick={() => {
                  setShowPostTreatmentModal(false);
                  sendWhatsApp('AFTER_TREATMENT');
                }}
              >
                <Send size={15} /> إرسال الآن 💬
              </button>
              <button
                className="btn-secondary"
                style={{ padding: '10px 18px', fontWeight: 700 }}
                onClick={() => setShowPostTreatmentModal(false)}
              >
                إرسال لاحقاً
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Permission Denied Modal (Admin Only Restriction for non-creators) */}
      {showPermissionModal && (
        <div className="modal-overlay" style={{ zIndex: 1200 }}>
          <div className="modal-content" style={{ maxWidth: '440px', textAlign: 'center', padding: '24px' }} onClick={e => e.stopPropagation()}>
            <div style={{
              width: '60px',
              height: '60px',
              borderRadius: '50%',
              background: 'rgba(239,68,68,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              color: '#ef4444'
            }}>
              <Lock size={34} />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '8px', color: '#ef4444' }}>
              عفواً، لا تملك الصلاحية
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '20px' }}>
              لا يمكنك إلغاء أو حذف هذا الموعد لأنه مسجل بواسطة: <b>'{appointment.bookedByUserName || 'موظف آخر'}'</b>.<br /><br />
              إلغاء وحذف مواعيد الموظفين الآخرين متاح فقط لحساب الأدمن.
            </p>
            <button
              className="btn-primary"
              style={{ width: '100%', padding: '10px', fontWeight: 800 }}
              onClick={() => setShowPermissionModal(false)}
            >
              حسناً
            </button>
          </div>
        </div>
      )}

      {/* Patient Edit Modal */}
      {isEditPatientModalOpen && (
        <PatientModal
          isOpen={isEditPatientModalOpen}
          onClose={() => setIsEditPatientModalOpen(false)}
          patientToEdit={
            patients.find(p => p.id === appointment.patientId || p.name === appointment.patientName) || {
              id: appointment.patientId || Date.now(),
              name: appointment.patientName,
              phone: appointment.patientPhone || '',
              classification: appointment.patientClassification || 'NORMAL',
              notes: appointment.notes || '',
              createdAt: Date.now()
            }
          }
          currentUser={currentUser}
        />
      )}
    </>
  );
};
