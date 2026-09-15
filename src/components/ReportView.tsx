import React, { useState } from 'react';
import {
  CheckCircle2,
  Camera,
  Layers,
  Receipt,
  ShoppingCart,
  ShieldCheck,
  Zap,
  HardDrive,
  AlertTriangle,
  FileCode,
  Smartphone,
  CreditCard
} from 'lucide-react';

export const ReportView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'phase3' | 'phase2' | 'phase1'>('phase3');

  return (
    <div className="space-y-8 text-right" dir="rtl">
      {/* Header Banner */}
      <div className="bg-gradient-to-l from-slate-900 via-sky-950 to-indigo-950 text-white p-6 md:p-8 rounded-2xl shadow-sm border border-slate-800">
        <div className="flex flex-wrap items-center justify-between gap-4 mb-3">
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 text-xs font-bold rounded-full border border-emerald-400/30">
              المرحلة الثالثة: نظام المبيعات ونقطة البيع (POS)
            </span>
            <span className="px-2.5 py-0.5 bg-sky-500/20 text-sky-300 text-[11px] font-semibold rounded-full border border-sky-400/30">
              Android Native • Room SQLite
            </span>
          </div>
          <span className="text-xs text-slate-400 font-mono">
            Atomic Transactions • Auto Stock Deduction • Price Snapshot
          </span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold mb-2">
          ماركتي | Markety — تقرير إنجاز المرحلة الثالثة
        </h1>
        <p className="text-slate-300 text-sm leading-relaxed max-w-3xl">
          تم بحمد الله بناء نظام المبيعات ونقطة البيع الكاملة (POS) لتطبيق «ماركتي»، مع الحفاظ التام والصلب على جميع وظائف المرحلتين الأولى والثانية دون كسر أي ميزة. تم تنفيذ المعاملات الذرية (Database Transactions)، وخصم المخزون التلقائي، ولقطة الأسعار الثابتة (Price Snapshots)، ودعم البيع الكاش والآجل، وربط لوحة التحكم (Dashboard) بأرقام المبيعات الحقيقية.
        </p>

        {/* Phase Toggle Tabs */}
        <div className="flex flex-wrap gap-2 mt-6">
          <button
            onClick={() => setActiveTab('phase3')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
              activeTab === 'phase3'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <Receipt className="w-4 h-4" />
            تقرير المرحلة الثالثة (نظام المبيعات والكاشير)
          </button>
          <button
            onClick={() => setActiveTab('phase2')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
              activeTab === 'phase2'
                ? 'bg-sky-600 text-white shadow-xs'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <Camera className="w-4 h-4" />
            المرحلة الثانية (الكاميرا والباركود)
          </button>
          <button
            onClick={() => setActiveTab('phase1')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
              activeTab === 'phase1'
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'bg-slate-800/80 text-slate-300 hover:bg-slate-800'
            }`}
          >
            <Layers className="w-4 h-4" />
            المرحلة الأولى (الأصناف والمخزون)
          </button>
        </div>
      </div>

      {activeTab === 'phase3' && (
        <div className="space-y-6">
          {/* 1. ما تم إنجازه */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">1. ما تم إنجازه في المرحلة الثالثة</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-slate-700">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="font-bold text-slate-900 flex items-center gap-2">
                  <ShoppingCart className="w-4 h-4 text-emerald-600" /> شاشة الكاشير وسلة البيع (POS)
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  واجهة بيع سريعة للمحاسب والكاشير: قراءة الباركود عبر كاميرا الهاتف الحقيقية، وإمكانية البحث باسم المنتج أو إدخال الباركود يدوياً، مع إضافة الصنف وإذا كان موجوداً بالسلة يتم زيادة كميته (+1) تلقائياً، مع أزرار التحكم في الكمية وحذف العناصر وإفراغ السلة بالكامل بعد التأكيد.
                </p>
              </div>

              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="font-bold text-slate-900 flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-sky-600" /> حسابات الفاتورة والدفع (كاش / آجل)
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  حساب دقيق لـ: الإجمالي الفرعي، الخصم المالي، الإجمالي النهائي، والمبلغ المدفوع. دعم الدفع النقدي (كاش) مع حساب الباقي للعميل تلقائياً، والدفع الآجل (شكك) مع تسجيل المبلغ المتبقي، والتحقق الصارم من أن المدفوع يغطي الفاتورة في حالة الكاش.
                </p>
              </div>

              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="font-bold text-slate-900 flex items-center gap-2">
                  <Receipt className="w-4 h-4 text-purple-600" /> سجل الفواتير وتفاصيل الفاتورة
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  شاشة كاملة لـ «سجل المبيعات» تعرض رقم الفاتورة، التاريخ والوقت، الإجمالي، المدفوع، المتبقي، ونوع الدفع. البحث برقم الفاتورة، وشاشة تفاصيل متكاملة تعرض كافة الأصناف بأسعارها وقت البيع وملخص الفاتورة المالي وملاحظاتها.
                </p>
              </div>

              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="font-bold text-slate-900 flex items-center gap-2">
                  <Zap className="w-4 h-4 text-amber-600" /> لوحة التحكم الحقيقية (Real Dashboard)
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  تم ربط لوحة التحكم بالكامل بقاعدة البيانات: إجمالي مبيعات اليوم، عدد فواتير اليوم، إجمالي الكاش المحصل، إجمالي الآجل المتبقي، وإجمالي قطع الأصناف المباعة اليوم؛ بدون أي بيانات وهمية أو محاكاة ثابتة.
                </p>
              </div>
            </div>
          </div>

          {/* 2. الجداول التي تمت إضافتها */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-purple-50 rounded-lg text-purple-600">
                <HardDrive className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">2. الجداول والكيانات المضافة (Room Entities)</h2>
            </div>
            <div className="space-y-3 text-sm text-slate-700">
              <div className="p-4 bg-purple-50/50 rounded-xl border border-purple-100">
                <div className="font-bold text-purple-900 text-sm mb-1 font-mono">1. جدول sales_invoices (SalesInvoiceEntity)</div>
                <p className="text-xs text-slate-600 mb-2">
                  يخزن الفواتير الصادرة برقم فريد:
                </p>
                <ul className="list-disc list-inside text-xs space-y-1 text-slate-600 font-mono">
                  <li><strong>id:</strong> Long (Primary Key AutoGenerate)</li>
                  <li><strong>invoiceNumber:</strong> String (UNIQUE INDEX) بصيغة MK-YYYYMMDD-XXXX</li>
                  <li><strong>dateTime:</strong> Long (طابع وقت البيع بالمللي ثانية)</li>
                  <li><strong>subtotal / discount / total:</strong> Double (حسابات الفاتورة)</li>
                  <li><strong>paidAmount / remainingAmount:</strong> Double (المدفوع والمتبقي)</li>
                  <li><strong>paymentType:</strong> PaymentType (CASH / CREDIT)</li>
                  <li><strong>status:</strong> InvoiceStatus (COMPLETED / CANCELLED)</li>
                  <li><strong>notes:</strong> String? (ملاحظات اختيارية)</li>
                </ul>
              </div>

              <div className="p-4 bg-amber-50/50 rounded-xl border border-amber-100">
                <div className="font-bold text-amber-900 text-sm mb-1 font-mono">2. جدول sale_items (SaleItemEntity)</div>
                <p className="text-xs text-slate-600 mb-2">
                  يخزن السطور التفصيلية لكل فاتورة مع الربط بالمنتج والفاتورة:
                </p>
                <ul className="list-disc list-inside text-xs space-y-1 text-slate-600 font-mono">
                  <li><strong>id:</strong> Long (Primary Key AutoGenerate)</li>
                  <li><strong>invoiceId:</strong> Long (Foreign Key -&gt; sales_invoices مع CASCADE DELETE)</li>
                  <li><strong>productId:</strong> Long (Foreign Key -&gt; products مع RESTRICT)</li>
                  <li><strong>productNameSnapshot:</strong> String (اسم الصنف المحفوظ وقت البيع)</li>
                  <li><strong>barcodeSnapshot:</strong> String? (باركود الصنف وقت البيع)</li>
                  <li><strong>unitCostPrice:</strong> Double (سعر التكلفة لحظة البيع)</li>
                  <li><strong>unitSellingPrice:</strong> Double (سعر البيع لحظة خروج الفاتورة)</li>
                  <li><strong>quantity:</strong> Int (الكمية المباعة)</li>
                  <li><strong>total:</strong> Double (إجمالي السطر = quantity × unitSellingPrice)</li>
                </ul>
              </div>
            </div>
          </div>

          {/* 3. Transaction والخصم التلقائي */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">3. كيف تم تنفيذ Transaction والخصم التلقائي</h2>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-700 leading-relaxed space-y-2">
              <p>
                تم تنفيذ عملية البيع بالكامل داخل دالة <code>completeSale(...)</code> في <code>SalesRepository</code> باستخدام:
              </p>
              <pre className="bg-slate-900 text-emerald-400 p-3 rounded-lg font-mono text-[11px] overflow-x-auto" dir="ltr">
{`database.withTransaction {
    // 1. فحص توفر المخزون الحي لكل صنف في السلة بقفل مباشر
    // 2. إدراج رأس الفاتورة في جدول sales_invoices
    // 3. إدراج بنود الأصناف في جدول sale_items مع الـ Snapshots
    // 4. خصم الكمية من جدول products: productDao.decrementQuantity(...)
    // 5. تسجيل حركة مخزنية في جدول stock_movements من نوع SALE مع رقم الفاتورة referenceId
}`}
              </pre>
              <p>
                إذا حدث أي استثناء أو انقطاع أو عدم توفر كمية في أي صنف أثناء العملية، تفشل الـ Transaction بأكملها وتتراجع قاعدة بيانات SQLite (Rollback) تلقائياً، فلا تُسجل الفاتورة، ولا تُخصم أي قطعة من المخزن، ويبقى التطبيق متسقاً 100%.
              </p>
            </div>
          </div>

          {/* 4. منع تجاوز المخزون */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">4. كيف تم منع تجاوز المخزون المتاح</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-slate-700">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                <div className="font-bold text-slate-900">حماية على مستوى واجهة المستخدم (PosViewModel):</div>
                <p className="text-slate-600 leading-relaxed">
                  عند زيادة الكمية في السلة (سواء بالضغط على + أو بإعادة قراءة الباركود)، يقوم التطبيق بفحص <code>currentInCart &gt;= product.quantity</code> فورياً. إذا وصل الحد الأقصى للمخزون يمنع الزيادة ويعرض رسالة تحذيرية: «لا يمكن زيادة الكمية عن المخزون المتاح».
                </p>
              </div>
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-1">
                <div className="font-bold text-slate-900">حماية صلبة داخل قاعدة البيانات (SalesRepository):</div>
                <p className="text-slate-600 leading-relaxed">
                  قبل أي خصم داخل الـ Transaction، يتم جلب المنتج مباشرة من قاعدة البيانات والتحقق من <code>freshProduct.quantity &lt; item.quantity</code>. إذا تبيّن عدم كفاية المخزون لأي سبب، يتم رمي <code>IllegalStateException</code> فوراً مما يطلق Rollback فوري ويحمي المخزون من الأرقام السالبة.
                </p>
              </div>
            </div>
          </div>

          {/* 5. التعامل مع Snapshot للأسعار */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-amber-50 rounded-lg text-amber-600">
                <Zap className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">5. كيف تم التعامل مع Snapshot للأسعار والبيانات</h2>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed mb-3">
              تم الالتزام الصارم بمبدأ عدم الاعتماد على بيانات جدول <code>products</code> مستقبلاً لعرض الفواتير القديمة:
            </p>
            <div className="p-4 bg-amber-50/50 rounded-xl border border-amber-100 text-xs text-slate-700 space-y-2">
              <p>
                في كل سطر بجدول <code>sale_items</code> يتم حفظ:
              </p>
              <ul className="list-disc list-inside space-y-1 font-mono text-[11px] text-slate-800">
                <li><code>productNameSnapshot</code> = اسم المنتج أثناء لحظة الضغط على إتمام البيع</li>
                <li><code>barcodeSnapshot</code> = باركود المنتج أثناء لحظة البيع</li>
                <li><code>unitCostPrice</code> = سعر الشراء والتكلفة وقت البيع (لحساب صافي الربح الحقيقي بدقة)</li>
                <li><code>unitSellingPrice</code> = سعر البيع المعتمد وقت خروج الفاتورة</li>
              </ul>
              <p className="text-emerald-800 font-semibold pt-1">
                النتيجة: لو قام المستخدم لاحقاً بتغيير اسم الصنف أو رفع سعره أو حتى حذفه، تظل كل الفواتير التاريخية محتفظة باسمه وسعره القديم كما خرجت تماماً دون أي تشوه.
              </p>
            </div>
          </div>

          {/* 6. حالات الفشل وكيف تم منعها */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-red-50 rounded-lg text-red-600">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">6. حالات الفشل التي تم التعامل معها ومنعها</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs text-slate-700">
              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">1. محاولة إتمام بيع لسلة فارغة</div>
                <p className="text-slate-600">زر إتمام البيع معطل، وتمنع الدالة أي معالجة لسلة لا تحتوي على أصناف صالحة.</p>
              </div>

              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">2. محاولة بيع منتج رصيده صفر (Out of stock)</div>
                <p className="text-slate-600">يتم رفض الإضافة للسلة مع رسالة واضحة: «المنتج نفد من المخزون»، وتتحقق الـ Transaction من ذلك ثانية.</p>
              </div>

              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">3. إدخال مبلغ مدفوع غير كافٍ في الدفع الكاش</div>
                <p className="text-slate-600">يتم التحقق من أن <code>paidAmount &gt;= total</code> في الكاش، وتنبيه المحاسب فوراً بالمبلغ المطلوب.</p>
              </div>

              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">4. تكرار أرقام الفواتير أو تعارض الـ Concurrency</div>
                <p className="text-slate-600">توليد رقم الفاتورة يعتمد على التاريخ والوقت وأجزاء الثانية والعد التسلسلي الفعلي مع قيد UNIQUE على عمود <code>invoiceNumber</code>.</p>
              </div>

              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">5. قراءة باركود غير مسجل في الكاشير</div>
                <p className="text-slate-600">عدم إضافة منتج وهمي تلقائياً؛ بل عرض نافذة «المنتج غير مسجل» مع خياري: إضافة المنتج أو إلغاء.</p>
              </div>

              <div className="p-3 bg-red-50/40 rounded-xl border border-red-100">
                <div className="font-bold text-red-900 mb-1">6. انقطاع أو خطأ برمجي أثناء الحفظ</div>
                <p className="text-slate-600">تضمن <code>database.withTransaction</code> التراجع الشامل (Rollback) وعدم بقاء أي بيانات جزئية أو غير متسقة.</p>
              </div>
            </div>
          </div>

          {/* 7. الملفات التي تم إنشاؤها وتعديلها */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs">
            <div className="flex items-center gap-3 mb-4 text-slate-900">
              <div className="p-2 bg-slate-100 rounded-lg text-slate-700">
                <FileCode className="w-5 h-5" />
              </div>
              <h2 className="text-lg font-bold">7. الملفات التي تم إنشاؤها وتعديلها في المرحلة الثالثة</h2>
            </div>
            <div className="overflow-x-auto text-xs font-mono">
              <table className="w-full text-right">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-700">
                  <tr>
                    <th className="p-3">مسار الملف</th>
                    <th className="p-3">الإجراء</th>
                    <th className="p-3">الدور والمسؤولية</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/entity/PaymentType.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">Enums لطرق الدفع (CASH/CREDIT) وحالات الفاتورة (COMPLETED/CANCELLED)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/entity/SalesInvoiceEntity.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">كيان رأس الفاتورة بـ SQLite مع فهارس البحث والقيد الفريد لرقم الفاتورة</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/entity/SaleItemEntity.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">كيان أصناف الفاتورة مع المفتاح الخارجي والـ Snapshot للأسعار والاسم</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/Converters.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">إضافة TypeConverters لتحويل PaymentType و InvoiceStatus لـ SQLite</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/dao/SalesDao.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">واجهة Room DAO للمبيعات، الفواتير، والأصناف، واستعلامات تجميع الـ Dashboard الحقيقية</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/local/MarketyDatabase.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">تسجيل الكيانات الجديدة وترقية نسخة قاعدة البيانات إلى Version 2</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">data/repository/SalesRepository.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">تنفيذ الـ Transaction الذرية، خصم المخزون، توليد أرقام الفواتير، وحركات التدقيق</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/viewmodel/PosViewModel.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">منطق سلة المشتريات، مسح الباركود، حساب الخصم، إدارة طرق الدفع، والتحقق من التوفر</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/viewmodel/InvoicesViewModel.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">إدارة سجل الفواتير والبحث برقم الفاتورة وعرض تفاصيل الفاتورة الواحدة</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/viewmodel/DashboardViewModel.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">ربط إحصائيات المبيعات اليومية والكاش والآجل والأصناف المباعة بالبيانات الحقيقية</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/screens/PosScreen.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">واجهة الكاشير ونقطة البيع مع دعم اللغة العربية وواجهات تأكيد الدفع وإفراغ السلة</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/screens/InvoicesScreen.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">واجهة سجل الفواتير والمبيعات السابقة مع البحث والفلترة حسب رقم الفاتورة</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/screens/InvoiceDetailScreen.kt</td>
                    <td className="p-3 text-blue-600">إنشاء</td>
                    <td className="p-3 text-slate-600 font-sans">شاشة تفاصيل الفاتورة الصادرة كاملة مع بنودها وملاحظاتها وأسعار الـ Snapshot</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/screens/DashboardScreen.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">عرض مؤشرات مبيعات اليوم الحقيقية وتوفير زر سريع مباشر لفتح الكاشير وسجل الفواتير</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/screens/BarcodeScannerScreen.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">إضافة وضع الكاشير (isPosMode) مع نافذة تنبيه للمنتجات غير المسجلة مع خياري الإضافة والإلغاء</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-bold text-emerald-700">ui/navigation/NavGraph.kt</td>
                    <td className="p-3 text-amber-600">تعديل</td>
                    <td className="p-3 text-slate-600 font-sans">إضافة مسارات الكاشير (Pos) وسجل الفواتير (Invoices) وتفاصيل الفاتورة في شريط التنقل السفلي والـ NavHost</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'phase2' && (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <h2 className="text-lg font-bold text-slate-900">المرحلة الثانية: الكاميرا وماسح الباركود الحقيقي</h2>
          <p className="text-xs text-slate-600 leading-relaxed">
            تم تنفيذ تصوير المنتجات بالكاميرا الحقيقية وحفظ الصور في التخزين الداخلي، وماسح باركود حقيقي باستخدام Google ML Kit و CameraX مع قيد UNIQUE على الباركود في SQLite ومنع التكرار، ودمجها مع شاشات الإضافة والتعديل والبحث.
          </p>
        </div>
      )}

      {activeTab === 'phase1' && (
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <h2 className="text-lg font-bold text-slate-900">المرحلة الأولى: الأصناف والمخزون والفئات</h2>
          <p className="text-xs text-slate-600 leading-relaxed">
            تأسيس قاعدة بيانات SQLite و Room، الفئات، المنتجات، حركات المخزون، الحذف المنطقي Soft Delete، تنبيهات المخزون المنخفض، ولوحة التحكم.
          </p>
        </div>
      )}
    </div>
  );
};
