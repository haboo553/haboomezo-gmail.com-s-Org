package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.*
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class ReturnItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double
)

data class ReturnResult(
    val returnInvoice: ReturnInvoiceEntity,
    val items: List<ReturnItemEntity>
)

class ReturnRepository(
    private val database: MarketyDatabase,
    private val returnDao: ReturnDao,
    private val salesDao: SalesDao,
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao
) {

    fun getAllReturnInvoices(): Flow<List<ReturnInvoiceWithItems>> =
        returnDao.getAllReturnInvoices()

    /**
     * Process a Sales Return:
     * - Checks original invoice and sold items.
     * - Prevents returning quantity greater than sold.
     * - Increases product stock.
     * - Records StockMovementEntity (+quantity, RETURN).
     * - Adjusts customer debt if customerId was on invoice.
     */
    suspend fun processSalesReturn(
        invoiceNumber: String,
        itemsToReturn: List<ReturnItemRequest>,
        reason: String? = null
    ): Result<ReturnResult> = withContext(Dispatchers.IO) {
        try {
            if (itemsToReturn.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("يجب اختيار صنف واحد على الأقل للإرجاع."))
            }

            val result = database.withTransaction {
                val invoiceWithItems = salesDao.getInvoiceWithItemsByNumber(invoiceNumber)
                    ?: throw IllegalStateException("فاتورة المبيعات $invoiceNumber غير موجودة.")

                val originalItemsMap = invoiceWithItems.items.associateBy { it.productId }

                var totalReturnAmount = 0.0

                // Validate each return request against sold quantity
                for (req in itemsToReturn) {
                    if (req.quantity <= 0) {
                        throw IllegalArgumentException("كمية المرتجع يجب أن تكون أكبر من صفر.")
                    }
                    val originalItem = originalItemsMap[req.productId]
                        ?: throw IllegalArgumentException("الصنف برقم ${req.productId} لم يتم بيعه في هذه الفاتورة.")

                    if (req.quantity > originalItem.quantity) {
                        throw IllegalArgumentException("لا يمكن إرجاع كمية (${req.quantity}) أكبر من الكمية المباعة الأصلية (${originalItem.quantity}) للصنف ${originalItem.productNameSnapshot}.")
                    }

                    totalReturnAmount += req.quantity * req.unitPrice
                }

                // Generate RTN-YYYYMMDD-XXXX
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val dateStr = dateFormat.format(Date())
                val prefix = "RTN-S-$dateStr-"

                val latestNum = returnDao.getLatestReturnNumberForPrefix(prefix)
                var nextSeq = 1
                if (latestNum != null && latestNum.startsWith(prefix)) {
                    val suffix = latestNum.removePrefix(prefix)
                    nextSeq = (suffix.toIntOrNull() ?: 0) + 1
                }
                val returnNumber = String.format(Locale.US, "RTN-S-%s-%04d", dateStr, nextSeq)

                val now = System.currentTimeMillis()

                val returnInvoice = ReturnInvoiceEntity(
                    returnNumber = returnNumber,
                    type = ReturnType.SALES_RETURN,
                    originalInvoiceNumber = invoiceNumber,
                    customerOrSupplierId = invoiceWithItems.invoice.customerId,
                    totalAmount = totalReturnAmount,
                    reason = reason,
                    dateTime = now
                )

                val returnInvoiceId = returnDao.insertReturnInvoice(returnInvoice)
                val persistedReturn = returnInvoice.copy(id = returnInvoiceId)

                val returnItemEntities = mutableListOf<ReturnItemEntity>()

                for (req in itemsToReturn) {
                    val originalItem = originalItemsMap[req.productId]!!
                    val product = productDao.getProductById(req.productId)
                        ?: throw IllegalStateException("الصنف غير موجود في قاعدة البيانات.")

                    val itemTotal = req.quantity * req.unitPrice
                    val itemEntity = ReturnItemEntity(
                        returnInvoiceId = returnInvoiceId,
                        productId = req.productId,
                        productNameSnapshot = originalItem.productNameSnapshot,
                        quantity = req.quantity,
                        unitPrice = req.unitPrice,
                        total = itemTotal
                    )
                    returnItemEntities.add(itemEntity)

                    // Increase stock back
                    val prevQty = product.quantity
                    val newQty = prevQty + req.quantity

                    productDao.updateProduct(
                        product.copy(
                            quantity = newQty,
                            updatedAt = now
                        )
                    )

                    // Record StockMovement
                    val movement = StockMovementEntity(
                        productId = product.id,
                        movementType = MovementType.RETURN,
                        quantity = req.quantity,
                        previousQuantity = prevQty,
                        newQuantity = newQty,
                        purchasePrice = product.purchasePrice,
                        sellingPrice = req.unitPrice,
                        referenceId = returnNumber,
                        notes = "مرتجع مبيعات للفاتورة $invoiceNumber",
                        createdAt = now
                    )
                    stockMovementDao.insertMovement(movement)
                }

                returnDao.insertReturnItems(returnItemEntities)

                // If customer account was associated, deduct customer debt
                val customerId = invoiceWithItems.invoice.customerId
                if (customerId != null) {
                    val customerTx = CustomerTransactionEntity(
                        customerId = customerId,
                        type = CustomerTransactionType.RETURN,
                        amount = totalReturnAmount,
                        invoiceId = invoiceWithItems.invoice.id,
                        notes = "مرتجع مبيعات إشعار رقم $returnNumber",
                        dateTime = now
                    )
                    customerDao.insertTransaction(customerTx)
                }

                ReturnResult(
                    returnInvoice = persistedReturn,
                    items = returnItemEntities
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Process a Purchase Return:
     * - Checks original purchase invoice.
     * - Prevents returning quantity greater than purchased.
     * - Ensures current inventory has enough quantity.
     * - Decreases product stock.
     * - Records StockMovementEntity (-quantity, RETURN).
     * - Adjusts supplier balance.
     */
    suspend fun processPurchaseReturn(
        purchaseInvoiceNumber: String,
        itemsToReturn: List<ReturnItemRequest>,
        reason: String? = null
    ): Result<ReturnResult> = withContext(Dispatchers.IO) {
        try {
            if (itemsToReturn.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("يجب اختيار صنف واحد على الأقل للإرجاع."))
            }

            val result = database.withTransaction {
                val invoiceWithItems = purchaseDao.getPurchaseInvoiceByNumber(purchaseInvoiceNumber)
                    ?: throw IllegalStateException("فاتورة الشراء $purchaseInvoiceNumber غير موجودة.")

                val originalItemsMap = invoiceWithItems.items.associateBy { it.productId }

                var totalReturnAmount = 0.0

                for (req in itemsToReturn) {
                    if (req.quantity <= 0) {
                        throw IllegalArgumentException("كمية المرتجع يجب أن تكون أكبر من صفر.")
                    }
                    val originalItem = originalItemsMap[req.productId]
                        ?: throw IllegalArgumentException("الصنف برقم ${req.productId} لم يتم شراؤه في هذه الفاتورة.")

                    if (req.quantity > originalItem.quantity) {
                        throw IllegalArgumentException("لا يمكن إرجاع كمية (${req.quantity}) أكبر من الكمية المشتراة الأصلية (${originalItem.quantity}) للصنف ${originalItem.productNameSnapshot}.")
                    }

                    val product = productDao.getProductById(req.productId)
                        ?: throw IllegalStateException("الصنف غير موجود.")

                    if (product.quantity < req.quantity) {
                        throw IllegalStateException("المخزون الحالي (${product.quantity}) لا يكفي لإرجاع كمية (${req.quantity}) من الصنف ${product.name}.")
                    }

                    totalReturnAmount += req.quantity * req.unitPrice
                }

                // Generate RTN-P-YYYYMMDD-XXXX
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val dateStr = dateFormat.format(Date())
                val prefix = "RTN-P-$dateStr-"

                val latestNum = returnDao.getLatestReturnNumberForPrefix(prefix)
                var nextSeq = 1
                if (latestNum != null && latestNum.startsWith(prefix)) {
                    val suffix = latestNum.removePrefix(prefix)
                    nextSeq = (suffix.toIntOrNull() ?: 0) + 1
                }
                val returnNumber = String.format(Locale.US, "RTN-P-%s-%04d", dateStr, nextSeq)

                val now = System.currentTimeMillis()

                val returnInvoice = ReturnInvoiceEntity(
                    returnNumber = returnNumber,
                    type = ReturnType.PURCHASE_RETURN,
                    originalInvoiceNumber = purchaseInvoiceNumber,
                    customerOrSupplierId = invoiceWithItems.invoice.supplierId,
                    totalAmount = totalReturnAmount,
                    reason = reason,
                    dateTime = now
                )

                val returnInvoiceId = returnDao.insertReturnInvoice(returnInvoice)
                val persistedReturn = returnInvoice.copy(id = returnInvoiceId)

                val returnItemEntities = mutableListOf<ReturnItemEntity>()

                for (req in itemsToReturn) {
                    val originalItem = originalItemsMap[req.productId]!!
                    val product = productDao.getProductById(req.productId)!!

                    val itemTotal = req.quantity * req.unitPrice
                    val itemEntity = ReturnItemEntity(
                        returnInvoiceId = returnInvoiceId,
                        productId = req.productId,
                        productNameSnapshot = originalItem.productNameSnapshot,
                        quantity = req.quantity,
                        unitPrice = req.unitPrice,
                        total = itemTotal
                    )
                    returnItemEntities.add(itemEntity)

                    // Deduct stock
                    val prevQty = product.quantity
                    val newQty = prevQty - req.quantity

                    productDao.updateProduct(
                        product.copy(
                            quantity = newQty,
                            updatedAt = now
                        )
                    )

                    // Record StockMovement (-quantity)
                    val movement = StockMovementEntity(
                        productId = product.id,
                        movementType = MovementType.RETURN,
                        quantity = -req.quantity,
                        previousQuantity = prevQty,
                        newQuantity = newQty,
                        purchasePrice = req.unitPrice,
                        sellingPrice = product.sellingPrice,
                        referenceId = returnNumber,
                        notes = "مرتجع مشتريات للفاتورة $purchaseInvoiceNumber",
                        createdAt = now
                    )
                    stockMovementDao.insertMovement(movement)
                }

                returnDao.insertReturnItems(returnItemEntities)

                // Adjust supplier balance
                val supplierId = invoiceWithItems.invoice.supplierId
                if (supplierId != null) {
                    val supplierTx = SupplierTransactionEntity(
                        supplierId = supplierId,
                        type = SupplierTransactionType.RETURN,
                        amount = totalReturnAmount,
                        purchaseInvoiceId = invoiceWithItems.invoice.id,
                        notes = "مرتجع مشتريات إشعار رقم $returnNumber",
                        dateTime = now
                    )
                    supplierDao.insertTransaction(supplierTx)
                }

                ReturnResult(
                    returnInvoice = persistedReturn,
                    items = returnItemEntities
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
