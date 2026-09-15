package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.InvoiceWithItems
import com.markety.app.data.local.dao.ProductDao
import com.markety.app.data.local.dao.SalesDao
import com.markety.app.data.local.dao.StockMovementDao
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class SaleItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitSellingPrice: Double
)

data class SaleResult(
    val invoice: SalesInvoiceEntity,
    val items: List<SaleItemEntity>,
    val change: Double
)

class SalesRepository(
    private val database: MarketyDatabase,
    private val salesDao: SalesDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao
) {

    fun getAllInvoices(): Flow<List<SalesInvoiceEntity>> = salesDao.getAllInvoices()

    fun searchInvoices(query: String): Flow<List<SalesInvoiceEntity>> = salesDao.searchInvoices(query)

    fun getInvoiceWithItems(id: Long): Flow<InvoiceWithItems?> = salesDao.getInvoiceWithItemsById(id)

    suspend fun getInvoiceWithItemsDirect(id: Long): InvoiceWithItems? = withContext(Dispatchers.IO) {
        salesDao.getInvoiceWithItemsByIdDirect(id)
    }

    /**
     * Executes atomic sale transaction inside SQLite:
     * 1. Validates products existence and active status.
     * 2. Validates available stock for each product.
     * 3. Generates unique sequential invoice number.
     * 4. Creates SalesInvoiceEntity.
     * 5. Creates SaleItemEntity snapshots.
     * 6. Decrements product stock quantity in Products table.
     * 7. Creates StockMovementEntity (SALE, -quantity, previous, new).
     * Any failure rolls back the entire transaction automatically.
     */
    suspend fun completeSale(
        items: List<SaleItemRequest>,
        discount: Double,
        paidAmount: Double,
        paymentType: PaymentType,
        notes: String? = null,
        customerId: Long? = null
    ): Result<SaleResult> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("السلة فارغة. يرجى إضافة أصناف أولاً."))
            }

            if (discount < 0) {
                return@withContext Result.failure(IllegalArgumentException("قيمة الخصم لا يمكن أن تكون سالبة."))
            }

            if (paidAmount < 0) {
                return@withContext Result.failure(IllegalArgumentException("المبلغ المدفوع لا يمكن أن يكون سالباً."))
            }

            val saleResult = database.withTransaction {
                // 1 & 2. Verify all products & stock
                val loadedProducts = mutableMapOf<Long, ProductEntity>()
                var subtotal = 0.0

                for (req in items) {
                    if (req.quantity <= 0) {
                        throw IllegalArgumentException("كمية البيع يجب أن تكون أكبر من صفر.")
                    }

                    val product = productDao.getProductById(req.productId)
                        ?: throw IllegalStateException("الصنف برقم ${req.productId} غير موجود في قاعدة البيانات.")

                    if (!product.isActive) {
                        throw IllegalStateException("الصنف «${product.name}» غير نشط أو محذوف.")
                    }

                    if (product.quantity < req.quantity) {
                        throw IllegalStateException("الكمية المطلوبة من «${product.name}» (${req.quantity}) أكبر من المخزون المتاح (${product.quantity}).")
                    }

                    loadedProducts[product.id] = product
                    subtotal += req.quantity * req.unitSellingPrice
                }

                if (discount > subtotal) {
                    throw IllegalArgumentException("قيمة الخصم ($discount ج.م) لا يمكن أن تتجاوز إجمالي الفاتورة ($subtotal ج.م).")
                }

                val total = (subtotal - discount).coerceAtLeast(0.0)

                var finalPaid = paidAmount
                var remainingAmount = 0.0
                var changeAmount = 0.0

                if (paymentType == PaymentType.CASH) {
                    if (finalPaid < total) {
                        // In cash, if paid is less than total, require full payment
                        throw IllegalArgumentException("في الدفع الكاش، يجب أن يغطي المبلغ المدفوع (${finalPaid} ج.م) كامل قيمة الفاتورة (${total} ج.م).")
                    }
                    changeAmount = (finalPaid - total).coerceAtLeast(0.0)
                    remainingAmount = 0.0
                } else {
                    // CREDIT (آجل)
                    if (finalPaid > total) {
                        changeAmount = finalPaid - total
                        remainingAmount = 0.0
                    } else {
                        remainingAmount = (total - finalPaid).coerceAtLeast(0.0)
                        changeAmount = 0.0
                    }
                }

                // 3. Generate unique sequential invoice number: MK-YYYYMMDD-XXXX
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val dateStr = dateFormat.format(Date())
                val prefix = "MK-$dateStr-"

                val latestInvoiceNum = salesDao.getLatestInvoiceNumberForPrefix(prefix)
                var nextSeq = 1
                if (latestInvoiceNum != null && latestInvoiceNum.startsWith(prefix)) {
                    val suffixStr = latestInvoiceNum.removePrefix(prefix)
                    val lastNum = suffixStr.toIntOrNull() ?: 0
                    nextSeq = lastNum + 1
                }

                var generatedInvoiceNumber = String.format(Locale.US, "MK-%s-%04d", dateStr, nextSeq)
                while (salesDao.getInvoiceByNumber(generatedInvoiceNumber) != null) {
                    nextSeq++
                    generatedInvoiceNumber = String.format(Locale.US, "MK-%s-%04d", dateStr, nextSeq)
                }

                val now = System.currentTimeMillis()

                // 4. Create SalesInvoice
                val invoiceEntity = SalesInvoiceEntity(
                    invoiceNumber = generatedInvoiceNumber,
                    dateTime = now,
                    subtotal = subtotal,
                    discount = discount,
                    total = total,
                    paidAmount = finalPaid,
                    remainingAmount = remainingAmount,
                    paymentType = paymentType,
                    customerId = customerId,
                    notes = notes,
                    status = InvoiceStatus.COMPLETED,
                    createdAt = now
                )

                val invoiceId = salesDao.insertInvoice(invoiceEntity)
                val persistedInvoice = invoiceEntity.copy(id = invoiceId)

                // 5, 6, 7 & 8. Create SaleItems, Deduct Stock, and Record StockMovement
                val createdSaleItems = mutableListOf<SaleItemEntity>()

                for (req in items) {
                    val product = loadedProducts[req.productId]!!
                    val itemTotal = req.quantity * req.unitSellingPrice

                    val saleItem = SaleItemEntity(
                        invoiceId = invoiceId,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        barcodeSnapshot = product.barcode,
                        quantity = req.quantity,
                        unitSellingPrice = req.unitSellingPrice,
                        discount = 0.0,
                        total = itemTotal
                    )
                    createdSaleItems.add(saleItem)

                    val previousQty = product.quantity
                    val newQty = previousQty - req.quantity

                    // Deduct from Products table
                    productDao.updateProduct(
                        product.copy(
                            quantity = newQty,
                            updatedAt = now
                        )
                    )

                    // Record StockMovement
                    val movement = StockMovementEntity(
                        productId = product.id,
                        movementType = MovementType.SALE,
                        quantity = -req.quantity,
                        previousQuantity = previousQty,
                        newQuantity = newQty,
                        purchasePrice = product.purchasePrice,
                        sellingPrice = req.unitSellingPrice,
                        referenceId = generatedInvoiceNumber,
                        notes = "فاتورة مبيعات رقم $generatedInvoiceNumber",
                        createdAt = now
                    )
                    stockMovementDao.insertMovement(movement)
                }

                salesDao.insertSaleItems(createdSaleItems)

                // Record Customer Credit Debt if remainingAmount > 0 and customerId is set
                if (remainingAmount > 0 && customerId != null) {
                    val customerTx = CustomerTransactionEntity(
                        customerId = customerId,
                        type = CustomerTransactionType.SALE,
                        amount = remainingAmount,
                        invoiceId = invoiceId,
                        notes = "فاتورة مبيعات آجلة رقم $generatedInvoiceNumber",
                        dateTime = now
                    )
                    database.customerDao().insertTransaction(customerTx)
                }

                SaleResult(
                    invoice = persistedInvoice,
                    items = createdSaleItems,
                    change = changeAmount
                )
            }

            Result.success(saleResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Real Dashboard Aggregation Queries
    fun getTodayInvoicesCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        salesDao.getTodayInvoicesCount(startOfDay, endOfDay)

    fun getTodaySalesTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        salesDao.getTodaySalesTotal(startOfDay, endOfDay)

    fun getTodayCashTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        salesDao.getTodayCashTotal(startOfDay, endOfDay)

    fun getTodayCreditTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        salesDao.getTodayCreditTotal(startOfDay, endOfDay)

    fun getTodayItemsSoldCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        salesDao.getTodayItemsSoldCount(startOfDay, endOfDay)
}
