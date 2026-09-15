import React, { useState } from 'react';
import { ANDROID_FILES } from '../data/androidProjectFiles';
import { Copy, Check, FileCode, Folder } from 'lucide-react';

export const CodeExplorerView: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState(ANDROID_FILES[0]);
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6 text-right" dir="rtl">
      <div>
        <h2 className="text-xl font-bold text-slate-900 mb-1">مستعرض ملفات Kotlin و Compose و Room</h2>
        <p className="text-sm text-slate-600">
          استعرض الملفات البرمجية الأصلية التي تم إنشاؤها في مسار المشروع (<code className="font-mono text-xs bg-slate-100 px-1 py-0.5 rounded">android/</code>) جاهزة للنسخ أو التصدير.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* File Tree List */}
        <div className="lg:col-span-4 bg-white rounded-xl border border-slate-200 p-3 shadow-xs space-y-2">
          <div className="text-xs font-bold text-slate-500 uppercase px-2 py-1">
            ملفات المشروع ({ANDROID_FILES.length})
          </div>
          <div className="space-y-1 max-h-[600px] overflow-y-auto">
            {ANDROID_FILES.map((f, i) => {
              const isSelected = selectedFile.path === f.path;
              const fileName = f.path.split('/').pop();
              return (
                <button
                  key={i}
                  onClick={() => setSelectedFile(f)}
                  className={`w-full text-right p-2.5 rounded-lg text-xs font-mono transition-colors flex items-center justify-between gap-2 ${
                    isSelected
                      ? 'bg-sky-50 text-sky-900 font-bold border border-sky-200'
                      : 'hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <FileCode className={`w-4 h-4 shrink-0 ${isSelected ? 'text-sky-600' : 'text-slate-400'}`} />
                    <span className="truncate">{fileName}</span>
                  </div>
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 shrink-0 font-sans">
                    {f.category}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Code Viewer */}
        <div className="lg:col-span-8 bg-slate-950 text-slate-100 rounded-xl border border-slate-800 overflow-hidden shadow-md">
          <div className="bg-slate-900 px-4 py-3 border-b border-slate-800 flex items-center justify-between text-xs" dir="ltr">
            <div className="flex items-center gap-2 font-mono text-slate-300">
              <Folder className="w-4 h-4 text-sky-400" />
              <span className="font-semibold">{selectedFile.path}</span>
            </div>
            <button
              onClick={handleCopy}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs transition"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-400" />
                  <span className="text-emerald-400 font-medium">تم النسخ</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>نسخ الكود</span>
                </>
              )}
            </button>
          </div>

          <div className="p-3 bg-slate-900/50 border-b border-slate-800/80 text-xs text-slate-400" dir="rtl">
            <span className="text-sky-300 font-medium">الوصف:</span> {selectedFile.description}
          </div>

          <div className="p-4 overflow-x-auto max-h-[520px] font-mono text-xs leading-relaxed text-slate-200" dir="ltr">
            <pre>
              <code>{selectedFile.code}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
