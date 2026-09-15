package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.CustomerDao
import com.markety.app.data.local.dao.CustomerWithBalance
import com.markety.app.data.local.dao.TransactionWithInvoice
import com.markety.app.data.local.entity.CustomerEntity
import com.markety.app.data.local.entity.CustomerTransactionEntity
import com.markety.app.data.local.entity.CustomerTransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val database: MarketyDatabase,
    private val customerDao: CustomerDao
) {

    fun getAllActiveCustomers(): Flow<List<CustomerEntity>> =
        customerDao.getAllActiveCustomers()

    fun searchActiveCustomers(query: String): Flow<List<CustomerEntity>> =
        customerDao.searchActiveCustomers(query.trim())

    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>> =
        customerDao.getCustomersWithBalance()

    fun searchCustomersWithBalance(query: String): Flow<List<CustomerWithBalance>> =
        customerDao.searchCustomersWithBalance(query.trim())

    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?> =
        customerDao.getCustomerByIdFlow(id)

    suspend fun getCustomerById(id: Long): CustomerEntity? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    fun getCustomerBalance(customerId: Long): Flow<Double> =
        customerDao.getCustomerBalance(customerId)

    suspend fun getCustomerBalanceDirect(customerId: Long): Double = withContext(Dispatchers.IO) {
        customerDao.getCustomerBalanceDirect(customerId)
    }

    fun getTotalCustomerDebts(): Flow<Double> =
        customerDao.getTotalCustomerDebts()

    fun getTransactionsWithInvoice(customerId: Long): Flow<List<TransactionWithInvoice>> =
        customerDao.getTransactionsWithInvoiceForCustomer(customerId)

    suspend fun addCustomer(
        name: String,
        phone: String,
        address: String = "",
        notes: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("اسم العميل مطلوب ولا يمكن تركه فارغاً."))
            }

            val entity = CustomerEntity(
                name = trimmedName,
                phone = phone.trim(),
                address = address.trim(),
                notes = notes?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
                isActive = true
            )

            val id = customerDao.insertCustomer(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(customer: CustomerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = customer.name.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("اسم العميل مطلوب."))
            }

            customerDao.updateCustomer(
                customer.copy(
                    name = trimmedName,
                    phone = customer.phone.trim(),
                    address = customer.address.trim(),
                    notes = customer.notes?.trim()?.ifBlank { null }
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteCustomer(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val customer = customerDao.getCustomerById(id)
                ?: return@withContext Result.failure(IllegalStateException("العميل غير موجود."))

            customerDao.softDeleteCustomer(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes atomic payment collection inside SQLite transaction:
     * 1. Validates customer existence and active status.
     * 2. Validates amount > 0.
     * 3. Inserts CustomerTransactionEntity with type PAYMENT.
     */
    suspend fun collectPayment(
        customerId: Long,
        amount: Double,
        notes: String? = null
    ): Result<CustomerTransactionEntity> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("مبلغ التحصيل يجب أن يكون أكبر من صفر."))
            }

            val customer = customerDao.getCustomerById(customerId)
                ?: return@withContext Result.failure(IllegalStateException("العميل غير موجود أو محذوف."))

            if (!customer.isActive) {
                return@withContext Result.failure(IllegalStateException("لا يمكن تسجيل دفعة لعميل غير نشط."))
            }

            val transaction = database.withTransaction {
                val now = System.currentTimeMillis()
                val paymentTx = CustomerTransactionEntity(
                    customerId = customerId,
                    type = CustomerTransactionType.PAYMENT,
                    amount = amount,
                    invoiceId = null,
                    notes = notes?.trim()?.ifBlank { null } ?: "تحصيل دفعة نقدية",
                    dateTime = now
                )

                val id = customerDao.insertTransaction(paymentTx)
                paymentTx.copy(id = id)
            }

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
