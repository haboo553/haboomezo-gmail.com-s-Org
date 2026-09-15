package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.SupplierDao
import com.markety.app.data.local.dao.SupplierWithBalance
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.local.entity.SupplierTransactionEntity
import com.markety.app.data.local.entity.SupplierTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SupplierRepository(
    private val database: MarketyDatabase,
    private val supplierDao: SupplierDao
) {

    fun getAllActiveSuppliers(): Flow<List<SupplierEntity>> =
        supplierDao.getAllActiveSuppliers()

    fun searchActiveSuppliers(query: String): Flow<List<SupplierEntity>> =
        supplierDao.searchActiveSuppliers(query.trim())

    fun getSuppliersWithBalance(): Flow<List<SupplierWithBalance>> =
        supplierDao.getSuppliersWithBalance()

    fun searchSuppliersWithBalance(query: String): Flow<List<SupplierWithBalance>> =
        supplierDao.searchSuppliersWithBalance(query.trim())

    fun getSupplierByIdFlow(id: Long): Flow<SupplierEntity?> =
        supplierDao.getSupplierByIdFlow(id)

    suspend fun getSupplierById(id: Long): SupplierEntity? = withContext(Dispatchers.IO) {
        supplierDao.getSupplierById(id)
    }

    fun getSupplierBalance(supplierId: Long): Flow<Double> =
        supplierDao.getSupplierBalance(supplierId)

    suspend fun getSupplierBalanceDirect(supplierId: Long): Double = withContext(Dispatchers.IO) {
        supplierDao.getSupplierBalanceDirect(supplierId)
    }

    fun getTotalSupplierDebts(): Flow<Double> =
        supplierDao.getTotalSupplierDebts()

    fun getTransactionsForSupplier(supplierId: Long): Flow<List<SupplierTransactionEntity>> =
        supplierDao.getTransactionsForSupplier(supplierId)

    suspend fun addSupplier(
        name: String,
        phone: String,
        address: String = "",
        notes: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("اسم المورد مطلوب."))
            }

            val entity = SupplierEntity(
                name = trimmedName,
                phone = phone.trim(),
                address = address.trim(),
                notes = notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
                isActive = true
            )

            val id = supplierDao.insertSupplier(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSupplier(supplier: SupplierEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = supplier.name.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("اسم المورد مطلوب."))
            }

            supplierDao.updateSupplier(
                supplier.copy(
                    name = trimmedName,
                    phone = supplier.phone.trim(),
                    address = supplier.address.trim(),
                    notes = supplier.notes?.trim()?.ifBlank { null }
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteSupplier(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supplier = supplierDao.getSupplierById(id)
                ?: return@withContext Result.failure(IllegalStateException("المورد غير موجود."))

            supplierDao.softDeleteSupplier(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Records a payment to supplier atomically.
     */
    suspend fun paySupplier(
        supplierId: Long,
        amount: Double,
        notes: String? = null
    ): Result<SupplierTransactionEntity> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("مبلغ السداد يجب أن يكون أكبر من صفر."))
            }

            val supplier = supplierDao.getSupplierById(supplierId)
                ?: return@withContext Result.failure(IllegalStateException("المورد غير موجود."))

            if (!supplier.isActive) {
                return@withContext Result.failure(IllegalStateException("المورد غير نشط."))
            }

            val transaction = database.withTransaction {
                val now = System.currentTimeMillis()
                val paymentTx = SupplierTransactionEntity(
                    supplierId = supplierId,
                    type = SupplierTransactionType.PAYMENT,
                    amount = amount,
                    purchaseInvoiceId = null,
                    notes = notes?.trim()?.ifBlank { null } ?: "سداد دفعة للمورد",
                    dateTime = now
                )

                val id = supplierDao.insertTransaction(paymentTx)
                paymentTx.copy(id = id)
            }

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
