import React, { useState } from 'react';
import { Search, Plus, Edit, UserPlus, Phone, FileText } from 'lucide-react';
import type { Patient, Appointment, User as UserModel } from '../types';
import { PatientModal } from './PatientModal';

interface Props {
  patients: Patient[];
  appointments: Appointment[];
  onBookForPatient: (patient: Patient) => void;
  currentUser: UserModel;
}

export const PatientsView: React.FC<Props> = ({
  patients,
  appointments,
  onBookForPatient,
  currentUser
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPatient, setEditingPatient] = useState<Patient | null>(null);

  const filtered = patients.filter(p =>
    (p.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (p.phone || '').includes(searchTerm) ||
    (p.notes || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const classificationLabels: Record<string, { label: string; color: string; bg: string }> = {
    VIP: { label: '⭐ VIP', color: '#eab308', bg: 'rgba(234, 179, 8, 0.15)' },
    DISCIPLINED: { label: 'منضبط ✅', color: '#10b981', bg: 'rgba(16, 185, 129, 0.15)' },
    NORMAL: { label: 'عادي', color: '#64748b', bg: 'rgba(100, 116, 139, 0.15)' },
    FREQUENT_CANCELLER: { label: 'كثير الغياب ❌', color: '#ef4444', bg: 'rgba(239, 68, 68, 0.15)' },
    DELAYED: { label: 'متأخر غالباً ⚠️', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' }
  };

  const handleOpenAdd = () => {
    setEditingPatient(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (p: Patient) => {
    setEditingPatient(p);
    setIsModalOpen(true);
  };

  return (
    <div>
      {/* Controls Bar */}
      <div className="controls-bar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1, minWidth: '280px', maxWidth: '420px' }}>
          <Search size={20} color="var(--text-secondary)" />
          <input
            type="text"
            className="custom-input"
            style={{ width: '100%' }}
            placeholder="بحث بالاسم أو الهاتف أو الملاحظات..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{ fontWeight: 700, color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            إجمالي المرضى: <span style={{ color: 'var(--primary)', fontSize: '1.05rem', fontWeight: 800 }}>{patients.length}</span>
          </div>

          <button
            className="btn-primary"
            onClick={handleOpenAdd}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <UserPlus size={16} />
            <span>إضافة مريض جديد</span>
          </button>
        </div>
      </div>

      {/* Patients Table */}
      <div className="patients-table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>اسم المريض</th>
              <th>رقم الهاتف</th>
              <th>التصنيف والسلوك</th>
              <th>عدد المواعيد</th>
              <th>ملاحظات طبية</th>
              <th style={{ textAlign: 'center' }}>إجراءات</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '48px', color: 'var(--text-secondary)' }}>
                  {searchTerm ? 'لا يوجد مرضى مطابقين لكلمات البحث' : 'لا يوجد مرضى مسجلين حتى الآن'}
                </td>
              </tr>
            ) : (
              filtered.map(p => {
                const pApps = appointments.filter(a => a.patientId === p.id || a.patientName === p.name);
                const tagInfo = classificationLabels[p.classification] || classificationLabels.NORMAL;

                return (
                  <tr key={p.id}>
                    <td style={{ fontWeight: 800, color: 'var(--text-primary)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span>{p.name}</span>
                      </div>
                    </td>
                    <td style={{ direction: 'ltr', textAlign: 'right', fontFamily: 'monospace' }}>
                      {p.phone ? (
                        <a
                          href={`tel:${p.phone}`}
                          style={{ color: 'var(--primary)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                        >
                          <Phone size={13} />
                          {p.phone}
                        </a>
                      ) : (
                        <span style={{ color: 'var(--text-secondary)' }}>-</span>
                      )}
                    </td>
                    <td>
                      <span
                        className="app-classification-pill"
                        style={{
                          color: tagInfo.color,
                          backgroundColor: tagInfo.bg,
                          border: `1px solid ${tagInfo.color}40`,
                          fontWeight: 700
                        }}
                      >
                        {tagInfo.label}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 800, color: 'var(--primary)' }}>
                        {pApps.length}
                      </span>
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={p.notes || ''}>
                      {p.notes ? (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <FileText size={13} color="var(--primary)" />
                          {p.notes}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
                        <button
                          className="btn-secondary"
                          style={{
                            padding: '6px 12px',
                            fontSize: '0.8rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px'
                          }}
                          onClick={() => handleOpenEdit(p)}
                          title="تعديل بيانات وملف المريض"
                        >
                          <Edit size={14} color="var(--primary)" />
                          <span>تعديل</span>
                        </button>

                        <button
                          className="btn-primary"
                          style={{
                            padding: '6px 12px',
                            fontSize: '0.8rem',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px'
                          }}
                          onClick={() => onBookForPatient(p)}
                          title="حجز موعد لهذا المريض"
                        >
                          <Plus size={14} />
                          <span>حجز</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Patient Create / Edit Modal */}
      <PatientModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingPatient(null);
        }}
        patientToEdit={editingPatient}
        currentUser={currentUser}
      />
    </div>
  );
};
