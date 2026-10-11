import React, { useState } from 'react';
import { MessageSquare, Save, Clock, Info, Check } from 'lucide-react';
import type { WhatsAppTemplate, ClinicSettings, WhatsAppCategory } from '../types';
import { saveTemplate, saveSettings } from '../services/clinicService';

interface Props {
  templates: WhatsAppTemplate[];
  settings: ClinicSettings;
}

export const TemplatesView: React.FC<Props> = ({ templates, settings }) => {
  const [editedTemplates, setEditedTemplates] = useState<Record<string, string>>({});
  const [workStart, setWorkStart] = useState(settings.workStartHour);
  const [workEnd, setWorkEnd] = useState(settings.workEndHour);
  const [dayOff, setDayOff] = useState(settings.dayOff);
  const [savedSuccess, setSavedSuccess] = useState<string | null>(null);

  const categories: { key: WhatsAppCategory; title: string; defaultText: string }[] = [
    {
      key: 'CONFIRMATION',
      title: 'رسالة تأكيد الحجز',
      defaultText: 'مرحباً {اسم المريض}، تم تأكيد حجز موعدك في {اسم العيادة} يوم {تاريخ الموعد} الساعة {وقت الموعد}. نتشرف بحضورك.'
    },
    {
      key: 'BEFORE_APPOINTMENT',
      title: 'رسالة تذكير قبل الموعد',
      defaultText: 'مرحباً {اسم المريض}، نذكرك بموعدك القادم في {اسم العيادة} يوم {تاريخ الموعد} الساعة {وقت الموعد}. برجاء الحضور قبل الموعد بـ 10 دقائق.'
    },
    {
      key: 'ONE_HOUR_BEFORE',
      title: 'رسالة تذكير قبل الموعد بساعة',
      defaultText: 'مرحباً {اسم المريض} ⏰ نذكرك بموعدك في {اسم العيادة} بعد ساعة في تمام {وقت الموعد}.'
    },
    {
      key: 'AFTER_TREATMENT',
      title: 'تعليمات ما بعد العلاج',
      defaultText: 'مرحباً {اسم المريض}، نتمنى لك الشفاء العاجل. يُرجى الالتزام بالتعليمات الطبية وتناول الأدوية في مواعيدها. في حال وجود أي استفسار يسعدنا تواصلك معنا.'
    },
    {
      key: 'CANCELLATION',
      title: 'رسالة إلغاء الموعد',
      defaultText: 'مرحباً {اسم المريض}، تم إلغاء موعدك المحدد يوم {تاريخ الموعد} الساعة {وقت الموعد} بناءً على طلبك. لتحديد موعد جديد يسعدنا تواصلك معنا.'
    }
  ];

  const handleTextChange = (cat: string, text: string) => {
    setEditedTemplates(prev => ({ ...prev, [cat]: text }));
  };

  const handleSaveTemplate = async (cat: WhatsAppCategory, title: string, defaultText: string) => {
    const existing = templates.find(t => t.category === cat);
    const textToSave = editedTemplates[cat] ?? existing?.messageText ?? defaultText;

    const tpl: WhatsAppTemplate = {
      id: existing ? existing.id : Date.now(),
      title,
      category: cat,
      messageText: textToSave
    };

    try {
      await saveTemplate(tpl);
      setSavedSuccess(cat);
      setTimeout(() => setSavedSuccess(null), 3000);
    } catch (e: any) {
      alert('خطأ أثناء الحفظ: ' + e.message);
    }
  };

  const handleSaveSettings = async () => {
    try {
      await saveSettings({
        workStartHour: workStart,
        workEndHour: workEnd,
        dayOff
      });
      alert('تم حفظ إعدادات مواعيد العمل بنجاح ومزامنتها لحظياً لجميع الهواتف!');
    } catch (e: any) {
      alert('خطأ أثناء حفظ الإعدادات: ' + e.message);
    }
  };

  return (
    <div>
      {/* Clinic Working Hours Card */}
      <div style={{
        background: 'var(--bg-card)',
        backdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px',
        marginBottom: '24px'
      }}>
        <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Clock size={22} color="#00b4d8" /> مواعيد عمل العيادة ويوم الإجازة
        </h3>

        <div className="form-grid" style={{ maxWidth: '700px' }}>
          <div className="form-group">
            <label className="form-label">بداية دوام العيادة (ساعة 24)</label>
            <input
              type="number"
              className="custom-input"
              value={workStart}
              onChange={e => setWorkStart(Number(e.target.value))}
              min="0"
              max="23"
            />
          </div>

          <div className="form-group">
            <label className="form-label">نهاية دوام العيادة (ساعة 24)</label>
            <input
              type="number"
              className="custom-input"
              value={workEnd}
              onChange={e => setWorkEnd(Number(e.target.value))}
              min="1"
              max="24"
            />
          </div>

          <div className="form-group">
            <label className="form-label">يوم الإجازة الأسبوعية</label>
            <select
              className="custom-select"
              value={dayOff}
              onChange={e => setDayOff(e.target.value)}
            >
              <option value="FRIDAY">الجمعة</option>
              <option value="THURSDAY">الخميس</option>
              <option value="SATURDAY">السبت</option>
              <option value="SUNDAY">الأحد</option>
              <option value="NONE">بدون إجازة</option>
            </select>
          </div>
        </div>

        <button className="btn-primary" onClick={handleSaveSettings} style={{ marginTop: '14px' }}>
          <Save size={16} /> حفظ إعدادات العمل
        </button>
      </div>

      {/* WhatsApp Templates */}
      <div style={{
        background: 'var(--bg-card)',
        backdropFilter: 'var(--glass-blur)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '24px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MessageSquare size={22} color="#25d366" /> قوالب رسائل الواتساب (مزامنة فورية مع الأجهزة)
          </h3>
        </div>

        <div style={{
          background: 'rgba(0,180,216,0.1)',
          border: '1px solid rgba(0,180,216,0.3)',
          borderRadius: '8px',
          padding: '12px 16px',
          fontSize: '0.85rem',
          color: '#90e0ef',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <Info size={18} />
          <span>يمكنك استخدام المتغيرات التالية داخل نص الرسالة وسيتم استبدالها تلقائياً: <b>{'{اسم المريض}'}</b> و <b>{'{وقت الموعد}'}</b> و <b>{'{تاريخ الموعد}'}</b> و <b>{'{اسم العيادة}'}</b></span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {categories.map(cat => {
            const currentObj = templates.find(t => t.category === cat.key);
            const currentVal = editedTemplates[cat.key] ?? currentObj?.messageText ?? cat.defaultText;
            const isSaved = savedSuccess === cat.key;

            return (
              <div key={cat.key} style={{
                background: 'var(--bg-secondary)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                padding: '18px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                  <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'white' }}>
                    {cat.title}
                  </div>
                  <button
                    className="btn-primary"
                    style={{
                      background: isSaved ? '#10b981' : undefined,
                      padding: '6px 16px',
                      fontSize: '0.85rem'
                    }}
                    onClick={() => handleSaveTemplate(cat.key, cat.title, cat.defaultText)}
                  >
                    {isSaved ? (
                      <>
                        <Check size={16} /> تم الحفظ بنجاح!
                      </>
                    ) : (
                      <>
                        <Save size={16} /> حفظ القالب
                      </>
                    )}
                  </button>
                </div>

                <textarea
                  className="custom-input"
                  style={{ width: '100%', minHeight: '90px', resize: 'vertical' }}
                  value={currentVal}
                  onChange={e => handleTextChange(cat.key, e.target.value)}
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
