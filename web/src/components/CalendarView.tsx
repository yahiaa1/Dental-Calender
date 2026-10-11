import React, { useState, useEffect, useRef } from 'react';
import {
  ChevronRight,
  ChevronLeft,
  Calendar as CalendarIcon,
  Clock,
  Plus,
  CalendarDays,
  Grid,
  Columns,
  Move,
  User,
  Eye,
  EyeOff
} from 'lucide-react';
import type { Appointment, User as UserModel, WhatsAppTemplate, ClinicSettings, ProcedureConfig, Patient } from '../types';
import { AppointmentDetailsModal } from './AppointmentDetailsModal';
import { moveAppointment } from '../services/clinicService';

interface Props {
  appointments: Appointment[];
  procedures?: ProcedureConfig[];
  templates: WhatsAppTemplate[];
  settings: ClinicSettings;
  currentUser: UserModel;
  patients?: Patient[];
  onOpenNewBooking: (dateStr: string, startTime?: string, endTime?: string) => void;
  onEditAppointment: (app: Appointment) => void;
}

type ViewMode = 'DAY' | 'THREE_DAYS' | 'WEEK' | 'MONTH';

export const CalendarView: React.FC<Props> = ({
  appointments,
  procedures = [],
  templates,
  settings,
  currentUser,
  patients = [],
  onOpenNewBooking,
  onEditAppointment
}) => {
  const [selectedDate, setSelectedDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [viewMode, setViewMode] = useState<ViewMode>('DAY');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [hideCancelled, setHideCancelled] = useState<boolean>(true);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

  // Drag and Drop appointment rescheduling state
  const [draggedAppointmentId, setDraggedAppointmentId] = useState<number | null>(null);
  const [dragOverSlot, setDragOverSlot] = useState<{ dateStr: string; hour: number } | null>(null);

  // Mouse drag-to-select time range state
  const [rangeSelection, setRangeSelection] = useState<{
    dateStr: string;
    anchorMins: number;
    currentMins: number;
  } | null>(null);
  const [isMouseDownSelecting, setIsMouseDownSelecting] = useState(false);
  const isMouseDownSelectingRef = useRef(isMouseDownSelecting);
  isMouseDownSelectingRef.current = isMouseDownSelecting;
  const rangeSelectionRef = useRef(rangeSelection);
  rangeSelectionRef.current = rangeSelection;

  // Dynamic Clinic working hours synced from Firebase
  const startHour = settings.workStartHour ?? 14; // e.g. 14 (2 PM)
  const endHour = settings.workEndHour ?? 24;     // e.g. 24 (12 AM midnight)
  const totalHours = Math.max(endHour - startHour, 1);
  const hourHeight = 74; // px per hour

  // Global mouseup listener for drag-to-select range
  useEffect(() => {
    const handleGlobalMouseUp = () => {
      if (isMouseDownSelectingRef.current && rangeSelectionRef.current) {
        const sel = rangeSelectionRef.current;
        const sMins = Math.min(sel.anchorMins, sel.currentMins);
        let eMins = Math.max(sel.anchorMins, sel.currentMins);
        if (eMins - sMins < 15) {
          eMins = sMins + 30;
        }

        const sHour = startHour + Math.floor(sMins / 60);
        const sMin = sMins % 60;
        const eHour = startHour + Math.floor(eMins / 60);
        const eMin = eMins % 60;

        const sTime = `${String(sHour).padStart(2, '0')}:${String(sMin).padStart(2, '0')}`;
        const eTime = `${String(eHour).padStart(2, '0')}:${String(eMin).padStart(2, '0')}`;

        onOpenNewBooking(sel.dateStr, sTime, eTime);
        setRangeSelection(null);
        setIsMouseDownSelecting(false);
      }
    };

    window.addEventListener('mouseup', handleGlobalMouseUp);
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp);
  }, [startHour, onOpenNewBooking]);

  // Date utilities
  const changeDateBy = (days: number) => {
    const cur = new Date(selectedDate);
    cur.setDate(cur.getDate() + days);
    setSelectedDate(cur.toISOString().split('T')[0]);
  };

  const goToToday = () => {
    setSelectedDate(new Date().toISOString().split('T')[0]);
  };

  const formatArabicMonthYear = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('ar-EG', { month: 'long', year: 'numeric' });
  };

  const formatArabicFullDate = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('ar-EG', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  };

  const format12Hour = (time24: string) => {
    try {
      const [h, m] = time24.split(':').map(Number);
      const isPm = h >= 12;
      const h12 = h % 12 === 0 ? 12 : h % 12;
      return `${h12}:${m.toString().padStart(2, '0')} ${isPm ? 'م' : 'ص'}`;
    } catch {
      return time24;
    }
  };

  // Generate 7 days week strip around selected date (Saturday to Friday)
  const getWeekDays = () => {
    const d = new Date(selectedDate);
    const dayOfWeek = d.getDay(); // 0: Sun, 6: Sat
    const diffToSat = (dayOfWeek + 1) % 7;
    const sat = new Date(d);
    sat.setDate(sat.getDate() - diffToSat);

    const week: { dateStr: string; dayName: string; dayNum: number; isSelected: boolean }[] = [];
    const arabicDays = ['السبت', 'الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'];

    for (let i = 0; i < 7; i++) {
      const cur = new Date(sat);
      cur.setDate(cur.getDate() + i);
      const str = cur.toISOString().split('T')[0];
      week.push({
        dateStr: str,
        dayName: arabicDays[i],
        dayNum: cur.getDate(),
        isSelected: str === selectedDate
      });
    }
    return week;
  };

  const getDisplayDates = (): string[] => {
    if (viewMode === 'DAY') return [selectedDate];
    if (viewMode === 'THREE_DAYS') {
      const dates: string[] = [];
      const cur = new Date(selectedDate);
      for (let i = 0; i < 3; i++) {
        const d = new Date(cur);
        d.setDate(d.getDate() + i);
        dates.push(d.toISOString().split('T')[0]);
      }
      return dates;
    }
    if (viewMode === 'WEEK') {
      return getWeekDays().map(w => w.dateStr);
    }
    return [selectedDate];
  };

  const weekStrip = getWeekDays();
  const displayDates = getDisplayDates();

  // Calculate day stats from raw appointments for today
  const allDayAppointments = appointments.filter(a => a.dateStr === selectedDate);
  const totalCount = allDayAppointments.length;
  const confirmedCount = allDayAppointments.filter(a => a.status === 'CONFIRMED').length;
  const attendedCount = allDayAppointments.filter(a => a.status === 'ATTENDED' || a.status === 'COMPLETED').length;
  const cancelledCount = allDayAppointments.filter(a => a.status === 'CANCELLED').length;
  const activeRevenue = allDayAppointments
    .filter(a => a.status !== 'CANCELLED' && a.status !== 'NO_SHOW')
    .reduce((sum, a) => sum + (a.price || 0), 0);

  // Filtered appointments for single chair
  const filteredAppointments = appointments.filter(a => {
    if (statusFilter === 'CANCELLED') return a.status === 'CANCELLED';
    if (hideCancelled && a.status === 'CANCELLED') return false;
    if (statusFilter !== 'ALL' && a.status !== statusFilter) return false;
    return true;
  });

  // Position calculation for timeline (minutes from startHour)
  const getAppointmentTopAndHeight = (app: Appointment) => {
    try {
      const [h1, m1] = app.startTime.split(':').map(Number);
      const [h2, m2] = app.endTime.split(':').map(Number);

      const startMinutes = (h1 - startHour) * 60 + m1;
      const endMinutes = (h2 - startHour) * 60 + m2;
      const duration = Math.max(endMinutes - startMinutes, 25);

      const top = (startMinutes / 60) * hourHeight;
      const height = Math.max((duration / 60) * hourHeight, 36);

      return { top: Math.max(top, 0), height };
    } catch {
      return { top: 0, height: hourHeight };
    }
  };

  // Drag and Drop Handler (Move appointment)
  const handleDropAppointment = async (targetDateStr: string, targetHour: number) => {
    if (!draggedAppointmentId) return;
    const app = appointments.find(a => a.id === draggedAppointmentId);
    if (!app) return;

    try {
      const [h1, m1] = app.startTime.split(':').map(Number);
      const [h2, m2] = app.endTime.split(':').map(Number);
      const originalDuration = Math.max((h2 * 60 + m2) - (h1 * 60 + m1), 20);

      const newStartHour = targetHour;
      const newStartMinute = 0;
      const newEndTotal = newStartHour * 60 + newStartMinute + originalDuration;
      const endH = Math.floor(newEndTotal / 60) % 24;
      const endM = newEndTotal % 60;

      const newStartTime = `${String(newStartHour).padStart(2, '0')}:00`;
      const newEndTime = `${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`;

      await moveAppointment(app, targetDateStr, newStartTime, newEndTime, currentUser);
    } catch (e: any) {
      alert('خطأ أثناء نقل الموعد: ' + e.message);
    } finally {
      setDraggedAppointmentId(null);
      setDragOverSlot(null);
    }
  };

  // Mouse Drag-to-select range handlers on empty column slots
  const handleMouseDownColumn = (e: React.MouseEvent<HTMLDivElement>, dateStr: string) => {
    if ((e.target as HTMLElement).closest('.appointment-card-item')) return;
    if (draggedAppointmentId) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const y = e.clientY - rect.top;
    const minsFromStart = (y / hourHeight) * 60;
    const snappedStart = Math.floor(minsFromStart / 15) * 15;
    const clampedStart = Math.max(0, Math.min(totalHours * 60 - 15, snappedStart));

    setRangeSelection({
      dateStr,
      anchorMins: clampedStart,
      currentMins: clampedStart + 30
    });
    setIsMouseDownSelecting(true);
  };

  const handleMouseMoveColumn = (e: React.MouseEvent<HTMLDivElement>, dateStr: string) => {
    if (!isMouseDownSelecting || !rangeSelection || rangeSelection.dateStr !== dateStr) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const y = e.clientY - rect.top;
    const minsFromStart = (y / hourHeight) * 60;
    const snappedCurrent = Math.round(minsFromStart / 15) * 15;
    const clampedCurrent = Math.max(0, Math.min(totalHours * 60, snappedCurrent));

    setRangeSelection(prev => prev ? { ...prev, currentMins: clampedCurrent } : null);
  };

  // Month View Days
  const getMonthDays = () => {
    const d = new Date(selectedDate);
    const year = d.getFullYear();
    const month = d.getMonth();
    const firstDayOfMonth = new Date(year, month, 1);
    const lastDayOfMonth = new Date(year, month + 1, 0);

    const firstDayIndex = (firstDayOfMonth.getDay() + 1) % 7;
    const totalDays = lastDayOfMonth.getDate();

    const days = [];
    for (let i = 0; i < firstDayIndex; i++) {
      days.push(null);
    }
    for (let day = 1; day <= totalDays; day++) {
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      days.push(dateStr);
    }
    return days;
  };

  return (
    <div>
      {/* Top Controls Bar */}
      <div className="controls-bar" style={{ padding: '14px 20px', gap: '14px' }}>
        {/* Navigation & Month */}
        <div className="date-controls">
          <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--primary)', minWidth: '150px' }}>
            {formatArabicMonthYear(selectedDate)}
          </div>

          <button className="date-nav-btn" onClick={() => changeDateBy(-1)} title="اليوم السابق">
            <ChevronRight size={20} />
          </button>
          <button className="btn-secondary" onClick={goToToday} style={{ padding: '6px 14px', fontWeight: 700 }}>
            اليوم
          </button>
          <button className="date-nav-btn" onClick={() => changeDateBy(1)} title="اليوم التالي">
            <ChevronLeft size={20} />
          </button>

          <input
            type="date"
            className="custom-input"
            value={selectedDate}
            onChange={e => setSelectedDate(e.target.value)}
            style={{ width: '150px' }}
          />
        </div>

        {/* View Mode Toggle */}
        <div style={{ display: 'flex', background: 'var(--bg-input)', padding: '4px', borderRadius: '10px', gap: '4px' }}>
          <button
            className={`nav-pill-btn ${viewMode === 'DAY' ? 'active' : ''}`}
            style={{ padding: '6px 14px', fontSize: '0.85rem' }}
            onClick={() => setViewMode('DAY')}
          >
            <Columns size={15} /> يوم
          </button>
          <button
            className={`nav-pill-btn ${viewMode === 'THREE_DAYS' ? 'active' : ''}`}
            style={{ padding: '6px 14px', fontSize: '0.85rem' }}
            onClick={() => setViewMode('THREE_DAYS')}
          >
            <Grid size={15} /> 3 أيام
          </button>
          <button
            className={`nav-pill-btn ${viewMode === 'WEEK' ? 'active' : ''}`}
            style={{ padding: '6px 14px', fontSize: '0.85rem' }}
            onClick={() => setViewMode('WEEK')}
          >
            <CalendarDays size={15} /> أسبوع
          </button>
          <button
            className={`nav-pill-btn ${viewMode === 'MONTH' ? 'active' : ''}`}
            style={{ padding: '6px 14px', fontSize: '0.85rem' }}
            onClick={() => setViewMode('MONTH')}
          >
            <CalendarIcon size={15} /> شهر
          </button>
        </div>

        {/* Status Filter, Hide Cancelled Toggle & New Booking Button */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => setHideCancelled(prev => !prev)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '7px 12px',
              fontSize: '0.85rem',
              fontWeight: 700,
              background: hideCancelled ? 'rgba(239, 68, 68, 0.12)' : 'var(--bg-secondary)',
              border: `1.5px solid ${hideCancelled ? 'rgba(239, 68, 68, 0.5)' : 'var(--border-color)'}`,
              color: hideCancelled ? '#ef4444' : 'var(--text-secondary)',
              cursor: 'pointer',
              borderRadius: '8px',
              transition: 'all 0.15s'
            }}
            title={hideCancelled ? 'المواعيد الملغاة مخفية حالياً (اضغط لإظهارها)' : 'المواعيد الملغاة معروضة حالياً (اضغط لإخفائها)'}
          >
            {hideCancelled ? <EyeOff size={15} color="#ef4444" /> : <Eye size={15} color="#10b981" />}
            <span>{hideCancelled ? 'إخفاء الملغي' : 'إظهار الملغي'}</span>
            {cancelledCount > 0 && (
              <span style={{
                background: hideCancelled ? '#ef4444' : '#64748b',
                color: '#ffffff',
                borderRadius: '10px',
                padding: '0 6px',
                fontSize: '0.72rem',
                fontWeight: 800
              }}>
                {cancelledCount}
              </span>
            )}
          </button>

          <select
            className="custom-select"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ fontSize: '0.85rem' }}
          >
            <option value="ALL">جميع الحالات</option>
            <option value="CONFIRMED">المؤكدة</option>
            <option value="UNCONFIRMED">غير المؤكدة</option>
            <option value="ATTENDED">تم الحضور</option>
            <option value="NO_SHOW">لم يحضر</option>
            <option value="CANCELLED">الملغية فقط</option>
          </select>

          <button
            className="btn-primary"
            onClick={() => onOpenNewBooking(selectedDate)}
          >
            <Plus size={18} /> حجز موعد جديد
          </button>
        </div>
      </div>

      {/* Week Days Strip */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(7, 1fr)',
        gap: '8px',
        background: 'var(--bg-card)',
        backdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '12px',
        marginBottom: '16px'
      }}>
        {weekStrip.map(day => {
          const dayApps = filteredAppointments.filter(a => a.dateStr === day.dateStr);
          const hasApps = dayApps.length > 0;

          return (
            <div
              key={day.dateStr}
              onClick={() => setSelectedDate(day.dateStr)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                padding: '8px 4px',
                borderRadius: '12px',
                cursor: 'pointer',
                background: day.isSelected ? 'linear-gradient(135deg, var(--primary), #0077b6)' : 'transparent',
                color: day.isSelected ? 'white' : 'var(--text-primary)',
                border: day.isSelected ? '1px solid rgba(255,255,255,0.2)' : '1px solid transparent',
                transition: 'all 0.2s',
                boxShadow: day.isSelected ? '0 4px 14px var(--primary-glow)' : 'none'
              }}
            >
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: day.isSelected ? 'white' : 'var(--text-secondary)' }}>
                {day.dayName}
              </span>
              <span style={{ fontSize: '1.2rem', fontWeight: 800, marginTop: '2px' }}>
                {day.dayNum}
              </span>
              <div style={{ display: 'flex', gap: '3px', marginTop: '4px', height: '6px', alignItems: 'center' }}>
                {hasApps ? (
                  dayApps.slice(0, 3).map((a, i) => (
                    <div
                      key={i}
                      style={{
                        width: '6px',
                        height: '6px',
                        borderRadius: '50%',
                        background: day.isSelected ? '#ffffff' : (a.status === 'CONFIRMED' ? '#10b981' : a.status === 'ATTENDED' ? '#3b82f6' : a.status === 'CANCELLED' ? '#ef4444' : '#f59e0b')
                      }}
                    />
                  ))
                ) : (
                  <div style={{ width: '6px', height: '6px' }} />
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Day Overview Strip */}
      <div className="stats-strip" style={{ marginBottom: '16px' }}>
        <div className="stat-card" style={{ padding: '12px 16px' }}>
          <div className="stat-info">
            <span className="stat-label">مواعيد {formatArabicFullDate(selectedDate)}</span>
            <span className="stat-value">{totalCount}</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(0,180,216,0.15)', color: 'var(--primary)' }}>
            <CalendarIcon size={20} />
          </div>
        </div>

        <div className="stat-card" style={{ padding: '12px 16px' }}>
          <div className="stat-info">
            <span className="stat-label">المؤكدة</span>
            <span className="stat-value" style={{ color: '#10b981' }}>{confirmedCount}</span>
          </div>
        </div>

        <div className="stat-card" style={{ padding: '12px 16px' }}>
          <div className="stat-info">
            <span className="stat-label">تم الحضور</span>
            <span className="stat-value" style={{ color: '#3b82f6' }}>{attendedCount}</span>
          </div>
        </div>

        <div
          className="stat-card"
          style={{
            padding: '12px 16px',
            cursor: 'pointer',
            border: hideCancelled ? '1.5px solid rgba(239, 68, 68, 0.4)' : '1px solid var(--border-color)',
            background: hideCancelled ? 'rgba(239, 68, 68, 0.06)' : 'var(--bg-card)',
            transition: 'all 0.15s'
          }}
          onClick={() => setHideCancelled(prev => !prev)}
          title={hideCancelled ? 'المواعيد الملغاة مخفية حالياً (اضغط لإظهارها)' : 'المواعيد الملغاة معروضة حالياً (اضغط لإخفائها)'}
        >
          <div className="stat-info">
            <span className="stat-label">الملغاة {hideCancelled ? '(مخفية 👁️)' : '(ظاهرة)'}</span>
            <span className="stat-value" style={{ color: '#ef4444' }}>{cancelledCount}</span>
          </div>
        </div>

        <div className="stat-card" style={{ padding: '12px 16px' }}>
          <div className="stat-info">
            <span className="stat-label">دوام العيادة النشط</span>
            <span className="stat-value" style={{ fontSize: '1.1rem', color: 'var(--primary)' }}>
              {format12Hour(`${startHour}:00`)} - {format12Hour(`${endHour}:00`)}
            </span>
          </div>
        </div>

        <div className="stat-card" style={{ padding: '12px 16px' }}>
          <div className="stat-info">
            <span className="stat-label">الإيراد المتوقع (بدون الملغي)</span>
            <span className="stat-value" style={{ color: '#2dd4bf' }}>{activeRevenue} ج.م</span>
          </div>
        </div>
      </div>

      {/* Main Calendar Body: Timeline or Month View */}
      {viewMode === 'MONTH' ? (
        /* Full Month Grid View */
        <div style={{
          background: 'var(--bg-card)',
          backdropFilter: 'var(--glass-blur)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px'
        }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
            gap: '8px',
            textAlign: 'center',
            marginBottom: '10px',
            fontWeight: 700,
            color: 'var(--text-secondary)'
          }}>
            {['السبت', 'الأحد', 'الإثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'].map(d => (
              <div key={d} style={{ padding: '8px' }}>{d}</div>
            ))}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '8px' }}>
            {getMonthDays().map((dStr, idx) => {
              if (!dStr) {
                return <div key={`empty-${idx}`} style={{ minHeight: '90px' }} />;
              }
              const dNum = Number(dStr.split('-')[2]);
              const dApps = filteredAppointments.filter(a => a.dateStr === dStr);
              const isSelected = dStr === selectedDate;

              return (
                <div
                  key={dStr}
                  onClick={() => {
                    setSelectedDate(dStr);
                    setViewMode('DAY');
                  }}
                  style={{
                    minHeight: '90px',
                    background: isSelected ? 'rgba(0,180,216,0.15)' : 'var(--bg-secondary)',
                    border: `1.5px solid ${isSelected ? 'var(--primary)' : 'var(--grid-line)'}`,
                    borderRadius: '10px',
                    padding: '8px',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'all 0.2s'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontWeight: 800, color: isSelected ? 'var(--primary)' : 'var(--text-primary)' }}>{dNum}</span>
                    {dApps.length > 0 && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{dApps.length} مواعيد</span>
                    )}
                  </div>

                  <div style={{ marginTop: '6px', display: 'flex', flexDirection: 'column', gap: '4px', overflow: 'hidden' }}>
                    {dApps.slice(0, 2).map(app => {
                      const proc = procedures.find(p => p.name === app.procedureName);
                      const procColor = proc?.colorHex || '#0284c7';
                      const statusLabels: Record<string, string> = {
                        CONFIRMED: 'مؤكد',
                        UNCONFIRMED: 'لم يتم التاكيد',
                        IN_TREATMENT: 'جار العلاج',
                        COMPLETED: 'مكتمل',
                        ATTENDED: 'مكتمل',
                        CANCELLED: 'ملغي',
                        NO_SHOW: 'لم يحضر'
                      };
                      const statusText = statusLabels[app.status] || app.status;

                      return (
                        <div
                          key={app.id}
                          style={{
                            fontSize: '0.75rem',
                            background: procColor,
                            color: '#ffffff',
                            padding: '2px 6px',
                            borderRadius: '4px',
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            fontWeight: 600
                          }}
                        >
                          {format12Hour(app.startTime)} {app.patientName} [{statusText}]
                        </div>
                      );
                    })}
                    {dApps.length > 2 && (
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>+{dApps.length - 2} المزيد</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        /* Timeline View with Drag & Drop AND Mouse Drag-to-Select Range */
        <div style={{
          background: 'var(--bg-card)',
          backdropFilter: 'var(--glass-blur)',
          border: '1px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          overflow: 'hidden',
          userSelect: isMouseDownSelecting ? 'none' : 'auto'
        }}>
          {displayDates.length > 1 && (
            <div style={{
              display: 'grid',
              gridTemplateColumns: `85px repeat(${displayDates.length}, 1fr)`,
              background: 'var(--bg-secondary)',
              borderBottom: '1px solid var(--border-color)',
              textAlign: 'center',
              fontWeight: 700,
              fontSize: '0.9rem'
            }}>
              <div style={{ padding: '12px', borderLeft: '1px solid var(--border-color)' }}>الوقت</div>
              {displayDates.map(dateStr => (
                <div
                  key={dateStr}
                  onClick={() => setSelectedDate(dateStr)}
                  style={{
                    padding: '12px',
                    borderRight: '1px solid var(--border-color)',
                    cursor: 'pointer',
                    background: dateStr === selectedDate ? 'rgba(0,180,216,0.12)' : 'transparent',
                    color: dateStr === selectedDate ? 'var(--primary)' : 'var(--text-primary)'
                  }}
                >
                  {formatArabicFullDate(dateStr)}
                </div>
              ))}
            </div>
          )}

          {/* Timeline Body with Vertical Time Grid */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: displayDates.length > 1 ? `85px repeat(${displayDates.length}, 1fr)` : '85px 1fr',
            position: 'relative',
            maxHeight: '720px',
            overflowY: 'auto'
          }}>
            {/* Time Axis Column */}
            <div style={{ background: 'var(--time-col-bg)', borderLeft: '1px solid var(--border-color)' }}>
              {Array.from({ length: totalHours }).map((_, idx) => {
                const hour = startHour + idx;
                const h12 = hour % 12 === 0 ? 12 : hour % 12;
                const amPm = hour >= 12 && hour < 24 ? 'م' : 'ص';
                return (
                  <div
                    key={hour}
                    style={{
                      height: `${hourHeight}px`,
                      borderBottom: '1px solid var(--grid-line)',
                      display: 'flex',
                      alignItems: 'flex-start',
                      justifyContent: 'center',
                      paddingTop: '6px',
                      fontSize: '0.85rem',
                      color: 'var(--text-secondary)',
                      fontWeight: 600
                    }}
                  >
                    {h12}:00 {amPm}
                  </div>
                );
              })}
            </div>

            {/* Date Columns */}
            {displayDates.map(dateStr => {
              const appsForDate = filteredAppointments.filter(a => a.dateStr === dateStr);

              // Calculate range selection preview rectangle if actively selecting in this column
              let selTop = 0;
              let selHeight = 0;
              let selLabel = '';
              if (rangeSelection && rangeSelection.dateStr === dateStr) {
                const sMins = Math.min(rangeSelection.anchorMins, rangeSelection.currentMins);
                let eMins = Math.max(rangeSelection.anchorMins, rangeSelection.currentMins);
                if (eMins - sMins < 15) eMins = sMins + 30;

                selTop = (sMins / 60) * hourHeight;
                selHeight = ((eMins - sMins) / 60) * hourHeight;

                const sH = startHour + Math.floor(sMins / 60);
                const sM = sMins % 60;
                const eH = startHour + Math.floor(eMins / 60);
                const eM = eMins % 60;
                const dur = eMins - sMins;
                selLabel = `حجز: ${format12Hour(`${String(sH).padStart(2, '0')}:${String(sM).padStart(2, '0')}`)} إلى ${format12Hour(`${String(eH).padStart(2, '0')}:${String(eM).padStart(2, '0')}`)} (${dur} دقيقة)`;
              }

              return (
                <div
                  key={dateStr}
                  onMouseDown={(e) => handleMouseDownColumn(e, dateStr)}
                  onMouseMove={(e) => handleMouseMoveColumn(e, dateStr)}
                  style={{
                    position: 'relative',
                    height: `${totalHours * hourHeight}px`,
                    borderRight: '1px solid var(--border-color)',
                    cursor: isMouseDownSelecting ? 'crosshair' : 'default'
                  }}
                >
                  {/* Background Hour Slot Lines */}
                  {Array.from({ length: totalHours }).map((_, idx) => {
                    const hour = startHour + idx;
                    const isOver = dragOverSlot?.dateStr === dateStr && dragOverSlot?.hour === hour;

                    return (
                      <div
                        key={hour}
                        onDragOver={(e) => {
                          e.preventDefault();
                          e.dataTransfer.dropEffect = 'move';
                          if (dragOverSlot?.dateStr !== dateStr || dragOverSlot?.hour !== hour) {
                            setDragOverSlot({ dateStr, hour });
                          }
                        }}
                        onDragLeave={() => {
                          if (dragOverSlot?.dateStr === dateStr && dragOverSlot?.hour === hour) {
                            setDragOverSlot(null);
                          }
                        }}
                        onDrop={(e) => {
                          e.preventDefault();
                          handleDropAppointment(dateStr, hour);
                        }}
                        style={{
                          height: `${hourHeight}px`,
                          borderBottom: '1px solid var(--grid-line)',
                          background: isOver ? 'rgba(0, 180, 216, 0.25)' : 'transparent',
                          outline: isOver ? '2px dashed var(--primary)' : 'none',
                          outlineOffset: '-2px',
                          transition: 'background 0.15s',
                          position: 'relative'
                        }}
                      />
                    );
                  })}

                  {/* Mouse Drag-to-Select Visual Preview Box */}
                  {rangeSelection && rangeSelection.dateStr === dateStr && (
                    <div
                      style={{
                        position: 'absolute',
                        top: `${selTop}px`,
                        height: `${selHeight}px`,
                        right: '8px',
                        left: '8px',
                        background: 'rgba(0, 180, 216, 0.32)',
                        border: '2px solid var(--primary)',
                        borderRadius: '8px',
                        zIndex: 35,
                        pointerEvents: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontWeight: 800,
                        fontSize: '0.85rem',
                        boxShadow: '0 4px 16px var(--primary-glow)',
                        textShadow: '0 1px 3px rgba(0,0,0,0.6)'
                      }}
                    >
                      {selLabel}
                    </div>
                  )}

                  {/* Positioned Appointments Cards with Procedure Color and Status in Brackets */}
                  {appsForDate.map(app => {
                    const { top, height } = getAppointmentTopAndHeight(app);
                    const isDraggingThis = draggedAppointmentId === app.id;
                    const isCancelled = app.status === 'CANCELLED';

                    // Procedure color remains constant and does NOT change when status changes!
                    const proc = procedures.find(p => p.name === app.procedureName);
                    const procColor = proc?.colorHex || '#0284c7';

                    const statusLabels: Record<string, string> = {
                      CONFIRMED: 'مؤكد',
                      UNCONFIRMED: 'لم يتم التاكيد',
                      IN_TREATMENT: 'جار العلاج',
                      COMPLETED: 'مكتمل',
                      ATTENDED: 'مكتمل',
                      CANCELLED: 'ملغي',
                      NO_SHOW: 'لم يحضر'
                    };
                    const statusText = statusLabels[app.status] || app.status;

                    return (
                      <div
                        key={app.id}
                        className="appointment-card-item"
                        draggable={true}
                        onMouseDown={(e) => {
                          e.stopPropagation(); // Prevent range selection when interacting with appointment
                        }}
                        onDragStart={(e) => {
                          setDraggedAppointmentId(app.id);
                          e.dataTransfer.setData('text/plain', JSON.stringify({ id: app.id }));
                          e.dataTransfer.effectAllowed = 'move';
                        }}
                        onDragEnd={() => {
                          setDraggedAppointmentId(null);
                          setDragOverSlot(null);
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedAppointment(app);
                        }}
                        style={{
                          position: 'absolute',
                          top: `${top + 1}px`,
                          height: `${Math.max(height - 3, 34)}px`,
                          right: '6px',
                          left: '6px',
                          background: isCancelled ? 'rgba(239, 68, 68, 0.85)' : procColor,
                          border: isCancelled ? '1.5px dashed #ef4444' : `1px solid rgba(255, 255, 255, 0.2)`,
                          borderRight: isCancelled ? '5px solid #ef4444' : '5px solid rgba(255, 255, 255, 0.55)',
                          borderBottom: isCancelled ? '2.5px solid #b91c1c' : '2.5px solid rgba(0, 0, 0, 0.35)',
                          borderRadius: '8px',
                          padding: '6px 12px',
                          cursor: 'grab',
                          boxShadow: '0 4px 12px rgba(0,0,0,0.25)',
                          display: 'flex',
                          flexDirection: 'column',
                          justifyContent: 'space-between',
                          overflow: 'hidden',
                          zIndex: 10,
                          opacity: isCancelled ? 0.7 : (isDraggingThis ? 0.4 : 1),
                          transition: 'transform 0.15s, box-shadow 0.15s'
                        }}
                        onMouseEnter={e => {
                          if (!isDraggingThis) {
                            e.currentTarget.style.transform = 'translateY(-2px)';
                            e.currentTarget.style.boxShadow = '0 8px 20px rgba(0,0,0,0.4)';
                          }
                        }}
                        onMouseLeave={e => {
                          if (!isDraggingThis) {
                            e.currentTarget.style.transform = 'none';
                            e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.25)';
                          }
                        }}
                        title="اسحب لنقل الموعد لساعة أخرى، أو اضغط لعرض التفاصيل"
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <Move size={13} color="rgba(255,255,255,0.75)" />
                            <span style={{
                              fontWeight: 800,
                              fontSize: '0.95rem',
                              color: '#ffffff',
                              textDecoration: isCancelled ? 'line-through' : 'none'
                            }}>
                              {app.patientName}
                            </span>
                          </div>

                          <span style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            color: '#ffffff',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px'
                          }}>
                            <Clock size={12} />
                            {format12Hour(app.startTime)} - {format12Hour(app.endTime)}
                          </span>
                        </div>

                        {/* Visible Separator Line Between Patient & Procedure */}
                        <div style={{
                          height: '1px',
                          background: 'rgba(255, 255, 255, 0.35)',
                          margin: '3px 0',
                          width: '100%',
                          boxShadow: '0 1px 0 rgba(0, 0, 0, 0.15)'
                        }} />

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          {/* Procedure Name with Status in Brackets (Color doesn't change with status) */}
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{ fontSize: '0.86rem', color: '#ffffff', fontWeight: 800 }}>
                              {app.procedureName}
                            </span>
                            <span style={{
                              fontSize: '0.72rem',
                              background: 'rgba(0, 0, 0, 0.25)',
                              border: '1px solid rgba(255, 255, 255, 0.35)',
                              color: '#ffffff',
                              padding: '1px 6px',
                              borderRadius: '4px',
                              fontWeight: 700
                            }}>
                              [{statusText}]
                            </span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.9)', display: 'flex', alignItems: 'center', gap: 3 }}>
                              <User size={11} /> {app.bookedByUserName || 'طاقم العمل'}
                            </span>
                            <span style={{ fontSize: '0.8rem', fontWeight: 800, color: '#a7f3d0' }}>
                              {app.price} ج.م
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Appointment Details Modal */}
      <AppointmentDetailsModal
        appointment={selectedAppointment}
        isOpen={Boolean(selectedAppointment)}
        onClose={() => setSelectedAppointment(null)}
        onEdit={app => {
          setSelectedAppointment(null);
          onEditAppointment(app);
        }}
        currentUser={currentUser}
        templates={templates}
        patients={patients}
      />
    </div>
  );
};
