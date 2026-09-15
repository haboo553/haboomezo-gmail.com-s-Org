import React from 'react';
import { Database, Key, ArrowLeftRight, Hash, Receipt, ShoppingBag } from 'lucide-react';

export const SchemaView: React.FC = () => {
  return (
    <div className="space-y-8 text-right" dir="rtl">
      <div>
        <h2 className="text-xl font-bold text-slate-900 mb-2">مخطط قاعدة بيانات SQLite (Room Relational Schema - Version 2)</h2>
        <p className="text-sm text-slate-600">
          تم تصميم الجداول مع تفعيل المفاتيح الخارجية (Foreign Keys) والفهارس المركبة (Indexes) والقيود الصارمة (Unique, Restrict, Cascade) لضمان اتساق بيانات السوبر ماركت والمبيعات ومنع تكرار الباركود أو تلف فواتير المبيعات.
        </p>
      </div>

      {/* Relations Summary Diagram */}
      <div className="bg-slate-900 text-white p-6 rounded-xl border border-slate-800">
        <div className="text-xs text-sky-400 font-mono mb-2">العلاقات بين جداول SQLite (Foreign Keys & Cascades)</div>
        <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-center font-mono text-xs py-4 text-center">
          <div className="bg-slate-800 p-3 rounded-lg border border-slate-700">
            <div className="text-purple-300 font-bold text-sm">Categories</div>
            <div className="text-[11px] text-slate-400">id (PK)</div>
          </div>

          <div className="text-slate-400 text-[11px] flex items-center justify-center gap-1">
            <span>1</span>
            <div className="w-12 h-0.5 bg-slate-600 relative">
              <ArrowLeftRight className="w-3.5 h-3.5 text-purple-400 absolute -top-1.5 left-4" />
            </div>
            <span>N</span>
          </div>

          <div className="bg-slate-800 p-3 rounded-lg border border-slate-700">
            <div className="text-sky-300 font-bold text-sm">Products</div>
            <div className="text-[11px] text-slate-400">id (PK) | barcode (UNIQUE)</div>
          </div>

          <div className="text-slate-400 text-[11px] flex items-center justify-center gap-1">
            <span>1</span>
            <div className="w-12 h-0.5 bg-slate-600 relative">
              <ArrowLeftRight className="w-3.5 h-3.5 text-emerald-400 absolute -top-1.5 left-4" />
            </div>
            <span>N</span>
          </div>

          <div className="bg-slate-800 p-3 rounded-lg border border-slate-700">
            <div className="text-emerald-300 font-bold text-sm">StockMovements</div>
            <div className="text-[11px] text-slate-400">id (PK) | productId (FK)</div>
          </div>
        </div>

        {/* Phase 3 Sales Relations */}
        <div className="border-t border-slate-800 pt-4 mt-2">
          <div className="text-[11px] text-emerald-400 font-mono mb-2">علاقة فواتير المبيعات (Sales Invoices & Items Snapshot):</div>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 font-mono text-xs py-2">
            <div className="bg-slate-800 p-3 rounded-lg border border-slate-700 text-center min-w-[160px]">
              <div className="text-emerald-300 font-bold text-sm">SalesInvoices</div>
              <div className="text-[11px] text-slate-400">id (PK) | invoiceNumber (UNIQUE)</div>
            </div>

            <div className="text-slate-400 text-xs flex items-center gap-2">
              <span>1</span>
              <div className="w-16 h-0.5 bg-slate-600 relative">
                <ArrowLeftRight className="w-4 h-4 text-emerald-400 absolute -top-2 left-6" />
              </div>
              <span>N (CASCADE DELETE)</span>
            </div>

            <div className="bg-slate-800 p-3 rounded-lg border border-slate-700 text-center min-w-[160px]">
              <div className="text-amber-300 font-bold text-sm">SaleItems</div>
              <div className="text-[11px] text-slate-400">invoiceId (FK) | productId (FK RESTRICT)</div>
              <div className="text-[10px] text-emerald-400">Snapshots: name & price</div>
            </div>
          </div>
        </div>
      </div>

      {/* The Detailed Tables */}
      <div className="grid grid-cols-1 gap-6">

        {/* Sales Invoices Table (Phase 3) */}
        <div className="bg-white rounded-xl border border-emerald-200 overflow-hidden shadow-xs">
          <div className="bg-emerald-50 p-4 border-b border-emerald-200 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono font-bold text-emerald-900">
              <Receipt className="w-5 h-5 text-emerald-600" />
              <span>جدول فواتير المبيعات: sales_invoices (المرحلة الثالثة)</span>
            </div>
            <span className="text-xs bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded-full font-sans font-semibold border border-emerald-300">
              Entity: SalesInvoiceEntity
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100/75 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع في SQLite</th>
                  <th className="p-3">القيود (Constraints)</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1">
                    <Key className="w-3 h-3 text-amber-500" /> id
                  </td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الداخلي للفاتورة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">invoiceNumber</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 text-red-600 font-sans font-bold flex items-center gap-1">
                    <Hash className="w-3 h-3" /> UNIQUE INDEX
                  </td>
                  <td className="p-3 font-sans text-slate-600">رقم الفاتورة الفريد بصيغة MK-YYYYMMDD-XXXX</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">dateTime</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-blue-700 font-sans flex items-center gap-1">
                    <Hash className="w-3 h-3" /> INDEXED
                  </td>
                  <td className="p-3 font-sans text-slate-600">طابع الوقت لإتمام الفاتورة بالمللي ثانية</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">subtotal</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">NOT NULL</td>
                  <td className="p-3 font-sans text-slate-600">الإجمالي قبل الخصم</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">discount</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">DEFAULT 0.0</td>
                  <td className="p-3 font-sans text-slate-600">مبلغ الخصم النقدي</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">total</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-emerald-700 font-bold">subtotal - discount</td>
                  <td className="p-3 font-sans text-slate-600">المبلغ الإجمالي النهائي المطلوب</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">paidAmount</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">NOT NULL</td>
                  <td className="p-3 font-sans text-slate-600">المبلغ المدفوع من العميل</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">remainingAmount</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">DEFAULT 0.0</td>
                  <td className="p-3 font-sans text-slate-600">المتبقي على العميل (في حالة البيع الآجل)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">paymentType</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-700">CASH / CREDIT</td>
                  <td className="p-3 font-sans text-slate-600">طريقة الدفع (كاش نقدي أو آجل شكك)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">status</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-700">COMPLETED / CANCELLED</td>
                  <td className="p-3 font-sans text-slate-600">حالة الفاتورة</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Sale Items Table (Phase 3) */}
        <div className="bg-white rounded-xl border border-amber-200 overflow-hidden shadow-xs">
          <div className="bg-amber-50 p-4 border-b border-amber-200 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono font-bold text-amber-900">
              <ShoppingBag className="w-5 h-5 text-amber-600" />
              <span>جدول أصناف الفاتورة: sale_items (مع لقطة الأسعار Snapshot)</span>
            </div>
            <span className="text-xs bg-amber-100 text-amber-800 px-2.5 py-1 rounded-full font-sans font-semibold border border-amber-300">
              Entity: SaleItemEntity
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100/75 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع في SQLite</th>
                  <th className="p-3">القيود (Constraints)</th>
                  <th className="p-3">الوصف ودور الـ Snapshot</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1">
                    <Key className="w-3 h-3 text-amber-500" /> id
                  </td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد لسجل الصنف المباع</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">invoiceId</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-indigo-700 font-sans">FK -&gt; sales_invoices(id) [CASCADE]</td>
                  <td className="p-3 font-sans text-slate-600">معرف الفاتورة التابع لها</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">productId</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-indigo-700 font-sans">FK -&gt; products(id) [RESTRICT]</td>
                  <td className="p-3 font-sans text-slate-600">معرف المنتج الأصلي في المخزن</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-emerald-700">productNameSnapshot</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-emerald-800 font-bold">SNAPSHOT IMMUTABLE</td>
                  <td className="p-3 font-sans text-slate-600">اسم المنتج لحظة البيع (لا يتغير أبداً بتعديل اسم الصنف لاحقاً)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-emerald-700">barcodeSnapshot</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-emerald-800 font-bold">SNAPSHOT IMMUTABLE</td>
                  <td className="p-3 font-sans text-slate-600">باركود المنتج وقت البيع للرجوع القانوني</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">unitCostPrice</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">SNAPSHOT</td>
                  <td className="p-3 font-sans text-slate-600">سعر الشراء وقت البيع (لحساب صافي أرباح الفاتورة بدقة)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">unitSellingPrice</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">SNAPSHOT</td>
                  <td className="p-3 font-sans text-slate-600">سعر البيع المعتمد وقت خروج الفاتورة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">quantity</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 font-sans text-slate-700">&gt; 0</td>
                  <td className="p-3 font-sans text-slate-600">الكمية المباعة من هذا الصنف</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">total</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">quantity × unitSellingPrice</td>
                  <td className="p-3 font-sans text-slate-600">إجمالي سطر الصنف</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Categories Table */}
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="bg-slate-50 p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono font-bold text-slate-800">
              <Database className="w-4 h-4 text-purple-600" />
              <span>جدول الفئات: categories</span>
            </div>
            <span className="text-xs bg-slate-200 text-slate-800 px-2.5 py-1 rounded-full font-sans font-semibold">
              Entity: CategoryEntity
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100/75 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع</th>
                  <th className="p-3">القيود</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1">
                    <Key className="w-3 h-3 text-amber-500" /> id
                  </td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد للقسم</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">name</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 text-red-600 font-sans font-bold flex items-center gap-1">
                    <Hash className="w-3 h-3" /> UNIQUE INDEX
                  </td>
                  <td className="p-3 font-sans text-slate-600">اسم الفئة (ممنوع التكرار)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">createdAt</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 font-sans text-slate-500">NOT NULL</td>
                  <td className="p-3 font-sans text-slate-600">تاريخ الإنشاء</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Products Table */}
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="bg-slate-50 p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono font-bold text-slate-800">
              <Database className="w-4 h-4 text-sky-600" />
              <span>جدول المنتجات والمخزون: products</span>
            </div>
            <span className="text-xs bg-slate-200 text-slate-800 px-2.5 py-1 rounded-full font-sans font-semibold">
              Entity: ProductEntity
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100/75 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع</th>
                  <th className="p-3">القيود</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1">
                    <Key className="w-3 h-3 text-amber-500" /> id
                  </td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد للمنتج</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">categoryId</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-indigo-700 font-sans">FK -&gt; categories(id) [RESTRICT]</td>
                  <td className="p-3 font-sans text-slate-600">رقم الفئة التابع لها المنتج</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">name</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 text-blue-700 font-sans flex items-center gap-1">
                    <Hash className="w-3 h-3" /> INDEXED
                  </td>
                  <td className="p-3 font-sans text-slate-600">اسم المنتج التجاري</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">barcode</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 text-red-600 font-sans font-bold flex items-center gap-1">
                    <Hash className="w-3 h-3" /> UNIQUE INDEX (WHERE barcode != '')
                  </td>
                  <td className="p-3 font-sans text-slate-600">باركود السلعة الدولي أو المحلي (ممنوع التكرار)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">quantity</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 font-sans text-emerald-700 font-bold">خصم فوري في Transactions</td>
                  <td className="p-3 font-sans text-slate-600">الرصيد المتاح الحالي في المخزن</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">purchasePrice</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">&gt;= 0.0</td>
                  <td className="p-3 font-sans text-slate-600">سعر الشراء الحالي</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">sellingPrice</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">&gt;= 0.0</td>
                  <td className="p-3 font-sans text-slate-600">سعر البيع للجمهور</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">isActive</td>
                  <td className="p-3 text-slate-600">INTEGER (Boolean)</td>
                  <td className="p-3 font-sans text-slate-700">DEFAULT 1 (Soft Delete)</td>
                  <td className="p-3 font-sans text-slate-600">1: نشط، 0: محذوف منطقياً</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Stock Movements Table */}
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="bg-slate-50 p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono font-bold text-slate-800">
              <Database className="w-4 h-4 text-emerald-600" />
              <span>جدول حركات المخزون والتدقيق: stock_movements</span>
            </div>
            <span className="text-xs bg-slate-200 text-slate-800 px-2.5 py-1 rounded-full font-sans font-semibold">
              Entity: StockMovementEntity
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100/75 text-slate-700 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع</th>
                  <th className="p-3">القيود</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1">
                    <Key className="w-3 h-3 text-amber-500" /> id
                  </td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد لحركة المخزون</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">productId</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-indigo-700 font-sans">FK -&gt; products(id) [RESTRICT]</td>
                  <td className="p-3 font-sans text-slate-600">الصنف الذي تمت عليه الحركة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">movementType</td>
                  <td className="p-3 text-slate-600">TEXT (Enum)</td>
                  <td className="p-3 font-sans text-slate-700">SALE, INITIAL, ADJUSTMENT, etc.</td>
                  <td className="p-3 font-sans text-slate-600">نوع الحركة (في المبيعات يسجل تلقائياً SALE)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">quantity</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 font-sans text-slate-700">قيمة سالبة في البيع (-X)</td>
                  <td className="p-3 font-sans text-slate-600">فرق الكمية المخصومة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">referenceId</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-700">رقم الفاتورة MK-YYYYMMDD-XXXX</td>
                  <td className="p-3 font-sans text-slate-600">ربط الحركة برقم الفاتورة الصادرة</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Table 5: DailyOperations (Phase 4) */}
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="bg-slate-50 p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-bold text-slate-900">5. daily_operations</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                Phase 4: إدارة يوم التشغيل
              </span>
            </div>
            <div className="text-xs text-slate-500 font-mono">SQLite Table: daily_operations</div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع</th>
                  <th className="p-3">القيود</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1"><Key className="w-3 h-3 text-amber-500" /> id</td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد ليوم التشغيل</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">date</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-700">YYYY-MM-DD</td>
                  <td className="p-3 font-sans text-slate-600">تاريخ يوم التشغيل</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">openingCash</td>
                  <td className="p-3 text-slate-600">REAL (Double)</td>
                  <td className="p-3 font-sans text-slate-700">NOT NULL</td>
                  <td className="p-3 font-sans text-slate-600">رصيد البداية المسجل عند الافتتاح</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-sky-700">totalSales</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">إجمالي المبيعات المحسوبة من الفواتير</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-emerald-700">cashSales</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">المبيعات النقدية (الكاش المحصل)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-amber-700">creditSales</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">المبيعات الآجلة (المتبقي من الفواتير)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-rose-700">expenses</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">إجمالي المصروفات والنثريات المسجلة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-purple-700">netSales</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">صافي المبيعات (المبيعات - المصروفات)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">closingCash</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">الرصيد المتوقع في الخزينة عند الإغلاق</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">actualCash</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">النقدية الفعلية بعد العد اليدوي</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">difference</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">Default 0.0</td>
                  <td className="p-3 font-sans text-slate-600">الفرق (الفعلي - المتوقع): عجز أو زيادة</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">status</td>
                  <td className="p-3 text-slate-600">TEXT (Enum)</td>
                  <td className="p-3 font-sans text-slate-700">OPEN, CLOSED</td>
                  <td className="p-3 font-sans text-slate-600">حالة اليوم (عند الإغلاق يمنع التعديل نهائياً)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Table 6: Expenses (Phase 4) */}
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="bg-slate-50 p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-bold text-slate-900">6. expenses</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                Phase 4: المصروفات والنثريات
              </span>
            </div>
            <div className="text-xs text-slate-500 font-mono">SQLite Table: expenses</div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="p-3">اسم الحقل</th>
                  <th className="p-3">النوع</th>
                  <th className="p-3">القيود</th>
                  <th className="p-3">الوصف</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono">
                <tr>
                  <td className="p-3 font-bold text-sky-700 flex items-center gap-1"><Key className="w-3 h-3 text-amber-500" /> id</td>
                  <td className="p-3 text-slate-600">INTEGER (Long)</td>
                  <td className="p-3 text-emerald-700 font-sans">PRIMARY KEY AUTOINCREMENT</td>
                  <td className="p-3 font-sans text-slate-600">المعرف الفريد للمصروف</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-indigo-700">dailyOperationId</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 text-indigo-700 font-sans">FK -&gt; daily_operations(id) [CASCADE]</td>
                  <td className="p-3 font-sans text-slate-600">ربط المصروف بيوم التشغيل التابع له</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">category</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-700">NOT NULL</td>
                  <td className="p-3 font-sans text-slate-600">تصنيف المصروف (فواتير، إيجار، عمالة، إلخ)</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-rose-700">amount</td>
                  <td className="p-3 text-slate-600">REAL</td>
                  <td className="p-3 font-sans text-slate-700">&gt; 0</td>
                  <td className="p-3 font-sans text-slate-600">قيمة المصروف بالجنيه</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">notes</td>
                  <td className="p-3 text-slate-600">TEXT</td>
                  <td className="p-3 font-sans text-slate-500">NULLABLE</td>
                  <td className="p-3 font-sans text-slate-600">ملاحظات وتفاصيل المصروف</td>
                </tr>
                <tr>
                  <td className="p-3 font-bold text-slate-900">date</td>
                  <td className="p-3 text-slate-600">INTEGER</td>
                  <td className="p-3 font-sans text-slate-700">Timestamp (Millis)</td>
                  <td className="p-3 font-sans text-slate-600">تاريخ ووقت تسجيل المصروف</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
};
