import React, { useState } from 'react';
import { DailyOperation, Expense, SalesInvoice } from '../types';
import {
  Lock,
  LockOpen,
  Calendar,
  DollarSign,
  TrendingDown,
  TrendingUp,
  CreditCard,
  Receipt,
  AlertTriangle,
  CheckCircle2,
  Plus,
  ArrowRight,
  ShieldAlert,
  Wallet,
  Coins
} from 'lucide-react';

interface LiveDailyOperationsViewProps {
  invoices: SalesInvoice[];
  dailyOperations: DailyOperation[];
  setDailyOperations: React.Dispatch<React.SetStateAction<DailyOperation[]>>;
  expenses: Expense[];
  setExpenses: React.Dispatch<React.SetStateAction<Expense[]>>;
}

const DEFAULT_EXPENSE_CATEGORIES = [
  'فواتير ومرافق',
  'إيجار المحل',
  'عمالة ومرتبات',
  'وجبات وضيافة',
  'نقل وبضائع',
  'نظافة ومستلزمات',
  'صيانة وتجهيزات',
  'مصروفات أخرى'
];

export const LiveDailyOperationsView: React.FC<LiveDailyOperationsViewProps> = ({
  invoices,
  dailyOperations,
  setDailyOperations,
  expenses,
  setExpenses
}) => {
  const currentOpenOp = dailyOperations.find((op) => op.status === 'OPEN');

  // Open day form state
  const [openingCashInput, setOpeningCashInput] = useState('');
  const [openNotesInput, setOpenNotesInput] = useState('');

  // Close day form state
  const [actualCashInput, setActualCashInput] = useState('');
  const [closeNotesInput, setCloseNotesInput] = useState('');
  const [showConfirmCloseModal, setShowConfirmCloseModal] = useState(false);
  const [closedSummary, setClosedSummary] = useState<DailyOperation | null>(null);

  // Expense form state
  const [showAddExpenseModal, setShowAddExpenseModal] = useState(false);
  const [expenseCategory, setExpenseCategory] = useState(DEFAULT_EXPENSE_CATEGORIES[0]);
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expenseNotes, setExpenseNotes] = useState('');

  // Notifications
  const [alertMessage, setAlertMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const showAlert = (text: string, type: 'success' | 'error' = 'success') => {
    setAlertMessage({ text, type });
    setTimeout(() => setAlertMessage(null), 4500);
  };

  // Compute live day metrics for open operation
  const liveMetrics = React.useMemo(() => {
    if (!currentOpenOp) {
      return {
        totalSales: 0,
        cashSales: 0,
        creditSales: 0,
        invoicesCount: 0,
        totalExpenses: 0,
        netSales: 0,
        expectedCash: 0
      };
    }

    // Invoices made during this shift (from openedAt onwards)
    const shiftInvoices = invoices.filter(
      (inv) => inv.status === 'COMPLETED' && inv.dateTime >= currentOpenOp.openedAt
    );
    const totalSales = shiftInvoices.reduce((sum, inv) => sum + inv.total, 0);
    const cashSales = shiftInvoices.reduce((sum, inv) => sum + inv.paidAmount, 0);
    const creditSales = shiftInvoices.reduce((sum, inv) => sum + inv.remainingAmount, 0);
    const invoicesCount = shiftInvoices.length;

    const opExpenses = expenses.filter((e) => e.dailyOperationId === currentOpenOp.id);
    const totalExpenses = opExpenses.reduce((sum, e) => sum + e.amount, 0);

    const netSales = totalSales - totalExpenses;
    const expectedCash = currentOpenOp.openingCash + cashSales - totalExpenses;

    return {
      totalSales,
      cashSales,
      creditSales,
      invoicesCount,
      totalExpenses,
      netSales,
      expectedCash
    };
  }, [currentOpenOp, invoices, expenses]);

  // Actual vs expected difference
  const actualCash = parseFloat(actualCashInput) || 0;
  const liveDifference = actualCashInput.trim() !== '' ? actualCash - liveMetrics.expectedCash : 0;

  // Handlers
  const handleOpenDay = (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(openingCashInput);
    if (isNaN(amount) || amount < 0) {
      showAlert('يرجى إدخال رصيد بداية صحيح (صفر أو أكثر).', 'error');
      return;
    }

    if (currentOpenOp) {
      showAlert(`يوجد يوم تشغيل مفتوح بالفعل (#${currentOpenOp.id}). يجب إغلاقه أولاً.`, 'error');
      return;
    }

    const todayStr = new Date().toISOString().split('T')[0];
    const newOp: DailyOperation = {
      id: Date.now(),
      date: todayStr,
      openingCash: amount,
      totalSales: 0,
      cashSales: 0,
      creditSales: 0,
      expenses: 0,
      netSales: 0,
      closingCash: amount,
      actualCash: 0,
      difference: 0,
      status: 'OPEN',
      openedAt: Date.now(),
      notes: openNotesInput.trim() || undefined
    };

    setDailyOperations((prev) => [newOp, ...prev]);
    setOpeningCashInput('');
    setOpenNotesInput('');
    showAlert(`تم فتح يوم التشغيل بنجاح برصيد بداية ${amount.toFixed(2)} ج.م`);
  };

  const handleAddExpense = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOpenOp) {
      showAlert('لا يمكن تسجيل مصروف بدون وجود يوم تشغيل مفتوح.', 'error');
      return;
    }

    const amount = parseFloat(expenseAmount);
    if (isNaN(amount) || amount <= 0) {
      showAlert('قيمة المصروف يجب أن تكون أكبر من صفر.', 'error');
      return;
    }

    const newExpense: Expense = {
      id: Date.now(),
      dailyOperationId: currentOpenOp.id,
      category: expenseCategory,
      amount: amount,
      notes: expenseNotes.trim() || undefined,
      date: Date.now(),
      createdAt: Date.now()
    };

    setExpenses((prev) => [newExpense, ...prev]);
    setShowAddExpenseModal(false);
    setExpenseAmount('');
    setExpenseNotes('');
    showAlert(`تم تسجيل المصروف بقيمة ${amount.toFixed(2)} ج.م وربطه باليوم المفتوح بنجاح.`);
  };

  const handleExecuteCloseDay = () => {
    if (!currentOpenOp) return;
    if (actualCashInput.trim() === '' || isNaN(actualCash) || actualCash < 0) {
      showAlert('يرجى إدخال النقدية الفعلية في الدرج بشكل صحيح قبل الإغلاق.', 'error');
      return;
    }

    // Atomic room transaction simulation
    const finalDiff = actualCash - liveMetrics.expectedCash;

    const closedOp: DailyOperation = {
      ...currentOpenOp,
      totalSales: liveMetrics.totalSales,
      cashSales: liveMetrics.cashSales,
      creditSales: liveMetrics.creditSales,
      expenses: liveMetrics.totalExpenses,
      netSales: liveMetrics.netSales,
      closingCash: liveMetrics.expectedCash,
      actualCash: actualCash,
      difference: finalDiff,
      status: 'CLOSED',
      closedAt: Date.now(),
      notes: closeNotesInput.trim() || currentOpenOp.notes
    };

    setDailyOperations((prev) => prev.map((op) => (op.id === currentOpenOp.id ? closedOp : op)));
    setClosedSummary(closedOp);
    setShowConfirmCloseModal(false);
    setActualCashInput('');
    setCloseNotesInput('');
    showAlert('تم إغلاق يوم التشغيل بنجاح وقفل العمليات نهائياً.', 'success');
  };

  const currentOpExpenses = currentOpenOp
    ? expenses.filter((e) => e.dailyOperationId === currentOpenOp.id)
    : [];

  return (
    <div className="space-y-6" dir="rtl">
      {/* Alert toast */}
      {alertMessage && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between shadow-md transition-all ${
            alertMessage.type === 'error'
              ? 'bg-rose-50 text-rose-800 border border-rose-200'
              : 'bg-emerald-50 text-emerald-800 border border-emerald-200'
          }`}
        >
          <div className="flex items-center gap-2">
            {alertMessage.type === 'error' ? (
              <AlertTriangle className="w-5 h-5 text-rose-600" />
            ) : (
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            )}
            <span className="text-sm font-semibold">{alertMessage.text}</span>
          </div>
        </div>
      )}

      {/* Closed Summary Banner if just closed */}
      {closedSummary && (
        <div className="bg-slate-900 text-white p-6 rounded-2xl border border-slate-700 shadow-xl space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-6 h-6 text-emerald-400" />
              <h3 className="text-lg font-extrabold">
                ملخص اليوم المغلق (#{closedSummary.id.toString().slice(-4)}) - {closedSummary.date}
              </h3>
            </div>
            <button
              onClick={() => setClosedSummary(null)}
              className="text-xs bg-slate-800 hover:bg-slate-700 px-3 py-1.5 rounded-lg font-medium"
            >
              إخفاء
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">رصيد البداية</span>
              <span className="text-base font-bold text-white">
                {closedSummary.openingCash.toFixed(2)} ج.م
              </span>
            </div>
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">إجمالي المبيعات</span>
              <span className="text-base font-bold text-sky-400">
                {closedSummary.totalSales.toFixed(2)} ج.م
              </span>
            </div>
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">المصروفات</span>
              <span className="text-base font-bold text-rose-400">
                {closedSummary.expenses.toFixed(2)} ج.م
              </span>
            </div>
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">صافي المبيعات</span>
              <span className="text-base font-bold text-emerald-400">
                {closedSummary.netSales.toFixed(2)} ج.م
              </span>
            </div>
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">المتوقع في الخزينة</span>
              <span className="text-base font-bold text-amber-300">
                {closedSummary.closingCash.toFixed(2)} ج.م
              </span>
            </div>
            <div className="bg-slate-800/80 p-3 rounded-xl">
              <span className="text-slate-400 text-xs block">الفعلي المدخل</span>
              <span className="text-base font-bold text-white">
                {closedSummary.actualCash.toFixed(2)} ج.م
              </span>
            </div>
            <div className="col-span-2 bg-slate-800/80 p-3 rounded-xl flex items-center justify-between">
              <div>
                <span className="text-slate-400 text-xs block">الفرق (عجز / فائض)</span>
                <span
                  className={`text-lg font-black ${
                    closedSummary.difference >= 0 ? 'text-emerald-400' : 'text-rose-400'
                  }`}
                >
                  {closedSummary.difference >= 0 ? '+' : ''}
                  {closedSummary.difference.toFixed(2)} ج.م
                </span>
              </div>
              <span className="text-xs px-2.5 py-1 rounded-full bg-slate-700 text-slate-300 font-bold">
                محفوظ في SQLite (CLOSED)
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Main Mode: Open Day OR Active Shift */}
      {!currentOpenOp ? (
        // STATE 1: NO OPEN DAY -> Form to Open New Day
        <div className="bg-white rounded-2xl border border-slate-200 p-8 shadow-xs max-w-2xl mx-auto space-y-6">
          <div className="flex items-center gap-3 border-b border-slate-100 pb-4">
            <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
              <Lock className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg font-black text-slate-900">فتح يوم تشغيل جديد</h2>
              <p className="text-xs text-slate-500">
                يوم التشغيل مغلق حالياً. يرجى تسجيل رصيد البداية في الخزينة لافتتاح الوردية.
              </p>
            </div>
          </div>

          <form onSubmit={handleOpenDay} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">
                رصيد البداية في الخزينة (ج.م) *
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  required
                  value={openingCashInput}
                  onChange={(e) => setOpeningCashInput(e.target.value)}
                  placeholder="مثال: 500"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 text-base font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500 pr-10"
                />
                <DollarSign className="w-5 h-5 text-slate-400 absolute left-3 top-3.5" />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">
                ملاحظات الافتتاح (اختياري)
              </label>
              <input
                type="text"
                value={openNotesInput}
                onChange={(e) => setOpenNotesInput(e.target.value)}
                placeholder="مثال: وردية الصباح - استلام كاشير 1"
                className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>

            <button
              type="submit"
              className="w-full bg-sky-600 hover:bg-sky-700 text-white font-bold py-3 rounded-xl flex items-center justify-center gap-2 shadow-sm transition"
            >
              <LockOpen className="w-5 h-5" />
              <span>تأكيد وفتح يوم التشغيل</span>
            </button>
          </form>
        </div>
      ) : (
        // STATE 2: DAY IS OPEN -> Dashboard, Financial Calculations, Expenses, Actual Cash & Closing
        <div className="space-y-6">
          {/* Header Banner */}
          <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-extrabold text-emerald-900">
                    يوم التشغيل مفتوح (وردية نشطة)
                  </h2>
                  <span className="text-xs bg-emerald-200/70 text-emerald-800 px-2 py-0.5 rounded-md font-bold">
                    #{currentOpenOp.id.toString().slice(-4)}
                  </span>
                </div>
                <p className="text-xs text-emerald-700 mt-0.5">
                  التاريخ: {currentOpenOp.date} | وقت الفتح:{' '}
                  {new Date(currentOpenOp.openedAt).toLocaleTimeString('ar-EG')}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowAddExpenseModal(true)}
                className="bg-rose-600 hover:bg-rose-700 text-white px-3.5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition"
              >
                <Plus className="w-4 h-4" />
                <span>إضافة مصروف</span>
              </button>
              <button
                onClick={() => setShowConfirmCloseModal(true)}
                className="bg-slate-900 hover:bg-slate-800 text-white px-3.5 py-2 rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition"
              >
                <Lock className="w-4 h-4" />
                <span>إغلاق اليوم</span>
              </button>
            </div>
          </div>

          {/* Real Financial Cards Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-slate-500 block mb-1">رصيد البداية</span>
              <span className="text-lg font-black text-slate-800">
                {currentOpenOp.openingCash.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">افتتاح الخزينة</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-sky-600 block mb-1">مبيعات اليوم</span>
              <span className="text-lg font-black text-sky-700">
                {liveMetrics.totalSales.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">
                {liveMetrics.invoicesCount} فاتورة بيع
              </span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-emerald-600 block mb-1">مبيعات كاش</span>
              <span className="text-lg font-black text-emerald-700">
                {liveMetrics.cashSales.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">نقدية محصلة</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-amber-600 block mb-1">مبيعات آجل</span>
              <span className="text-lg font-black text-amber-700">
                {liveMetrics.creditSales.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">متبقي على الفواتير</span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-rose-600 block mb-1">المصروفات</span>
              <span className="text-lg font-black text-rose-700">
                {liveMetrics.totalExpenses.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">
                {currentOpExpenses.length} حركة نثريات
              </span>
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
              <span className="text-xs font-bold text-purple-600 block mb-1">صافي المبيعات</span>
              <span className="text-lg font-black text-purple-700">
                {liveMetrics.netSales.toFixed(2)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-1">المبيعات - المصروفات</span>
            </div>
          </div>

          {/* Expected Cash in Register Formula Card */}
          <div className="bg-gradient-to-l from-sky-900 to-slate-900 text-white p-5 rounded-2xl shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Wallet className="w-5 h-5 text-sky-400" />
                <h3 className="text-sm font-bold text-sky-200">الرصيد المتوقع في الخزينة الآن</h3>
              </div>
              <p className="text-xs text-slate-300">
                المعادلة: رصيد البداية ({currentOpenOp.openingCash.toFixed(1)}) + الكاش المحصل (
                {liveMetrics.cashSales.toFixed(1)}) - المصروفات ({liveMetrics.totalExpenses.toFixed(1)})
              </p>
            </div>
            <div className="text-left sm:text-right">
              <span className="text-3xl font-black text-white tracking-tight">
                {liveMetrics.expectedCash.toFixed(2)}
              </span>
              <span className="text-sm font-bold text-sky-300 mr-1.5">ج.م</span>
            </div>
          </div>

          {/* Grid of (Expenses List) and (Day Closing Form) */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Section 1: Expenses in Current Shift */}
            <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <TrendingDown className="w-5 h-5 text-rose-600" />
                  <h3 className="text-sm font-bold text-slate-900">مصروفات الوردية الحالية</h3>
                </div>
                <button
                  onClick={() => setShowAddExpenseModal(true)}
                  className="text-xs bg-rose-50 text-rose-700 hover:bg-rose-100 px-3 py-1.5 rounded-lg font-bold flex items-center gap-1 transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>إضافة</span>
                </button>
              </div>

              {currentOpExpenses.length === 0 ? (
                <div className="text-center py-8 text-slate-400 text-xs">
                  لا توجد مصروفات مسجلة في هذه الوردية حتى الآن.
                </div>
              ) : (
                <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                  {currentOpExpenses.map((exp) => (
                    <div
                      key={exp.id}
                      className="p-3 bg-slate-50 hover:bg-slate-100/80 rounded-xl border border-slate-200/70 flex items-center justify-between text-xs transition"
                    >
                      <div>
                        <div className="font-bold text-slate-800">{exp.category}</div>
                        {exp.notes && <div className="text-slate-500 mt-0.5">{exp.notes}</div>}
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          {new Date(exp.date).toLocaleTimeString('ar-EG')}
                        </div>
                      </div>
                      <div className="text-sm font-black text-rose-600">
                        -{exp.amount.toFixed(2)} ج.م
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Section 2: Cash Reconciliation & Closing */}
            <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
                <Coins className="w-5 h-5 text-sky-600" />
                <h3 className="text-sm font-bold text-slate-900">
                  تسوية النقدية الفعلية وإغلاق الوردية
                </h3>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    النقدية الفعلية في الدرج (ج.م) *
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    value={actualCashInput}
                    onChange={(e) => setActualCashInput(e.target.value)}
                    placeholder="أدخل المبلغ بعد عده يدوياً"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-base font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>

                {/* Auto Difference Badge */}
                {actualCashInput.trim() !== '' && (
                  <div
                    className={`p-3 rounded-xl border flex items-center justify-between text-xs font-bold ${
                      Math.abs(liveDifference) < 0.01
                        ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                        : liveDifference > 0
                        ? 'bg-sky-50 text-sky-800 border-sky-200'
                        : 'bg-rose-50 text-rose-800 border-rose-200'
                    }`}
                  >
                    <span>
                      {Math.abs(liveDifference) < 0.01
                        ? 'الخزينة مطابقة تماماً (بدون فرق)'
                        : liveDifference > 0
                        ? 'يوجد فائض في الخزينة (زيادة)'
                        : 'يوجد عجز في الخزينة (نقص)'}
                    </span>
                    <span className="text-sm font-black">
                      {liveDifference >= 0 ? '+' : ''}
                      {liveDifference.toFixed(2)} ج.م
                    </span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    ملاحظات الإغلاق (اختياري)
                  </label>
                  <input
                    type="text"
                    value={closeNotesInput}
                    onChange={(e) => setCloseNotesInput(e.target.value)}
                    placeholder="سبب العجز/الزيادة أو ملاحظات التسليم"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>

                <button
                  type="button"
                  onClick={() => setShowConfirmCloseModal(true)}
                  disabled={actualCashInput.trim() === ''}
                  className="w-full bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white font-bold py-3 rounded-xl text-xs flex items-center justify-center gap-1.5 shadow-xs transition"
                >
                  <Lock className="w-4 h-4" />
                  <span>إغلاق يوم التشغيل وحفظ الحسابات في SQLite</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* History of Closed Operations */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-4">
        <h3 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
          سجل أيام التشغيل السابقة
        </h3>

        {dailyOperations.length === 0 ? (
          <div className="text-center py-6 text-slate-400 text-xs">
            لا توجد أيام تشغيل مسجلة في قاعدة البيانات.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
                <tr>
                  <th className="p-3">#</th>
                  <th className="p-3">التاريخ</th>
                  <th className="p-3">رصيد البداية</th>
                  <th className="p-3">المبيعات</th>
                  <th className="p-3">المصروفات</th>
                  <th className="p-3">الصافي</th>
                  <th className="p-3">المتوقع</th>
                  <th className="p-3">الفعلي</th>
                  <th className="p-3">الفرق</th>
                  <th className="p-3">الحالة</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {dailyOperations.map((op) => (
                  <tr key={op.id} className="hover:bg-slate-50/70 transition">
                    <td className="p-3 font-bold text-slate-700">#{op.id.toString().slice(-4)}</td>
                    <td className="p-3">{op.date}</td>
                    <td className="p-3">{op.openingCash.toFixed(2)}</td>
                    <td className="p-3 font-bold text-sky-700">{op.totalSales.toFixed(2)}</td>
                    <td className="p-3 text-rose-600">-{op.expenses.toFixed(2)}</td>
                    <td className="p-3 font-bold text-emerald-700">{op.netSales.toFixed(2)}</td>
                    <td className="p-3">{op.closingCash.toFixed(2)}</td>
                    <td className="p-3 font-bold">{op.actualCash.toFixed(2)}</td>
                    <td
                      className={`p-3 font-bold ${
                        op.difference >= 0 ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {op.difference >= 0 ? '+' : ''}
                      {op.difference.toFixed(2)}
                    </td>
                    <td className="p-3">
                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          op.status === 'OPEN'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {op.status === 'OPEN' ? 'مفتوح' : 'مغلق'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* MODAL 1: Add Expense */}
      {showAddExpenseModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-base font-extrabold text-slate-900">تسجيل مصروف جديد</h3>
              <button
                onClick={() => setShowAddExpenseModal(false)}
                className="text-slate-400 hover:text-slate-600 text-xs font-bold"
              >
                إلغاء
              </button>
            </div>

            <form onSubmit={handleAddExpense} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">التصنيف *</label>
                <select
                  value={expenseCategory}
                  onChange={(e) => setExpenseCategory(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-xs text-slate-800 font-bold focus:outline-none focus:ring-2 focus:ring-sky-500"
                >
                  {DEFAULT_EXPENSE_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  قيمة المصروف (ج.م) *
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="0.5"
                  required
                  value={expenseAmount}
                  onChange={(e) => setExpenseAmount(e.target.value)}
                  placeholder="مثال: 45.0"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2.5 text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  ملاحظات (اختياري)
                </label>
                <input
                  type="text"
                  value={expenseNotes}
                  onChange={(e) => setExpenseNotes(e.target.value)}
                  placeholder="مثال: فاتورة كهرباء أو أدوات نظافة"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl px-3.5 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddExpenseModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-xs"
                >
                  حفظ المصروف
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: Confirm Day Closing */}
      {showConfirmCloseModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="flex items-center gap-3 border-b border-slate-100 pb-3 text-rose-600">
              <ShieldAlert className="w-6 h-6" />
              <h3 className="text-base font-extrabold text-slate-900">تأكيد إغلاق يوم التشغيل</h3>
            </div>

            <div className="space-y-2 text-xs text-slate-600">
              <p>هل أنت متأكد من رغبتك في إغلاق اليوم؟</p>
              <ul className="list-disc list-inside space-y-1 text-slate-500">
                <li>سيتم حساب كافة المبيعات والمصروفات من قاعدة البيانات وتثبيت النتيجة.</li>
                <li>سيتم قفل اليوم نهائياً ولن يُسمح بأي تعديل أو إضافة مصروفات أو بيع على هذه الوردية.</li>
                <li>النقدية الفعلية المدخلة: <span className="font-bold text-slate-800">{actualCashInput} ج.م</span></li>
                <li>الفرق النهائي: <span className="font-bold text-slate-800">{liveDifference.toFixed(2)} ج.م</span></li>
              </ul>
            </div>

            <div className="pt-3 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowConfirmCloseModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                رجوع
              </button>
              <button
                type="button"
                onClick={handleExecuteCloseDay}
                className="px-5 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-xs"
              >
                تأكيد وإغلاق اليوم نهائياً
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
