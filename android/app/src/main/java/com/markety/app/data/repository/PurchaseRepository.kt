package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.ProductDao
import com.markety.app.data.local.dao.PurchaseDao
import com.markety.app.data.local.dao.PurchaseInvoiceWithSupplierAndItems
import com.markety.app.data.local.dao.StockMovementDao
import com.markety.app.data.local.dao.SupplierDao
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class PurchaseItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitPurchasePrice: Double
)

data class PurchaseResult(
    val invoice: PurchaseInvoiceEntity,
    val items: List<PurchaseItemEntity>
)

class PurchaseRepository(
    private val database: MarketyDatabase,
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val supplierDao: SupplierDao
) {

    fun getAllPurchaseInvoices(): Flow<List<PurchaseInvoiceWithSupplierAndItems>> =
        purchaseDao.getAllPurchaseInvoices()

    suspend fun getPurchaseInvoiceByNumber(invoiceNumber: String): PurchaseInvoiceWithSupplierAndItems? =
        withContext(Dispatchers.IO) {
            purchaseDao.getPurchaseInvoiceByNumber(invoiceNumber)
        }

    fun getPurchasesTotalBetween(startTime: Long, endTime: Long): Flow<Double> =
        purchaseDao.getPurchasesTotalBetween(startTime, endTime)

    /**
     * Executes atomic purchase transaction:
     * 1. Validates products & quantities.
     * 2. Creates unique sequential purchase invoice number (PO-YYYYMMDD-XXXX).
     * 3. Increases product stock automatically: newQty = prevQty + quantity.
     * 4. Updates product purchasePrice if modified.
     * 5. Inserts StockMovementEntity (movementType = PURCHASE, quantity = +quantity).
     * 6. Inserts PurchaseInvoiceEntity and PurchaseItemEntity records.
     * 7. If supplierId != null and remainingAmount > 0, inserts SupplierTransactionEntity (PURCHASE).
     */
    suspend fun createPurchase(
        supplierId: Long?,
        items: List<PurchaseItemRequest>,
        paidAmount: Double,
        notes: String? = null
    ): Result<PurchaseResult> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("يجب إضافة صنف واحد على الأقل لفاتورة الشراء."))
            }

            val purchaseResult = database.withTransaction {
                val loadedProducts = mutableMapOf<Long, ProductEntity>()
                var total = 0.0

                for (req in items) {
                    if (req.quantity <= 0) {
                        throw IllegalArgumentException("كمية الشراء يجب أن تكون أكبر من صفر.")
                    }
                    if (req.unitPurchasePrice < 0) {
                        throw IllegalArgumentException("سعر الشراء لا يمكن أن يكون سالباً.")
                    }

                    val product = productDao.getProductById(req.productId)
                        ?: throw IllegalStateException("الصنف برقم ${req.productId} غير موجود.")

                    loadedProducts[product.id] = product
                    total += req.quantity * req.unitPurchasePrice
                }

                val finalPaid = paidAmount.coerceAtLeast(0.0)
                val remainingAmount = (total - finalPaid).coerceAtLeast(0.0)

                // Generate PO-YYYYMMDD-XXXX
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val dateStr = dateFormat.format(Date())
                val prefix = "PO-$dateStr-"

                val latestNum = purchaseDao.getLatestInvoiceNumberForPrefix(prefix)
                var nextSeq = 1
                if (latestNum != null && latestNum.startsWith(prefix)) {
                    val suffix = latestNum.removePrefix(prefix)
                    nextSeq = (suffix.toIntOrNull() ?: 0) + 1
                }

                var generatedNumber = String.format(Locale.US, "PO-%s-%04d", dateStr, nextSeq)
                while (purchaseDao.getPurchaseInvoiceByNumber(generatedNumber) != null) {
                    nextSeq++
                    generatedNumber = String.format(Locale.US, "PO-%s-%04d", dateStr, nextSeq)
                }

                val now = System.currentTimeMillis()

                val invoiceEntity = PurchaseInvoiceEntity(
                    invoiceNumber = generatedNumber,
                    supplierId = supplierId,
                    dateTime = now,
                    total = total,
                    paidAmount = finalPaid,
                    remainingAmount = remainingAmount,
                    notes = notes,
                    createdAt = now
                )

                val invoiceId = purchaseDao.insertInvoice(invoiceEntity)
                val persistedInvoice = invoiceEntity.copy(id = invoiceId)

                val createdItems = mutableListOf<PurchaseItemEntity>()

                for (req in items) {
                    val product = loadedProducts[req.productId]!!
                    val itemTotal = req.quantity * req.unitPurchasePrice

                    val purchaseItem = PurchaseItemEntity(
                        invoiceId = invoiceId,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        barcodeSnapshot = product.barcode,
                        quantity = req.quantity,
                        unitPurchasePrice = req.unitPurchasePrice,
                        total = itemTotal
                    )
                    createdItems.add(purchaseItem)

                    val previousQty = product.quantity
                    val newQty = previousQty + req.quantity

                    // Update product: increase quantity and update purchasePrice
                    productDao.updateProduct(
                        product.copy(
                            quantity = newQty,
                            purchasePrice = req.unitPurchasePrice,
                            updatedAt = now
                        )
                    )

                    // Record StockMovementEntity
                    val movement = StockMovementEntity(
                        productId = product.id,
                        movementType = MovementType.PURCHASE,
                        quantity = req.quantity,
                        previousQuantity = previousQty,
                        newQuantity = newQty,
                        purchasePrice = req.unitPurchasePrice,
                        sellingPrice = product.sellingPrice,
                        referenceId = generatedNumber,
                        notes = "فاتورة شراء رقم $generatedNumber",
                        createdAt = now
                    )
                    stockMovementDao.insertMovement(movement)
                }

                purchaseDao.insertPurchaseItems(createdItems)

                // Update supplier debt if remaining amount > 0 and supplierId is provided
                if (supplierId != null && remainingAmount > 0) {
                    val supplierTx = SupplierTransactionEntity(
                        supplierId = supplierId,
                        type = SupplierTransactionType.PURCHASE,
                        amount = remainingAmount,
                        purchaseInvoiceId = invoiceId,
                        notes = "مستحقات فاتورة شراء رقم $generatedNumber",
                        dateTime = now
                    )
                    supplierDao.insertTransaction(supplierTx)
                }

                PurchaseResult(
                    invoice = persistedInvoice,
                    items = createdItems
                )
            }

            Result.success(purchaseResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
