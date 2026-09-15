import React, { useState, useEffect, useMemo } from 'react';
import { Category, Product, StockMovement, MovementType, SalesInvoice, SaleItem, DailyOperation, Expense } from '../types';
import { 
  Plus, Search, RefreshCw, Trash2, Edit, AlertCircle, CheckCircle2, 
  Package, Folder, DollarSign, Layers, ArrowUpRight, ArrowDownRight,
  TrendingDown, ShieldCheck, History, ShoppingCart, Receipt, Calendar
} from 'lucide-react';
import { LivePosView } from './LivePosView';
import { LiveDailyOperationsView } from './LiveDailyOperationsView';

const STORAGE_KEYS = {
  CATEGORIES: 'markety_categories_v1',
  PRODUCTS: 'markety_products_v1',
  MOVEMENTS: 'markety_movements_v1',
  INVOICES: 'markety_sales_invoices_v1',
  SALE_ITEMS: 'markety_sale_items_v1',
  DAILY_OPERATIONS: 'markety_daily_operations_v1',
  EXPENSES: 'markety_expenses_v1',
};

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, name: 'مشروبات', createdAt: Date.now() - 100000 },
  { id: 2, name: 'بسكويت وحلويات', createdAt: Date.now() - 90000 },
  { id: 3, name: 'ألبان وجبن', createdAt: Date.now() - 80000 },
  { id: 4, name: 'منظفات', createdAt: Date.now() - 70000 },
  { id: 5, name: 'معلبات', createdAt: Date.now() - 60000 },
  { id: 6, name: 'زيوت وسمن', createdAt: Date.now() - 50000 },
  { id: 7, name: 'مياه معدنية', createdAt: Date.now() - 40000 },
  { id: 8, name: 'عصائر', createdAt: Date.now() - 30000 },
];

export const LiveEngineView: React.FC = () => {
  // Database States
  const [categories, setCategories] = useState<Category[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.CATEGORIES);
    return saved ? JSON.parse(saved) : DEFAULT_CATEGORIES;
  });

  const [products, setProducts] = useState<Product[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.PRODUCTS);
    return saved ? JSON.parse(saved) : [];
  });

  const [movements, setMovements] = useState<StockMovement[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.MOVEMENTS);
    return saved ? JSON.parse(saved) : [];
  });

  const [invoices, setInvoices] = useState<SalesInvoice[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.INVOICES);
    return saved ? JSON.parse(saved) : [];
  });

  const [saleItems, setSaleItems] = useState<SaleItem[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.SALE_ITEMS);
    return saved ? JSON.parse(saved) : [];
  });

  const [dailyOperations, setDailyOperations] = useState<DailyOperation[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.DAILY_OPERATIONS);
    return saved ? JSON.parse(saved) : [];
  });

  const [expenses, setExpenses] = useState<Expense[]>(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.EXPENSES);
    return saved ? JSON.parse(saved) : [];
  });

  // UI state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | 'ALL'>('ALL');
  const [activeTab, setActiveTab] = useState<'daily_operations' | 'pos' | 'products' | 'movements' | 'categories'>('daily_operations');
  const [showAddProductModal, setShowAddProductModal] = useState(false);
  const [showAddCategoryModal, setShowAddCategoryModal] = useState(false);
  const [adjustingProduct, setAdjustingProduct] = useState<Product | null>(null);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [selectedProductHistory, setSelectedProductHistory] = useState<Product | null>(null);

  // Sync to local storage
  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(categories));
  }, [categories]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.PRODUCTS, JSON.stringify(products));
  }, [products]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.MOVEMENTS, JSON.stringify(movements));
  }, [movements]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.INVOICES, JSON.stringify(invoices));
  }, [invoices]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.SALE_ITEMS, JSON.stringify(saleItems));
  }, [saleItems]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.DAILY_OPERATIONS, JSON.stringify(dailyOperations));
  }, [dailyOperations]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.EXPENSES, JSON.stringify(expenses));
  }, [expenses]);

  // Dashboard Aggregations (Exact mirror of Room Dao queries)
  const activeProducts = useMemo(() => products.filter(p => p.isActive), [products]);
  
  const stats = useMemo(() => {
    const totalCount = activeProducts.length;
    const totalCategories = categories.length;
    const totalStock = activeProducts.reduce((sum, p) => sum + p.quantity, 0);
    const lowStockCount = activeProducts.filter(p => p.quantity > 0 && p.quantity <= p.minimumQuantity).length;
    const outOfStockCount = activeProducts.filter(p => p.quantity <= 0).length;

    // Daily sales aggregations (Requirement 13)
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    const todayMillis = startOfToday.getTime();

    const todayInvoices = invoices.filter(inv => inv.dateTime >= todayMillis);
    const todaySalesTotal = todayInvoices.reduce((sum, inv) => sum + inv.total, 0);
    const todayInvoicesCount = todayInvoices.length;
    const todayCashTotal = todayInvoices.filter(inv => inv.paymentType === 'CASH').reduce((sum, inv) => sum + inv.total, 0);
    const todayCreditTotal = todayInvoices.filter(inv => inv.paymentType === 'CREDIT').reduce((sum, inv) => sum + inv.remainingAmount, 0);

    const todayInvoiceIds = new Set(todayInvoices.map(i => i.id));
    const todayItemsSoldCount = saleItems
      .filter(item => todayInvoiceIds.has(item.invoiceId))
      .reduce((sum, item) => sum + item.quantity, 0);

    return { 
      totalCount, 
      totalCategories, 
      totalStock, 
      lowStockCount, 
      outOfStockCount,
      todaySalesTotal,
      todayInvoicesCount,
      todayCashTotal,
      todayCreditTotal,
      todayItemsSoldCount
    };
  }, [activeProducts, categories, invoices, saleItems]);

  // Filtered products list (Search by name or barcode, filter by category)
  const filteredProducts = useMemo(() => {
    return activeProducts.filter(p => {
      const q = searchQuery.trim().toLowerCase();
      const matchesSearch = !q || p.name.toLowerCase().includes(q) || (p.barcode ? p.barcode.includes(q) : false);
      const matchesCategory = selectedCategoryId === 'ALL' || p.categoryId === selectedCategoryId;
      return matchesSearch && matchesCategory;
    });
  }, [activeProducts, searchQuery, selectedCategoryId]);

  // Add Product handler (Automatically records INITIAL movement if quantity > 0)
  const handleAddProduct = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const name = String(formData.get('name') || '').trim();
    const barcode = String(formData.get('barcode') || '').trim();
    const categoryId = Number(formData.get('categoryId'));
    const purchasePrice = Number(formData.get('purchasePrice')) || 0;
    const sellingPrice = Number(formData.get('sellingPrice')) || 0;
    const quantity = Number(formData.get('quantity')) || 0;
    const minimumQuantity = Number(formData.get('minimumQuantity')) || 5;
    const supplierName = String(formData.get('supplierName') || '').trim() || undefined;
    const expiryDate = String(formData.get('expiryDate') || '').trim() || undefined;
    const notes = String(formData.get('notes') || '').trim() || undefined;

    if (!name) {
      alert('يرجى إدخال اسم الصنف');
      return;
    }

    // Check unique barcode if provided
    if (barcode) {
      const isTaken = products.some(p => p.isActive && p.barcode === barcode);
      if (isTaken) {
        alert('هذا الباركود مسجل بالفعل لمنتج آخر');
        return;
      }
    }

    const newProductId = Date.now();
    const newProduct: Product = {
      id: newProductId,
      name,
      barcode: barcode || undefined,
      categoryId,
      purchasePrice,
      sellingPrice,
      quantity,
      minimumQuantity,
      supplierName,
      expiryDate,
      notes,
      createdAt: Date.now(),
      updatedAt: Date.now(),
      isActive: true,
    };

    setProducts(prev => [newProduct, ...prev]);

    // Requirement 9: INITIAL movement
    if (quantity > 0) {
      const initialMovement: StockMovement = {
        id: Date.now() + 1,
        productId: newProductId,
        movementType: 'INITIAL',
        quantity: quantity,
        previousQuantity: 0,
        newQuantity: quantity,
        purchasePrice,
        sellingPrice,
        notes: 'رصيد افتتاحي للصنف عند إضافته',
        createdAt: Date.now(),
      };
      setMovements(prev => [initialMovement, ...prev]);
    }

    setShowAddProductModal(false);
  };

  // Adjust Product Quantity handler (Requirement 10: ADJUSTMENT movement)
  const handleAdjustQuantity = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!adjustingProduct) return;

    const formData = new FormData(e.currentTarget);
    const newQuantity = Number(formData.get('newQuantity'));
    const reason = String(formData.get('reason') || 'تعديل جرد يدوي').trim();

    const oldQuantity = adjustingProduct.quantity;
    if (oldQuantity === newQuantity) {
      setAdjustingProduct(null);
      return;
    }

    const delta = newQuantity - oldQuantity;

    // Record ADJUSTMENT movement
    const adjustmentMovement: StockMovement = {
      id: Date.now(),
      productId: adjustingProduct.id,
      movementType: 'ADJUSTMENT',
      quantity: delta,
      previousQuantity: oldQuantity,
      newQuantity: newQuantity,
      purchasePrice: adjustingProduct.purchasePrice,
      sellingPrice: adjustingProduct.sellingPrice,
      notes: reason,
      createdAt: Date.now(),
    };

    setMovements(prev => [adjustmentMovement, ...prev]);

    // Update Product quantity in DB
    setProducts(prev =>
      prev.map(p =>
        p.id === adjustingProduct.id
          ? { ...p, quantity: newQuantity, updatedAt: Date.now() }
          : p
      )
    );

    setAdjustingProduct(null);
  };

  // Soft delete product handler (Requirement 8)
  const handleSoftDelete = (productId: number) => {
    if (!window.confirm('هل تريد حذف الصنف؟ سيتم تطبيق الحذف غير المباشر (Soft Delete: isActive = false) وحفظ سجل حركاته التاريخية بأمان.')) {
      return;
    }
    setProducts(prev =>
      prev.map(p => (p.id === productId ? { ...p, isActive: false, updatedAt: Date.now() } : p))
    );
  };

  // Add Category handler
  const handleAddCategory = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const name = String(formData.get('categoryName') || '').trim();
    if (!name) return;

    const newCat: Category = {
      id: Date.now(),
      name,
      createdAt: Date.now(),
    };
    setCategories(prev => [...prev, newCat]);
    setShowAddCategoryModal(false);
  };

  // Complete Sale handler (Phase 3 Atomic Transaction Simulation)
  const handleCompleteSale = (
    updatedProducts: Product[],
    newMovements: StockMovement[],
    newInvoice: SalesInvoice,
    newSaleItems: SaleItem[]
  ) => {
    setProducts(updatedProducts);
    setMovements(prev => [...newMovements, ...prev]);
    setInvoices(prev => [newInvoice, ...prev]);
    setSaleItems(prev => [...newSaleItems, ...prev]);
  };

  return (
    <div className="space-y-6 text-right" dir="rtl">
      {/* Top Controls & Explanation */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-extrabold text-slate-900">
            محرك التخزين المحلي التفاعلي ونقطة البيع (Live Local Database & POS Engine)
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            يُحاكي هذا المكون نفس استعلامات Room و SQLite تماماً (المفاتيح الخارجية، الحركات التلقائية INITIAL و ADJUSTMENT، والمعاملات الذرية ونظام المبيعات POS) مع حفظ دائم في متصفحك.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              if (window.confirm('إعادة تعيين قاعدة البيانات المحلية وحذف كل المنتجات والفواتير؟')) {
                localStorage.clear();
                setCategories(DEFAULT_CATEGORIES);
                setProducts([]);
                setMovements([]);
                setInvoices([]);
                setSaleItems([]);
              }
            }}
            className="px-3 py-1.5 text-xs text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg flex items-center gap-1.5 transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>إعادة ضبط البيانات</span>
          </button>
        </div>
      </div>

      {/* Real Daily Sales Dashboard (Phase 3 Requirement 13) */}
      <div className="bg-gradient-to-l from-slate-900 via-slate-800 to-indigo-950 text-white p-5 rounded-2xl shadow-xs space-y-3 border border-slate-800">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="p-1.5 bg-emerald-500/20 text-emerald-400 rounded-lg border border-emerald-500/30">
              <Receipt className="w-4 h-4" />
            </span>
            <span className="font-bold text-xs">مؤشرات مبيعات اليوم الحقيقية (Real Sales Dashboard Metrics)</span>
          </div>
          <span className="text-[11px] text-slate-400 font-mono">تحديث لحظي من جدول sales_invoices</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          <div className="bg-slate-800/80 border border-slate-700/80 p-3 rounded-xl">
            <div className="text-[11px] text-slate-400">إجمالي مبيعات اليوم</div>
            <div className="text-xl font-black font-mono text-emerald-400 mt-1">
              {stats.todaySalesTotal.toFixed(2)} ج.م
            </div>
            <div className="text-[10px] text-emerald-400/80 mt-0.5">القيمة الإجمالية</div>
          </div>
          <div className="bg-slate-800/80 border border-slate-700/80 p-3 rounded-xl">
            <div className="text-[11px] text-slate-400">عدد فواتير اليوم</div>
            <div className="text-xl font-black font-mono text-sky-400 mt-1">
              {stats.todayInvoicesCount}
            </div>
            <div className="text-[10px] text-sky-400/80 mt-0.5">فاتورة مسجلة</div>
          </div>
          <div className="bg-slate-800/80 border border-slate-700/80 p-3 rounded-xl">
            <div className="text-[11px] text-slate-400">المحصل نقداً (كاش)</div>
            <div className="text-xl font-black font-mono text-emerald-300 mt-1">
              {stats.todayCashTotal.toFixed(2)} ج.م
            </div>
            <div className="text-[10px] text-emerald-400/80 mt-0.5">في الخزينة</div>
          </div>
          <div className="bg-slate-800/80 border border-slate-700/80 p-3 rounded-xl">
            <div className="text-[11px] text-slate-400">المتبقي آجل (شكك)</div>
            <div className="text-xl font-black font-mono text-amber-400 mt-1">
              {stats.todayCreditTotal.toFixed(2)} ج.م
            </div>
            <div className="text-[10px] text-amber-400/80 mt-0.5">ديون مستحقة</div>
          </div>
          <div className="bg-slate-800/80 border border-slate-700/80 p-3 rounded-xl col-span-2 sm:col-span-1">
            <div className="text-[11px] text-slate-400">قطع مباعة اليوم</div>
            <div className="text-xl font-black font-mono text-purple-400 mt-1">
              {stats.todayItemsSoldCount}
            </div>
            <div className="text-[10px] text-purple-400/80 mt-0.5">قطعة تم خصمها</div>
          </div>
        </div>
      </div>

      {/* Live Dashboard Cards (Requirement 4: Dashboard reading real DB data) */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div className="bg-sky-50 border border-sky-100 p-4 rounded-xl">
          <div className="text-xs font-semibold text-sky-800">إجمالي الأصناف</div>
          <div className="text-2xl font-black text-sky-900 mt-1">{stats.totalCount}</div>
          <div className="text-[11px] text-sky-600 mt-0.5">صنف مفعل في المخزن</div>
        </div>

        <div className="bg-purple-50 border border-purple-100 p-4 rounded-xl">
          <div className="text-xs font-semibold text-purple-800">عدد الفئات</div>
          <div className="text-2xl font-black text-purple-900 mt-1">{stats.totalCategories}</div>
          <div className="text-[11px] text-purple-600 mt-0.5">أقسام رئيسية</div>
        </div>

        <div className="bg-emerald-50 border border-emerald-100 p-4 rounded-xl">
          <div className="text-xs font-semibold text-emerald-800">إجمالي الكمية</div>
          <div className="text-2xl font-black text-emerald-900 mt-1">{stats.totalStock}</div>
          <div className="text-[11px] text-emerald-600 mt-0.5">قطعة إجمالية بالمخزن</div>
        </div>

        <div className="bg-amber-50 border border-amber-100 p-4 rounded-xl">
          <div className="text-xs font-semibold text-amber-800">مخزون منخفض</div>
          <div className="text-2xl font-black text-amber-900 mt-1">{stats.lowStockCount}</div>
          <div className="text-[11px] text-amber-600 mt-0.5">وصل لحد الطلب الأدنى</div>
        </div>

        <div className="bg-red-50 border border-red-100 p-4 rounded-xl col-span-2 md:col-span-1">
          <div className="text-xs font-semibold text-red-800">أصناف نفدت</div>
          <div className="text-2xl font-black text-red-900 mt-1">{stats.outOfStockCount}</div>
          <div className="text-[11px] text-red-600 mt-0.5">الكمية = 0</div>
        </div>
      </div>

      {/* Main Tabs */}
      <div className="flex items-center justify-between border-b border-slate-200">
        <div className="flex gap-4 text-sm font-semibold">
          <button
            onClick={() => setActiveTab('daily_operations')}
            className={`pb-3 border-b-2 transition flex items-center gap-1.5 ${
              activeTab === 'daily_operations'
                ? 'border-sky-600 text-sky-600 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Calendar className="w-4 h-4" />
            <span>إدارة اليوم والمصروفات</span>
            {dailyOperations.find(o => o.status === 'OPEN') ? (
              <span className="px-1.5 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded-full font-bold animate-pulse">
                يوم مفتوح
              </span>
            ) : (
              <span className="px-1.5 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded-full font-bold">
                مغلق
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('pos')}
            className={`pb-3 border-b-2 transition flex items-center gap-1.5 ${
              activeTab === 'pos'
                ? 'border-emerald-600 text-emerald-600 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <ShoppingCart className="w-4 h-4" />
            <span>نقطة البيع (الكاشير والسلة)</span>
            {invoices.length > 0 && (
              <span className="px-1.5 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded-full font-mono font-bold">
                {invoices.length} فواتير
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab('products')}
            className={`pb-3 border-b-2 transition ${
              activeTab === 'products'
                ? 'border-sky-600 text-sky-600 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            الأصناف والمخزون ({filteredProducts.length})
          </button>
          <button
            onClick={() => setActiveTab('movements')}
            className={`pb-3 border-b-2 transition ${
              activeTab === 'movements'
                ? 'border-sky-600 text-sky-600 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            سجل حركات المخزون ({movements.length})
          </button>
          <button
            onClick={() => setActiveTab('categories')}
            className={`pb-3 border-b-2 transition ${
              activeTab === 'categories'
                ? 'border-sky-600 text-sky-600 font-bold'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            أقسام وفئات السوبر ماركت ({categories.length})
          </button>
        </div>

        {activeTab === 'products' && (
          <button
            onClick={() => setShowAddProductModal(true)}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold rounded-lg flex items-center gap-1.5 shadow-xs transition"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة صنف جديد</span>
          </button>
        )}

        {activeTab === 'categories' && (
          <button
            onClick={() => setShowAddCategoryModal(true)}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold rounded-lg flex items-center gap-1.5 shadow-xs transition"
          >
            <Plus className="w-4 h-4" />
            <span>إضافة فئة</span>
          </button>
        )}
      </div>

      {/* TAB 1: PRODUCTS */}
      {activeTab === 'products' && (
        <div className="space-y-4">
          {/* Search & Category Filter */}
          <div className="flex flex-col md:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute right-3 top-3" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="ابحث بالاسم أو الباركود (Indexed Search)..."
                className="w-full pl-3 pr-9 py-2 text-xs bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            <select
              value={selectedCategoryId}
              onChange={e => setSelectedCategoryId(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
              className="py-2 px-3 text-xs bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="ALL">جميع الفئات</option>
              {categories.map(c => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {filteredProducts.length === 0 ? (
            <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500 space-y-3">
              <Package className="w-12 h-12 mx-auto text-slate-300" />
              <p className="font-semibold text-sm">لا توجد أصناف تطابق معايير البحث الحالية</p>
              <p className="text-xs text-slate-400">اضغط على &quot;إضافة صنف جديد&quot; لبدء إدخال الأصناف وتسجيل حركاتها الافتتاحية</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredProducts.map(p => {
                const category = categories.find(c => c.id === p.categoryId);
                const isOutOfStock = p.quantity <= 0;
                const isLowStock = p.quantity > 0 && p.quantity <= p.minimumQuantity;

                return (
                  <div
                    key={p.id}
                    className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs hover:border-sky-300 transition space-y-3"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="text-xs font-bold text-sky-700">{category?.name || 'فئة عامة'}</div>
                        <h3 className="font-bold text-slate-900 text-sm mt-0.5">{p.name}</h3>
                        <div className="text-[11px] font-mono text-slate-500 mt-0.5">
                          باركود: {p.barcode ? p.barcode : <span className="text-slate-400 font-sans">غير محدد (بدون باركود)</span>}
                        </div>
                      </div>

                      {/* Stock badge */}
                      {isOutOfStock ? (
                        <span className="px-2 py-0.5 text-[10px] font-bold rounded-md bg-red-100 text-red-800">
                          نفد
                        </span>
                      ) : isLowStock ? (
                        <span className="px-2 py-0.5 text-[10px] font-bold rounded-md bg-amber-100 text-amber-800">
                          مخزون منخفض
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 text-[10px] font-bold rounded-md bg-emerald-100 text-emerald-800">
                          متوفر
                        </span>
                      )}
                    </div>

                    <div className="grid grid-cols-3 gap-2 bg-slate-50 p-2.5 rounded-lg text-center font-mono text-xs">
                      <div>
                        <div className="text-[10px] font-sans text-slate-500">سعر الشراء</div>
                        <div className="font-bold text-slate-700">{p.purchasePrice} ج.م</div>
                      </div>
                      <div>
                        <div className="text-[10px] font-sans text-slate-500">سعر البيع</div>
                        <div className="font-bold text-sky-700">{p.sellingPrice} ج.م</div>
                      </div>
                      <div>
                        <div className="text-[10px] font-sans text-slate-500">الكمية الحالية</div>
                        <div className={`font-black ${isOutOfStock ? 'text-red-700' : 'text-slate-900'}`}>
                          {p.quantity}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-1 border-t border-slate-100 text-xs">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => setAdjustingProduct(p)}
                          className="px-2 py-1 bg-sky-50 text-sky-700 hover:bg-sky-100 font-semibold rounded text-[11px] transition"
                        >
                          تعديل الكمية والجرد
                        </button>
                        <button
                          onClick={() => setSelectedProductHistory(p)}
                          className="p-1 text-slate-500 hover:text-slate-800 rounded hover:bg-slate-100"
                          title="عرض سجل حركات المخزون لهذا الصنف"
                        >
                          <History className="w-4 h-4" />
                        </button>
                      </div>

                      <button
                        onClick={() => handleSoftDelete(p.id)}
                        className="p-1 text-red-500 hover:text-red-700 hover:bg-red-50 rounded"
                        title="حذف الصنف (Soft Delete)"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: STOCK MOVEMENTS (Requirement 11) */}
      {activeTab === 'movements' && (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
            <div className="font-bold text-xs text-slate-800">
              سجل تدقيق حركة المخزون التاريخية (Stock Movements History Table)
            </div>
            <div className="text-xs text-slate-500">
              التاريخ | نوع الحركة | الكمية | قبل | بعد
            </div>
          </div>

          {movements.length === 0 ? (
            <div className="p-8 text-center text-slate-400 text-xs">
              لم تسجل أي حركة مخزون حتى الآن. أضف صنفاً أو عدل كمية لتسجيل الحركات.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-right text-xs">
                <thead className="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="p-3">التاريخ والوقت</th>
                    <th className="p-3">الصنف</th>
                    <th className="p-3">نوع الحركة</th>
                    <th className="p-3">الكمية</th>
                    <th className="p-3">قبل</th>
                    <th className="p-3">بعد</th>
                    <th className="p-3">ملاحظات / سبب التعديل</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-mono">
                  {movements.map(m => {
                    const product = products.find(p => p.id === m.productId);
                    const isPositive = m.quantity >= 0;
                    return (
                      <tr key={m.id} className="hover:bg-slate-50">
                        <td className="p-3 text-slate-500 font-sans">
                          {new Date(m.createdAt).toLocaleString('ar-EG')}
                        </td>
                        <td className="p-3 font-sans font-bold text-slate-900">
                          {product?.name || `صنف #${m.productId} (محذوف)`}
                        </td>
                        <td className="p-3 font-sans">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            m.movementType === 'INITIAL'
                              ? 'bg-blue-100 text-blue-800'
                              : m.movementType === 'ADJUSTMENT'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-slate-100 text-slate-800'
                          }`}>
                            {m.movementType === 'INITIAL' ? 'إضافة افتتاحية' : 'تعديل جرد'}
                          </span>
                        </td>
                        <td className={`p-3 font-bold ${isPositive ? 'text-emerald-700' : 'text-red-700'}`}>
                          {isPositive ? `+${m.quantity}` : m.quantity}
                        </td>
                        <td className="p-3 text-slate-600">{m.previousQuantity}</td>
                        <td className="p-3 font-bold text-slate-900">{m.newQuantity}</td>
                        <td className="p-3 font-sans text-slate-600">{m.notes || '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB: DAILY OPERATIONS & EXPENSES (Phase 4) */}
      {activeTab === 'daily_operations' && (
        <LiveDailyOperationsView
          invoices={invoices}
          dailyOperations={dailyOperations}
          setDailyOperations={setDailyOperations}
          expenses={expenses}
          setExpenses={setExpenses}
        />
      )}

      {/* TAB 0: POS & INVOICES (Phase 3) */}
      {activeTab === 'pos' && (
        <LivePosView
          products={products}
          onCompleteSale={handleCompleteSale}
          invoices={invoices}
          saleItems={saleItems}
        />
      )}

      {/* TAB 3: CATEGORIES */}
      {activeTab === 'categories' && (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {categories.map(c => {
            const count = activeProducts.filter(p => p.categoryId === c.id).length;
            return (
              <div key={c.id} className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center justify-between">
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{c.name}</h4>
                  <div className="text-xs text-slate-500 mt-1 font-mono">{count} أصناف مرتبطة</div>
                </div>
                <div className="p-2.5 rounded-lg bg-purple-50 text-purple-600">
                  <Folder className="w-5 h-5" />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* MODAL: Add Product */}
      {showAddProductModal && (
        <div className="fixed inset-0 bg-slate-950/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto space-y-4 shadow-xl">
            <h3 className="font-bold text-base text-slate-900">إضافة صنف جديد لقاعدة البيانات</h3>
            <form onSubmit={handleAddProduct} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1">اسم الصنف *</label>
                <input
                  name="name"
                  required
                  placeholder="مثال: زيت زيتون وادي فود 500 مل"
                  className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-700 font-bold mb-1">الباركود (اختياري)</label>
                  <input
                    name="barcode"
                    placeholder="مثال: 6221000000 (أو اتركه فارغاً)"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-bold mb-1">الفئة *</label>
                  <select name="categoryId" className="w-full p-2.5 border border-slate-200 rounded-lg text-xs">
                    {categories.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-700 font-bold mb-1">سعر الشراء (ج.م) *</label>
                  <input
                    name="purchasePrice"
                    type="number"
                    step="0.5"
                    required
                    defaultValue="50"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-bold mb-1">سعر البيع (ج.م) *</label>
                  <input
                    name="sellingPrice"
                    type="number"
                    step="0.5"
                    required
                    defaultValue="65"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-700 font-bold mb-1">الكمية الافتتاحية *</label>
                  <input
                    name="quantity"
                    type="number"
                    required
                    defaultValue="50"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-bold mb-1">حد الطلب الأدنى *</label>
                  <input
                    name="minimumQuantity"
                    type="number"
                    required
                    defaultValue="5"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-700 font-bold mb-1">المورد</label>
                  <input
                    name="supplierName"
                    placeholder="شركة الأهرام للتوزيع"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-bold mb-1">تاريخ الصلاحية</label>
                  <input
                    name="expiryDate"
                    placeholder="12/2026"
                    className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">ملاحظات</label>
                <textarea
                  name="notes"
                  rows={2}
                  placeholder="ملاحظات مكان الحفظ أو العرض..."
                  className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddProductModal(false)}
                  className="px-4 py-2 border rounded-lg hover:bg-slate-50 text-slate-700 font-semibold"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-lg"
                >
                  حفظ الصنف وتسجيل الرصيد الافتتاحي
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Adjust Stock Quantity */}
      {adjustingProduct && (
        <div className="fixed inset-0 bg-slate-950/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full space-y-4 shadow-xl">
            <h3 className="font-bold text-base text-slate-900">تعديل كمية الصنف وتسجيل حركة المخزون</h3>
            <div className="p-3 bg-slate-50 rounded-lg text-xs space-y-1">
              <div><strong className="text-slate-800">الصنف:</strong> {adjustingProduct.name}</div>
              <div><strong className="text-slate-800">الكمية المسجلة حالياً:</strong> {adjustingProduct.quantity} قطعة</div>
            </div>

            <form onSubmit={handleAdjustQuantity} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1">الكمية الجديدة الفعلية *</label>
                <input
                  name="newQuantity"
                  type="number"
                  required
                  defaultValue={adjustingProduct.quantity}
                  className="w-full p-2.5 border border-slate-200 rounded-lg font-mono text-sm"
                />
              </div>

              <div>
                <label className="block text-slate-700 font-bold mb-1">سبب التعديل / ملاحظة الحركة *</label>
                <input
                  name="reason"
                  required
                  placeholder="مثال: تعديل جرد دوري، عجز، هالك..."
                  className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setAdjustingProduct(null)}
                  className="px-4 py-2 border rounded-lg text-slate-700 font-semibold"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-lg"
                >
                  تسجيل حركة ADJUSTMENT وتحديث المخزن
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Add Category */}
      {showAddCategoryModal && (
        <div className="fixed inset-0 bg-slate-950/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full space-y-4 shadow-xl">
            <h3 className="font-bold text-base text-slate-900">إضافة فئة جديدة</h3>
            <form onSubmit={handleAddCategory} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-700 font-bold mb-1">اسم الفئة *</label>
                <input
                  name="categoryName"
                  required
                  placeholder="مثال: مجمدات ومأكولات سريعة"
                  className="w-full p-2.5 border border-slate-200 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddCategoryModal(false)}
                  className="px-4 py-2 border rounded-lg text-slate-700 font-semibold"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white font-bold rounded-lg"
                >
                  إضافة الفئة
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Single Product Movements History */}
      {selectedProductHistory && (
        <div className="fixed inset-0 bg-slate-950/60 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl p-6 max-w-xl w-full max-h-[80vh] overflow-y-auto space-y-4 shadow-xl">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-base text-slate-900">
                سجل حركات المخزون: {selectedProductHistory.name}
              </h3>
              <button
                onClick={() => setSelectedProductHistory(null)}
                className="text-xs text-slate-500 hover:text-slate-800"
              >
                إغلاق
              </button>
            </div>

            <div className="space-y-2">
              {movements.filter(m => m.productId === selectedProductHistory.id).length === 0 ? (
                <p className="text-xs text-slate-500 text-center py-4">لا توجد حركات مسجلة لهذا الصنف بعد.</p>
              ) : (
                movements
                  .filter(m => m.productId === selectedProductHistory.id)
                  .map(m => (
                    <div key={m.id} className="p-3 bg-slate-50 rounded-lg border border-slate-100 text-xs space-y-1 font-mono">
                      <div className="flex items-center justify-between font-sans">
                        <span className="font-bold text-slate-900">
                          {m.movementType === 'INITIAL' ? 'إضافة افتتاحية' : 'تعديل جرد'}
                        </span>
                        <span className="text-[11px] text-slate-500">
                          {new Date(m.createdAt).toLocaleString('ar-EG')}
                        </span>
                      </div>
                      <div className="flex items-center gap-4 text-xs font-bold">
                        <span className={m.quantity >= 0 ? 'text-emerald-700' : 'text-red-700'}>
                          الكمية: {m.quantity >= 0 ? `+${m.quantity}` : m.quantity}
                        </span>
                        <span className="text-slate-600">قبل: {m.previousQuantity}</span>
                        <span className="text-slate-900">بعد: {m.newQuantity}</span>
                      </div>
                      {m.notes && <div className="text-[11px] font-sans text-slate-600">ملاحظة: {m.notes}</div>}
                    </div>
                  ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
