import React, { useState, useMemo } from 'react';
import { Product, StockMovement, SalesInvoice, SaleItem, PaymentType } from '../types';
import {
  ShoppingCart,
  Plus,
  Minus,
  Trash2,
  Receipt,
  Search,
  CheckCircle2,
  AlertCircle,
  CreditCard,
  DollarSign,
  Package,
  X,
  Calendar,
  Layers
} from 'lucide-react';

interface LivePosViewProps {
  products: Product[];
  onCompleteSale: (
    updatedProducts: Product[],
    newMovements: StockMovement[],
    newInvoice: SalesInvoice,
    newSaleItems: SaleItem[]
  ) => void;
  invoices: SalesInvoice[];
  saleItems: SaleItem[];
}

interface CartItem {
  product: Product;
  quantity: number;
}

export const LivePosView: React.FC<LivePosViewProps> = ({
  products,
  onCompleteSale,
  invoices,
  saleItems
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'pos' | 'invoices'>('pos');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [discount, setDiscount] = useState<number>(0);
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH');
  const [paidAmountInput, setPaidAmountInput] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [selectedInvoice, setSelectedInvoice] = useState<SalesInvoice | null>(null);
  const [invoiceSearchQuery, setInvoiceSearchQuery] = useState('');
  const [lastSuccessInvoice, setLastSuccessInvoice] = useState<SalesInvoice | null>(null);

  // Active sellable products
  const activeProducts = useMemo(
    () => products.filter(p => p.isActive),
    [products]
  );

  // Search filtered products for quick selection
  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return activeProducts.slice(0, 8);
    return activeProducts.filter(
      p =>
        p.name.toLowerCase().includes(q) ||
        (p.barcode && p.barcode.toLowerCase().includes(q))
    );
  }, [activeProducts, searchQuery]);

  // Cart calculations
  const subtotal = useMemo(() => {
    return cart.reduce((sum, item) => sum + item.quantity * item.product.sellingPrice, 0);
  }, [cart]);

  const total = useMemo(() => {
    return Math.max(0, subtotal - discount);
  }, [subtotal, discount]);

  const paidAmount = useMemo(() => {
    const parsed = parseFloat(paidAmountInput);
    return isNaN(parsed) ? (paymentType === 'CASH' ? total : 0) : parsed;
  }, [paidAmountInput, paymentType, total]);

  const changeOrRemaining = useMemo(() => {
    if (paymentType === 'CASH') {
      return Math.max(0, paidAmount - total);
    } else {
      return Math.max(0, total - paidAmount);
    }
  }, [paymentType, paidAmount, total]);

  // Add to cart with stock limits
  const handleAddToCart = (product: Product) => {
    if (product.quantity <= 0) {
      alert(`المنتج "${product.name}" نفد من المخزون تماماً (الكمية = 0) ولا يمكن بيعه.`);
      return;
    }

    const existingIndex = cart.findIndex(c => c.product.id === product.id);
    if (existingIndex >= 0) {
      const currentQty = cart[existingIndex].quantity;
      if (currentQty >= product.quantity) {
        alert(`لا يمكن زيادة الكمية عن المخزون المتاح في المخزن (${product.quantity} قطعة).`);
        return;
      }
      const updated = [...cart];
      updated[existingIndex] = { ...updated[existingIndex], quantity: currentQty + 1 };
      setCart(updated);
    } else {
      setCart([...cart, { product, quantity: 1 }]);
    }
  };

  const handleUpdateQuantity = (productId: number, newQty: number) => {
    const item = cart.find(c => c.product.id === productId);
    if (!item) return;

    if (newQty <= 0) {
      setCart(cart.filter(c => c.product.id !== productId));
      return;
    }

    if (newQty > item.product.quantity) {
      alert(`الكمية المطلوبة تتجاوز المخزون المتاح (${item.product.quantity} قطعة).`);
      return;
    }

    setCart(cart.map(c => (c.product.id === productId ? { ...c, quantity: newQty } : c)));
  };

  const handleRemoveFromCart = (productId: number) => {
    setCart(cart.filter(c => c.product.id !== productId));
  };

  const handleClearCart = () => {
    if (cart.length === 0) return;
    if (confirm('هل أنت متأكد من رغبتك في إفراغ السلة الحالية؟')) {
      setCart([]);
      setDiscount(0);
      setPaidAmountInput('');
      setNotes('');
    }
  };

  // Complete Sale (Atomic Transaction Simulation)
  const handleCheckout = () => {
    if (cart.length === 0) {
      alert('سلة المشتريات فارغة.');
      return;
    }

    if (paymentType === 'CASH' && paidAmount < total) {
      alert(`في الدفع النقدي (كاش)، يجب أن يكون المدفوع (${paidAmount} ج.م) مساوياً أو أكبر من إجمالي الفاتورة (${total} ج.م).`);
      return;
    }

    // Step 1: Pre-validation of fresh stock
    for (const item of cart) {
      const freshProduct = products.find(p => p.id === item.product.id);
      if (!freshProduct || freshProduct.quantity < item.quantity) {
        alert(`فشلت المعاملة: الصنف "${item.product.name}" لم يعد يتوفر منه الكمية المطلوبة بالمخزن.`);
        return;
      }
    }

    // Step 2: Generate Unique Invoice Number (MK-YYYYMMDD-XXXX)
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const invoiceNumber = `MK-${year}${month}${day}-${randomSuffix}`;
    const invoiceId = Date.now();

    const remainingAmount = paymentType === 'CREDIT' ? Math.max(0, total - paidAmount) : 0;

    // Step 3: Create SalesInvoice entity
    const newInvoice: SalesInvoice = {
      id: invoiceId,
      invoiceNumber,
      dateTime: Date.now(),
      subtotal,
      discount,
      total,
      paidAmount,
      remainingAmount,
      paymentType,
      status: 'COMPLETED',
      notes: notes.trim() || undefined
    };

    // Step 4: Create SaleItems with Snapshots
    const newSaleItems: SaleItem[] = cart.map((item, idx) => ({
      id: invoiceId + idx + 1,
      invoiceId,
      productId: item.product.id,
      productNameSnapshot: item.product.name,
      barcodeSnapshot: item.product.barcode,
      unitCostPrice: item.product.purchasePrice,
      unitSellingPrice: item.product.sellingPrice,
      quantity: item.quantity,
      total: item.quantity * item.product.sellingPrice
    }));

    // Step 5: Update Products stock & generate StockMovement entities
    const newMovements: StockMovement[] = [];
    const updatedProducts = products.map(prod => {
      const cartItem = cart.find(c => c.product.id === prod.id);
      if (!cartItem) return prod;

      const previousQuantity = prod.quantity;
      const newQuantity = previousQuantity - cartItem.quantity;

      newMovements.push({
        id: Date.now() + Math.random(),
        productId: prod.id,
        movementType: 'SALE',
        quantity: -cartItem.quantity,
        previousQuantity,
        newQuantity,
        purchasePrice: prod.purchasePrice,
        sellingPrice: prod.sellingPrice,
        referenceId: invoiceNumber,
        notes: `فاتورة مبيعات رقم ${invoiceNumber}`,
        createdAt: Date.now()
      });

      return {
        ...prod,
        quantity: newQuantity,
        updatedAt: Date.now()
      };
    });

    // Execute atomic commit
    onCompleteSale(updatedProducts, newMovements, newInvoice, newSaleItems);

    // Reset POS form
    setCart([]);
    setDiscount(0);
    setPaidAmountInput('');
    setNotes('');
    setLastSuccessInvoice(newInvoice);
  };

  // Filtered invoices
  const filteredInvoices = useMemo(() => {
    const q = invoiceSearchQuery.trim().toLowerCase();
    if (!q) return invoices;
    return invoices.filter(inv => inv.invoiceNumber.toLowerCase().includes(q));
  }, [invoices, invoiceSearchQuery]);

  return (
    <div className="space-y-6 text-right" dir="rtl">
      {/* POS Sub Navigation */}
      <div className="flex items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-2xs">
        <div className="flex gap-3">
          <button
            onClick={() => setActiveSubTab('pos')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-2 ${
              activeSubTab === 'pos'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <ShoppingCart className="w-4 h-4" />
            <span>نقطة البيع (الكاشير والسلة)</span>
          </button>
          <button
            onClick={() => setActiveSubTab('invoices')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition flex items-center gap-2 ${
              activeSubTab === 'invoices'
                ? 'bg-sky-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Receipt className="w-4 h-4" />
            <span>سجل الفواتير والمبيعات ({invoices.length})</span>
          </button>
        </div>

        <div className="text-xs text-slate-500 hidden sm:block">
          معاملات ذرية • خصم فوري للمخزون • لقطة أسعار ثابتة
        </div>
      </div>

      {/* Success Notification Modal / Banner */}
      {lastSuccessInvoice && (
        <div className="bg-emerald-50 border border-emerald-200 p-4 rounded-xl flex items-center justify-between">
          <div className="flex items-center gap-3">
            <CheckCircle2 className="w-6 h-6 text-emerald-600" />
            <div>
              <div className="font-bold text-emerald-900 text-sm">
                تم إصدار الفاتورة بنجاح رقم: {lastSuccessInvoice.invoiceNumber}
              </div>
              <div className="text-xs text-emerald-700">
                الإجمالي: {lastSuccessInvoice.total.toFixed(2)} ج.م | تم خصم الكميات من المخزون وتسجيل حركات البيع.
              </div>
            </div>
          </div>
          <button
            onClick={() => setLastSuccessInvoice(null)}
            className="text-emerald-700 hover:text-emerald-900 text-xs font-bold px-3 py-1 bg-emerald-100 rounded-lg"
          >
            إغلاق
          </button>
        </div>
      )}

      {activeSubTab === 'pos' ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left / Main Section: Product catalog & quick selection (7 cols) */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs space-y-3">
              <div className="flex items-center gap-2 text-slate-800 font-bold text-sm">
                <Search className="w-4 h-4 text-emerald-600" />
                <span>البحث وإضافة الأصناف للسلة</span>
              </div>
              <div className="relative">
                <input
                  type="text"
                  placeholder="ابحث باسم المنتج أو الباركود..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-lg p-2.5 text-xs text-slate-900 pr-9 focus:ring-2 focus:ring-emerald-500 focus:outline-hidden"
                />
                <Search className="w-4 h-4 text-slate-400 absolute top-3 right-3" />
              </div>

              <div className="text-xs text-slate-500">
                اضغط على أي صنف لإضافته للسلة مباشرة (إذا كان موجوداً في السلة تزيد الكمية +1):
              </div>

              {filteredProducts.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  لا توجد أصناف مطابقة للبحث أو المخزون فارغ.
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 max-h-[360px] overflow-y-auto p-1">
                  {filteredProducts.map(product => {
                    const isOutOfStock = product.quantity <= 0;
                    return (
                      <button
                        key={product.id}
                        type="button"
                        disabled={isOutOfStock}
                        onClick={() => handleAddToCart(product)}
                        className={`p-3 rounded-xl border text-right transition flex flex-col justify-between ${
                          isOutOfStock
                            ? 'bg-slate-100 border-slate-200 opacity-60 cursor-not-allowed'
                            : 'bg-white hover:bg-emerald-50/50 hover:border-emerald-300 border-slate-200 shadow-2xs'
                        }`}
                      >
                        <div>
                          <div className="font-bold text-xs text-slate-900 truncate">
                            {product.name}
                          </div>
                          {product.barcode && (
                            <div className="text-[10px] text-slate-400 font-mono mt-0.5">
                              {product.barcode}
                            </div>
                          )}
                        </div>
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-xs font-black text-emerald-700">
                            {product.sellingPrice.toFixed(2)} ج.م
                          </span>
                          <span
                            className={`text-[10px] px-1.5 py-0.5 rounded-sm font-semibold ${
                              isOutOfStock
                                ? 'bg-red-100 text-red-700'
                                : product.quantity <= product.minimumQuantity
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            مخزون: {product.quantity}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Right Section: POS Cart & Checkout Panel (5 cols) */}
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200">
                <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                  <ShoppingCart className="w-5 h-5 text-emerald-600" />
                  <span>سلة المبيعات ({cart.length} أصناف)</span>
                </div>
                {cart.length > 0 && (
                  <button
                    onClick={handleClearCart}
                    className="text-xs text-red-600 hover:text-red-700 font-medium flex items-center gap-1"
                  >
                    <Trash2 className="w-3.5 h-3.5" /> إفراغ السلة
                  </button>
                )}
              </div>

              {/* Cart Items List */}
              <div className="space-y-2.5 max-h-[260px] overflow-y-auto">
                {cart.length === 0 ? (
                  <div className="text-center py-8 text-xs text-slate-400 space-y-1">
                    <ShoppingCart className="w-8 h-8 text-slate-300 mx-auto" />
                    <p>السلة فارغة حالياً</p>
                    <p className="text-[11px] text-slate-400">اختر من الأصناف أعلاه لإضافتها للسلة</p>
                  </div>
                ) : (
                  cart.map(item => (
                    <div
                      key={item.product.id}
                      className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-between gap-2"
                    >
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-xs text-slate-900 truncate">
                          {item.product.name}
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {item.product.sellingPrice.toFixed(2)} ج.م / للقطعة
                        </div>
                      </div>

                      {/* Quantity Controls */}
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={() => handleUpdateQuantity(item.product.id, item.quantity - 1)}
                          className="w-6 h-6 rounded-md bg-white border border-slate-300 flex items-center justify-center hover:bg-slate-100 text-slate-700"
                        >
                          <Minus className="w-3 h-3" />
                        </button>
                        <span className="font-bold text-xs text-slate-900 min-w-[24px] text-center font-mono">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => handleUpdateQuantity(item.product.id, item.quantity + 1)}
                          className="w-6 h-6 rounded-md bg-white border border-slate-300 flex items-center justify-center hover:bg-slate-100 text-slate-700"
                        >
                          <Plus className="w-3 h-3" />
                        </button>
                      </div>

                      {/* Item Total & Remove */}
                      <div className="text-left min-w-[65px]">
                        <div className="text-xs font-bold text-emerald-800">
                          {(item.quantity * item.product.sellingPrice).toFixed(2)} ج.م
                        </div>
                      </div>

                      <button
                        onClick={() => handleRemoveFromCart(item.product.id)}
                        className="text-slate-400 hover:text-red-600 p-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )}
              </div>

              {/* Financial Calculations */}
              <div className="border-t border-slate-200 pt-3 space-y-2 text-xs">
                <div className="flex items-center justify-between text-slate-600">
                  <span>الإجمالي قبل الخصم:</span>
                  <span className="font-mono font-bold text-slate-800">{subtotal.toFixed(2)} ج.م</span>
                </div>

                <div className="flex items-center justify-between gap-2">
                  <span className="text-slate-600">الخصم (ج.م):</span>
                  <input
                    type="number"
                    min="0"
                    max={subtotal}
                    value={discount || ''}
                    onChange={e => setDiscount(Math.max(0, parseFloat(e.target.value) || 0))}
                    placeholder="0.0"
                    className="w-24 bg-slate-50 border border-slate-300 rounded-md p-1 text-center font-mono text-xs focus:ring-1 focus:ring-emerald-500"
                  />
                </div>

                <div className="flex items-center justify-between text-sm font-extrabold text-slate-900 pt-1 border-t border-dashed border-slate-200">
                  <span>الإجمالي النهائي المطلوب:</span>
                  <span className="font-mono text-emerald-700 text-base">{total.toFixed(2)} ج.م</span>
                </div>
              </div>

              {/* Payment Type Selection */}
              <div className="border-t border-slate-200 pt-3 space-y-2.5 text-xs">
                <div className="font-bold text-slate-800">طريقة الدفع:</div>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentType('CASH');
                      setPaidAmountInput(total.toString());
                    }}
                    className={`py-2 px-3 rounded-lg border font-bold flex items-center justify-center gap-1.5 transition ${
                      paymentType === 'CASH'
                        ? 'bg-emerald-50 border-emerald-500 text-emerald-800'
                        : 'bg-slate-50 border-slate-200 text-slate-600'
                    }`}
                  >
                    <DollarSign className="w-4 h-4" /> كاش (نقدي)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentType('CREDIT');
                      setPaidAmountInput('0');
                    }}
                    className={`py-2 px-3 rounded-lg border font-bold flex items-center justify-center gap-1.5 transition ${
                      paymentType === 'CREDIT'
                        ? 'bg-amber-50 border-amber-500 text-amber-800'
                        : 'bg-slate-50 border-slate-200 text-slate-600'
                    }`}
                  >
                    <CreditCard className="w-4 h-4" /> آجل (شكك)
                  </button>
                </div>

                <div className="flex items-center justify-between gap-2 pt-1">
                  <span className="text-slate-600">المبلغ المدفوع (ج.م):</span>
                  <input
                    type="number"
                    min="0"
                    value={paidAmountInput}
                    onChange={e => setPaidAmountInput(e.target.value)}
                    placeholder={paymentType === 'CASH' ? total.toString() : '0.0'}
                    className="w-28 bg-slate-50 border border-slate-300 rounded-md p-1 text-center font-mono text-xs focus:ring-1 focus:ring-emerald-500 font-bold"
                  />
                </div>

                {paymentType === 'CASH' && paidAmount > total && (
                  <div className="flex items-center justify-between p-2 bg-emerald-50 rounded-lg text-emerald-800 font-bold">
                    <span>الباقي للعميل:</span>
                    <span className="font-mono">{changeOrRemaining.toFixed(2)} ج.م</span>
                  </div>
                )}

                {paymentType === 'CREDIT' && changeOrRemaining > 0 && (
                  <div className="flex items-center justify-between p-2 bg-amber-50 rounded-lg text-amber-800 font-bold">
                    <span>المتبقي على العميل (آجل):</span>
                    <span className="font-mono">{changeOrRemaining.toFixed(2)} ج.م</span>
                  </div>
                )}
              </div>

              {/* Checkout Button */}
              <button
                type="button"
                disabled={cart.length === 0}
                onClick={handleCheckout}
                className={`w-full py-3 rounded-xl font-bold text-sm text-white shadow-xs transition flex items-center justify-center gap-2 ${
                  cart.length === 0
                    ? 'bg-slate-300 cursor-not-allowed'
                    : 'bg-emerald-600 hover:bg-emerald-700 active:scale-98'
                }`}
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>إتمام عملية البيع ({total.toFixed(2)} ج.م)</span>
              </button>
            </div>
          </div>
        </div>
      ) : (
        /* Invoices List Tab */
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between gap-4">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="ابحث برقم الفاتورة (مثال: MK-)..."
                value={invoiceSearchQuery}
                onChange={e => setInvoiceSearchQuery(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-lg p-2.5 text-xs text-slate-900 pr-9 focus:ring-2 focus:ring-sky-500 focus:outline-hidden"
              />
              <Search className="w-4 h-4 text-slate-400 absolute top-3 right-3" />
            </div>
            <div className="text-xs text-slate-500">
              إجمالي الفواتير: <span className="font-bold text-slate-800">{invoices.length}</span>
            </div>
          </div>

          {filteredInvoices.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-xs">
              لا توجد فواتير مسجلة مطابقة للبحث.
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {filteredInvoices.map(inv => (
                <div
                  key={inv.id}
                  onClick={() => setSelectedInvoice(inv)}
                  className="p-3.5 hover:bg-slate-50 rounded-lg transition cursor-pointer flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-xs text-slate-900">{inv.invoiceNumber}</span>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                          inv.paymentType === 'CASH'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {inv.paymentType === 'CASH' ? 'نقدي (كاش)' : 'آجل (شكك)'}
                      </span>
                    </div>
                    <div className="text-[11px] text-slate-400">
                      {new Date(inv.dateTime).toLocaleString('ar-EG')}
                    </div>
                  </div>

                  <div className="flex items-center gap-4 text-xs font-mono">
                    <div>
                      <span className="text-slate-500 ml-1">الإجمالي:</span>
                      <span className="font-bold text-slate-900">{inv.total.toFixed(2)} ج.م</span>
                    </div>
                    {inv.paymentType === 'CREDIT' && inv.remainingAmount > 0 && (
                      <div className="text-amber-700 font-bold">
                        <span className="ml-1">المتبقي:</span>
                        <span>{inv.remainingAmount.toFixed(2)} ج.م</span>
                      </div>
                    )}
                    <span className="text-sky-600 font-sans text-xs">عرض التفاصيل &larr;</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Invoice Detail Modal */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <div>
                <div className="font-mono font-extrabold text-base text-slate-900">
                  {selectedInvoice.invoiceNumber}
                </div>
                <div className="text-xs text-slate-400">
                  {new Date(selectedInvoice.dateTime).toLocaleString('ar-EG')}
                </div>
              </div>
              <button
                onClick={() => setSelectedInvoice(null)}
                className="text-slate-400 hover:text-slate-700 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Snapshot Items Table */}
            <div className="space-y-2">
              <div className="text-xs font-bold text-slate-800">الأصناف المسجلة بالفاتورة (Snapshot):</div>
              <div className="bg-slate-50 rounded-xl p-3 border border-slate-200 space-y-2 text-xs">
                {saleItems
                  .filter(item => item.invoiceId === selectedInvoice.id)
                  .map(item => (
                    <div key={item.id} className="flex items-center justify-between pb-1.5 border-b border-slate-200/70 last:border-0">
                      <div>
                        <div className="font-bold text-slate-900">{item.productNameSnapshot}</div>
                        <div className="text-[10px] text-slate-400 font-mono">
                          {item.quantity} × {item.unitSellingPrice.toFixed(2)} ج.م
                        </div>
                      </div>
                      <div className="font-bold font-mono text-emerald-800">
                        {item.total.toFixed(2)} ج.م
                      </div>
                    </div>
                  ))}
              </div>
            </div>

            {/* Financial Summary */}
            <div className="p-3 bg-slate-100 rounded-xl space-y-1.5 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>الإجمالي قبل الخصم:</span>
                <span className="font-mono">{selectedInvoice.subtotal.toFixed(2)} ج.م</span>
              </div>
              {selectedInvoice.discount > 0 && (
                <div className="flex justify-between text-emerald-700 font-semibold">
                  <span>الخصم:</span>
                  <span className="font-mono">- {selectedInvoice.discount.toFixed(2)} ج.م</span>
                </div>
              )}
              <div className="flex justify-between text-sm font-bold text-slate-900 pt-1 border-t border-slate-200">
                <span>الإجمالي النهائي:</span>
                <span className="font-mono text-emerald-800">{selectedInvoice.total.toFixed(2)} ج.م</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>المدفوع:</span>
                <span className="font-mono">{selectedInvoice.paidAmount.toFixed(2)} ج.م</span>
              </div>
              {selectedInvoice.paymentType === 'CREDIT' && (
                <div className="flex justify-between text-amber-800 font-bold">
                  <span>المتبقي (شكك):</span>
                  <span className="font-mono">{selectedInvoice.remainingAmount.toFixed(2)} ج.م</span>
                </div>
              )}
            </div>

            <button
              onClick={() => setSelectedInvoice(null)}
              className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition"
            >
              إغلاق
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
