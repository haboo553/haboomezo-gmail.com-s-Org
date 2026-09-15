export interface Category {
  id: number;
  name: string;
  createdAt: number;
}

export type MovementType =
  | 'INITIAL'
  | 'PURCHASE'
  | 'SALE'
  | 'RETURN'
  | 'DAMAGE'
  | 'ADJUSTMENT'
  | 'INVENTORY';

export interface StockMovement {
  id: number;
  productId: number;
  movementType: MovementType;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  purchasePrice: number;
  sellingPrice: number;
  referenceId?: string;
  notes?: string;
  createdAt: number;
}

export interface Product {
  id: number;
  name: string;
  barcode?: string;
  imagePath?: string;
  categoryId: number;
  purchasePrice: number;
  sellingPrice: number;
  quantity: number;
  minimumQuantity: number;
  expiryDate?: string;
  supplierName?: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
  isActive: boolean;
}

export type PaymentType = 'CASH' | 'CREDIT';
export type InvoiceStatus = 'COMPLETED' | 'CANCELLED';

export interface SalesInvoice {
  id: number;
  invoiceNumber: string;
  dateTime: number;
  subtotal: number;
  discount: number;
  total: number;
  paidAmount: number;
  remainingAmount: number;
  paymentType: PaymentType;
  status: InvoiceStatus;
  notes?: string;
}

export interface SaleItem {
  id: number;
  invoiceId: number;
  productId: number;
  productNameSnapshot: string;
  barcodeSnapshot?: string;
  unitCostPrice: number;
  unitSellingPrice: number;
  quantity: number;
  total: number;
}

export type DailyOperationStatus = 'OPEN' | 'CLOSED';

export interface DailyOperation {
  id: number;
  date: string;
  openingCash: number;
  totalSales: number;
  cashSales: number;
  creditSales: number;
  expenses: number;
  netSales: number;
  closingCash: number;
  actualCash: number;
  difference: number;
  status: DailyOperationStatus;
  openedAt: number;
  closedAt?: number;
  notes?: string;
}

export interface Expense {
  id: number;
  dailyOperationId: number;
  category: string;
  amount: number;
  notes?: string;
  date: number;
  createdAt: number;
}

export interface ProjectFile {
  path: string;
  category: 'Entity' | 'DAO' | 'Database' | 'Repository' | 'ViewModel' | 'Screen' | 'Navigation' | 'Config' | 'Test';
  description: string;
  code: string;
}
