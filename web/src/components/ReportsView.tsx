import { DollarSign, XCircle, TrendingUp, Calendar, Stethoscope } from 'lucide-react';
import type { Appointment } from '../types';

interface Props {
  appointments: Appointment[];
}

export const ReportsView: React.FC<Props> = ({ appointments }) => {
  const todayStr = new Date().toISOString().split('T')[0];
  const thisMonthPrefix = todayStr.substring(0, 7); // YYYY-MM

  // Month appointments
  const monthAppointments = appointments.filter(a => a.dateStr.startsWith(thisMonthPrefix));

  // Active revenue (cancelled & no_show excluded)
  const totalActiveRevenue = appointments
    .filter(a => a.status !== 'CANCELLED' && a.status !== 'NO_SHOW')
    .reduce((sum, a) => sum + (a.price || 0), 0);

  const monthActiveRevenue = monthAppointments
    .filter(a => a.status !== 'CANCELLED' && a.status !== 'NO_SHOW')
    .reduce((sum, a) => sum + (a.price || 0), 0);

  const monthAttendedRevenue = monthAppointments
    .filter(a => a.status === 'ATTENDED')
    .reduce((sum, a) => sum + (a.price || 0), 0);

  const cancelledCount = monthAppointments.filter(a => a.status === 'CANCELLED').length;
  const attendedCount = monthAppointments.filter(a => a.status === 'ATTENDED').length;
  const confirmedCount = monthAppointments.filter(a => a.status === 'CONFIRMED').length;

  // Procedure breakdown
  const procCounts: Record<string, number> = {};
  monthAppointments.forEach(a => {
    procCounts[a.procedureName] = (procCounts[a.procedureName] || 0) + 1;
  });

  const sortedProcs = Object.entries(procCounts).sort((a, b) => b[1] - a[1]);

  return (
    <div>
      <div className="stats-strip" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">إيرادات هذا الشهر المتوقعة (النشطة)</span>
            <span className="stat-value" style={{ color: '#2dd4bf' }}>{monthActiveRevenue} ج.م</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(45,212,191,0.15)', color: '#2dd4bf' }}>
            <DollarSign size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">الإيرادات المحصلة بالفعل (تم الحضور)</span>
            <span className="stat-value" style={{ color: '#10b981' }}>{monthAttendedRevenue} ج.م</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(16,185,129,0.15)', color: '#10b981' }}>
            <TrendingUp size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">المواعيد المؤكدة هذا الشهر</span>
            <span className="stat-value" style={{ color: '#00b4d8' }}>{confirmedCount}</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(0,180,216,0.15)', color: '#00b4d8' }}>
            <Calendar size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">الحالات المنجزة (حضروا)</span>
            <span className="stat-value" style={{ color: '#10b981' }}>{attendedCount}</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(16,185,129,0.15)', color: '#10b981' }}>
            <TrendingUp size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">المواعيد الملغاة هذا الشهر</span>
            <span className="stat-value" style={{ color: '#ef4444' }}>{cancelledCount}</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(239,68,68,0.15)', color: '#ef4444' }}>
            <XCircle size={24} />
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-info">
            <span className="stat-label">إجمالي الإيراد النشط (كل الأوقات)</span>
            <span className="stat-value" style={{ color: '#38bdf8' }}>{totalActiveRevenue} ج.م</span>
          </div>
          <div className="stat-icon" style={{ background: 'rgba(56,189,248,0.15)', color: '#38bdf8' }}>
            <DollarSign size={24} />
          </div>
        </div>
      </div>

      {/* Procedures Popularity */}
      <div style={{
        background: 'var(--bg-card)',
        backdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        marginTop: '20px'
      }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 800, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Stethoscope size={20} color="#00b4d8" /> توزيع الإجراءات الطبية لهذا الشهر
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '14px' }}>
          {sortedProcs.length === 0 ? (
            <div style={{ color: '#64748b' }}>لا توجد بيانات لهذا الشهر</div>
          ) : (
            sortedProcs.map(([name, count]) => (
              <div key={name} style={{
                background: 'var(--bg-secondary)',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                padding: '14px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <span style={{ fontWeight: 600 }}>{name}</span>
                <span style={{
                  background: 'rgba(0,180,216,0.15)',
                  color: '#00b4d8',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  fontWeight: 800
                }}>
                  {count} حالة
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
