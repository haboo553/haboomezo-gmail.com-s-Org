import React, { useState } from 'react';
import { ReportView } from './components/ReportView';
import { SchemaView } from './components/SchemaView';
import { CodeExplorerView } from './components/CodeExplorerView';
import { LiveEngineView } from './components/LiveEngineView';
import { FileText, Database, Code2, PlayCircle, Smartphone, CheckCircle } from 'lucide-react';

export function App() {
  const [activeSection, setActiveSection] = useState<'report' | 'schema' | 'code' | 'sandbox'>('report');

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900 font-sans antialiased" dir="rtl">
      {/* Top Application Header */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40 shadow-2xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-sky-600 text-white flex items-center justify-center shadow-xs">
                <Smartphone className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-base font-extrabold text-slate-900 leading-tight">
                    ماركتي | Markety
                  </h1>
                  <span className="px-2 py-0.5 text-[11px] font-bold bg-emerald-100 text-emerald-800 rounded-full border border-emerald-200">
                    Android Native
                  </span>
                </div>
                <p className="text-xs text-slate-500">
                  نظام إدارة سوبر ماركت مصري | Kotlin • Jetpack Compose • Room • SQLite
                </p>
              </div>
            </div>

            {/* Navigation Tabs */}
            <nav className="flex items-center gap-1 sm:gap-2">
              <button
                onClick={() => setActiveSection('report')}
                className={`px-3 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                  activeSection === 'report'
                    ? 'bg-sky-50 text-sky-700 shadow-2xs'
                    : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <FileText className="w-4 h-4" />
                <span className="hidden sm:inline">تقرير المرحلة الأولى</span>
                <span className="sm:hidden">التقرير</span>
              </button>

              <button
                onClick={() => setActiveSection('schema')}
                className={`px-3 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                  activeSection === 'schema'
                    ? 'bg-sky-50 text-sky-700 shadow-2xs'
                    : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <Database className="w-4 h-4" />
                <span className="hidden sm:inline">مخطط SQLite و Room</span>
                <span className="sm:hidden">الجداول</span>
              </button>

              <button
                onClick={() => setActiveSection('code')}
                className={`px-3 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                  activeSection === 'code'
                    ? 'bg-sky-50 text-sky-700 shadow-2xs'
                    : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <Code2 className="w-4 h-4" />
                <span className="hidden sm:inline">ملفات الكود (Kotlin)</span>
                <span className="sm:hidden">الكود</span>
              </button>

              <button
                onClick={() => setActiveSection('sandbox')}
                className={`px-3 py-2 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                  activeSection === 'sandbox'
                    ? 'bg-sky-600 text-white shadow-2xs'
                    : 'bg-slate-900 text-white hover:bg-slate-800'
                }`}
              >
                <PlayCircle className="w-4 h-4" />
                <span>تجربة المحرك الحي</span>
              </button>
            </nav>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeSection === 'report' && <ReportView />}
        {activeSection === 'schema' && <SchemaView />}
        {activeSection === 'code' && <CodeExplorerView />}
        {activeSection === 'sandbox' && <LiveEngineView />}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-6 mt-12 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-600" />
            <span>ماركتي | Markety — المرحلة الأولى مكتملة بنجاح وفقاً للمواصفات الصارمة</span>
          </div>
          <div>
            <span>توقف الآن بانتظار طلب المرحلة الثانية</span>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
