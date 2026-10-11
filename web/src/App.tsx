import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Users,
  BarChart3,
  Settings,
  Plus,
  LogOut,
  Stethoscope,
  Sun,
  Moon
} from 'lucide-react';
import type {
  Appointment,
  Patient,
  ProcedureConfig,
  WhatsAppTemplate,
  ClinicSettings,
  User as UserModel
} from './types';
import {
  subscribeAppointments,
  subscribePatients,
  subscribeProcedures,
  subscribeTemplates,
  subscribeSettings
} from './services/clinicService';
import { CalendarView } from './components/CalendarView';
import { PatientsView } from './components/PatientsView';
import { ReportsView } from './components/ReportsView';
import { TemplatesView } from './components/TemplatesView';
import { AppointmentModal } from './components/AppointmentModal';
import { LoginView } from './components/LoginView';

export const App: React.FC = () => {
  // Theme state: light vs dark
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    return (localStorage.getItem('clinic_web_theme') as 'light' | 'dark') || 'dark';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('clinic_web_theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  };

  // Auth state
  const [currentUser, setCurrentUser] = useState<UserModel | null>(() => {
    const saved = localStorage.getItem('clinic_web_user');
    if (saved) {
      try { return JSON.parse(saved); } catch { return null; }
    }
    return null;
  });

  // Tab state
  const [activeTab, setActiveTab] = useState<'CALENDAR' | 'PATIENTS' | 'REPORTS' | 'TEMPLATES'>('CALENDAR');

  // Firebase Real-time State
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [procedures, setProcedures] = useState<ProcedureConfig[]>([]);
  const [templates, setTemplates] = useState<WhatsAppTemplate[]>([]);
  const [settings, setSettings] = useState<ClinicSettings>({
    workStartHour: 14, // 2 PM
    workEndHour: 24,   // 12 AM midnight
    dayOff: 'FRIDAY'
  });

  // Booking Modal State
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
  const [editingAppointment, setEditingAppointment] = useState<Appointment | null>(null);
  const [bookingDefaultDate, setBookingDefaultDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [bookingDefaultStartTime, setBookingDefaultStartTime] = useState<string>('16:00');
  const [bookingDefaultEndTime, setBookingDefaultEndTime] = useState<string>('16:30');

  // Check role: if not ADMIN, prevent staying on TEMPLATES tab
  useEffect(() => {
    if (currentUser && currentUser.role !== 'ADMIN' && activeTab === 'TEMPLATES') {
      setActiveTab('CALENDAR');
    }
  }, [currentUser, activeTab]);

  // Subscribe to Firestore collections in real time
  useEffect(() => {
    if (!currentUser) return;

    const unsubAppointments = subscribeAppointments(setAppointments);
    const unsubPatients = subscribePatients(setPatients);
    const unsubProcedures = subscribeProcedures(setProcedures);
    const unsubTemplates = subscribeTemplates(setTemplates);
    const unsubSettings = subscribeSettings(setSettings);

    return () => {
      unsubAppointments();
      unsubPatients();
      unsubProcedures();
      unsubTemplates();
      unsubSettings();
    };
  }, [currentUser]);

  const handleLoginSuccess = (user: UserModel) => {
    setCurrentUser(user);
    localStorage.setItem('clinic_web_user', JSON.stringify(user));
  };

  const handleLogout = () => {
    localStorage.removeItem('clinic_web_user');
    setCurrentUser(null);
  };

  const openNewBooking = (dateStr: string, startTime?: string, endTime?: string) => {
    setEditingAppointment(null);
    setBookingDefaultDate(dateStr);
    if (startTime) {
      setBookingDefaultStartTime(startTime);
      if (endTime) {
        setBookingDefaultEndTime(endTime);
      } else {
        try {
          const [h, m] = startTime.split(':').map(Number);
          const total = h * 60 + m + 30;
          const endH = Math.floor(total / 60) % 24;
          const endM = total % 60;
          setBookingDefaultEndTime(`${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`);
        } catch {
          setBookingDefaultEndTime('16:30');
        }
      }
    } else {
      setBookingDefaultStartTime('16:00');
      setBookingDefaultEndTime('16:30');
    }
    setIsBookingModalOpen(true);
  };

  const handleEditAppointment = (app: Appointment) => {
    setEditingAppointment(app);
    setIsBookingModalOpen(true);
  };

  const handleBookForPatient = (_p: Patient) => {
    setEditingAppointment(null);
    setBookingDefaultDate(new Date().toISOString().split('T')[0]);
    setBookingDefaultStartTime('16:00');
    setBookingDefaultEndTime('16:30');
    setIsBookingModalOpen(true);
  };

  if (!currentUser) {
    return (
      <div data-theme={theme} style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
        <div style={{ position: 'absolute', top: 20, left: 20 }}>
          <button className="theme-toggle-btn" onClick={toggleTheme} title="تبديل الوضع">
            {theme === 'dark' ? <Sun size={18} color="#f59e0b" /> : <Moon size={18} color="#0284c7" />}
          </button>
        </div>
        <LoginView onLoginSuccess={handleLoginSuccess} />
      </div>
    );
  }

  const isAdmin = currentUser.role === 'ADMIN';

  return (
    <div className="app-layout" data-theme={theme}>
      {/* Header */}
      <header className="app-header">
        <div className="brand-section">
          <div className="brand-logo-badge">
            <Stethoscope size={24} />
          </div>
          <div>
            <div className="brand-title">Dental Calendar Portal</div>
            <div className="brand-subtitle">نظام مواعيد عيادة الأسنان - بث حي متزامن</div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="nav-pills">
          <button
            className={`nav-pill-btn ${activeTab === 'CALENDAR' ? 'active' : ''}`}
            onClick={() => setActiveTab('CALENDAR')}
          >
            <Calendar size={18} />
            <span>جدول المواعيد</span>
          </button>
          <button
            className={`nav-pill-btn ${activeTab === 'PATIENTS' ? 'active' : ''}`}
            onClick={() => setActiveTab('PATIENTS')}
          >
            <Users size={18} />
            <span>سجل المرضى</span>
          </button>
          <button
            className={`nav-pill-btn ${activeTab === 'REPORTS' ? 'active' : ''}`}
            onClick={() => setActiveTab('REPORTS')}
          >
            <BarChart3 size={18} />
            <span>التقارير والإحصائيات</span>
          </button>
          {isAdmin && (
            <button
              className={`nav-pill-btn ${activeTab === 'TEMPLATES' ? 'active' : ''}`}
              onClick={() => setActiveTab('TEMPLATES')}
            >
              <Settings size={18} />
              <span>القوالب والإعدادات</span>
            </button>
          )}
        </nav>

        {/* Actions & User & Theme Toggle */}
        <div className="header-actions">
          {/* Theme Toggle Button */}
          <button
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'التبديل إلى الوضع الفاتح (Light Mode)' : 'التبديل إلى الوضع الليلي (Dark Mode)'}
          >
            {theme === 'dark' ? <Sun size={18} color="#f59e0b" /> : <Moon size={18} color="#0284c7" />}
          </button>

          <button
            className="btn-primary"
            onClick={() => openNewBooking(new Date().toISOString().split('T')[0])}
          >
            <Plus size={18} />
            <span>حجز سريع</span>
          </button>

          <div className="user-profile-badge">
            <div className="user-avatar">
              {currentUser.name.charAt(0)}
            </div>
            <div className="user-details">
              <span className="user-name">{currentUser.name}</span>
              <span className="user-role-tag">{isAdmin ? 'مدير النظام (أدمن)' : 'طاقم العمل'}</span>
            </div>
          </div>

          <button className="btn-secondary" onClick={handleLogout} title="تسجيل الخروج">
            <LogOut size={16} />
          </button>
        </div>
      </header>

      {/* Main Views */}
      <main className="main-content">
        {activeTab === 'CALENDAR' && (
          <CalendarView
            appointments={appointments}
            procedures={procedures}
            templates={templates}
            settings={settings}
            currentUser={currentUser}
            patients={patients}
            onOpenNewBooking={openNewBooking}
            onEditAppointment={handleEditAppointment}
          />
        )}

        {activeTab === 'PATIENTS' && (
          <PatientsView
            patients={patients}
            appointments={appointments}
            currentUser={currentUser}
            onBookForPatient={handleBookForPatient}
          />
        )}

        {activeTab === 'REPORTS' && (
          <ReportsView appointments={appointments} />
        )}

        {activeTab === 'TEMPLATES' && isAdmin && (
          <TemplatesView
            templates={templates}
            settings={settings}
          />
        )}
      </main>

      {/* Booking Modal */}
      <AppointmentModal
        isOpen={isBookingModalOpen}
        onClose={() => {
          setIsBookingModalOpen(false);
          setEditingAppointment(null);
        }}
        appointmentToEdit={editingAppointment}
        defaultDate={bookingDefaultDate}
        defaultStartTime={bookingDefaultStartTime}
        defaultEndTime={bookingDefaultEndTime}
        patients={patients}
        procedures={procedures}
        currentUser={currentUser}
      />
    </div>
  );
};

export default App;
