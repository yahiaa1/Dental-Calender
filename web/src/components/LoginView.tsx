import React, { useState } from 'react';
import { Lock, User, LogIn, Stethoscope } from 'lucide-react';
import type { User as UserModel } from '../types';
import { fetchUsers } from '../services/clinicService';

interface Props {
  onLoginSuccess: (user: UserModel) => void;
}

export const LoginView: React.FC<Props> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('1234655');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg('');

    try {
      // 1. Check fixed default admin: admin / 1234655
      if (username.trim() === 'admin' && password === '1234655') {
        const adminUser: UserModel = {
          id: 1,
          name: 'د. محمد (أدمن)',
          phone: '01000000001',
          role: 'ADMIN',
          username: 'admin'
        };
        onLoginSuccess(adminUser);
        return;
      }

      // 2. Fetch users from Firestore
      const users = await fetchUsers();
      const matched = users.find(
        u => u.username.toLowerCase() === username.trim().toLowerCase() && u.password === password
      );

      if (matched) {
        onLoginSuccess(matched);
      } else {
        setErrorMsg('اسم المستخدم أو كلمة المرور غير صحيحة');
      }
    } catch (e: any) {
      setErrorMsg('خطأ أثناء تسجيل الدخول: ' + e.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, #00b4d8, #0077b6)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            boxShadow: '0 8px 24px rgba(0,180,216,0.3)',
            marginBottom: '16px'
          }}>
            <Stethoscope size={32} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>تسجيل الدخول للعيادة</h2>
          <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginTop: '6px' }}>
            Dental Clinic Web Portal - كمبيوتر ولابتوب
          </p>
        </div>

        {errorMsg && (
          <div style={{
            background: 'rgba(239,68,68,0.15)',
            border: '1px solid rgba(239,68,68,0.3)',
            color: '#f87171',
            borderRadius: '8px',
            padding: '10px 14px',
            fontSize: '0.85rem',
            marginBottom: '18px',
            textAlign: 'center'
          }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label className="form-label">
              <User size={15} style={{ display: 'inline', marginLeft: 4 }} /> اسم المستخدم
            </label>
            <input
              type="text"
              className="custom-input"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="admin"
              required
            />
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label">
              <Lock size={15} style={{ display: 'inline', marginLeft: 4 }} /> كلمة المرور
            </label>
            <input
              type="password"
              className="custom-input"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          <button
            type="submit"
            className="btn-primary"
            style={{ width: '100%', justifyContent: 'center', padding: '12px' }}
            disabled={isLoading}
          >
            <LogIn size={18} />
            {isLoading ? 'جاري التحقق...' : 'دخول'}
          </button>
        </form>
      </div>
    </div>
  );
};
