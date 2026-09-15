package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.CustomerEntity
import com.markety.app.data.local.entity.CustomerTransactionEntity
import kotlinx.coroutines.flow.Flow

data class CustomerWithBalance(
    @Embedded val customer: CustomerEntity,
    val balance: Double = 0.0
)

data class TransactionWithInvoice(
    @Embedded val transaction: CustomerTransactionEntity,
    val invoiceNumber: String? = null
)

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchActiveCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("""
        SELECT 
            c.id AS id,
            c.name AS name,
            c.phone AS phone,
            c.address AS address,
            c.notes AS notes,
            c.createdAt AS createdAt,
            c.isActive AS isActive,
            COALESCE(SUM(CASE 
                WHEN t.type = 'SALE' THEN t.amount 
                WHEN t.type = 'PAYMENT' THEN -t.amount 
                WHEN t.type = 'RETURN' THEN -t.amount 
                ELSE 0.0 
            END), 0.0) AS balance
        FROM customers c
        LEFT JOIN customer_transactions t ON c.id = t.customerId
        WHERE c.isActive = 1
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun getCustomersWithBalance(): Flow<List<CustomerWithBalance>>

    @Query("""
        SELECT 
            c.id AS id,
            c.name AS name,
            c.phone AS phone,
            c.address AS address,
            c.notes AS notes,
            c.createdAt AS createdAt,
            c.isActive AS isActive,
            COALESCE(SUM(CASE 
                WHEN t.type = 'SALE' THEN t.amount 
                WHEN t.type = 'PAYMENT' THEN -t.amount 
                WHEN t.type = 'RETURN' THEN -t.amount 
                ELSE 0.0 
            END), 0.0) AS balance
        FROM customers c
        LEFT JOIN customer_transactions t ON c.id = t.customerId
        WHERE c.isActive = 1 AND (c.name LIKE '%' || :query || '%' OR c.phone LIKE '%' || :query || '%')
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun searchCustomersWithBalance(query: String): Flow<List<CustomerWithBalance>>

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN type = 'SALE' THEN amount 
            WHEN type = 'PAYMENT' THEN -amount 
            WHEN type = 'RETURN' THEN -amount 
            ELSE 0.0 
        END), 0.0)
        FROM customer_transactions
        WHERE customerId = :customerId
    """)
    fun getCustomerBalance(customerId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN type = 'SALE' THEN amount 
            WHEN type = 'PAYMENT' THEN -amount 
            WHEN type = 'RETURN' THEN -amount 
            ELSE 0.0 
        END), 0.0)
        FROM customer_transactions
        WHERE customerId = :customerId
    """)
    suspend fun getCustomerBalanceDirect(customerId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN t.type = 'SALE' THEN t.amount 
            WHEN t.type = 'PAYMENT' THEN -t.amount 
            WHEN t.type = 'RETURN' THEN -t.amount 
            ELSE 0.0 
        END), 0.0)
        FROM customer_transactions t
        INNER JOIN customers c ON t.customerId = c.id
        WHERE c.isActive = 1
    """)
    fun getTotalCustomerDebts(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransactionEntity): Long

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY dateTime DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerTransactionEntity>>

    @Query("""
        SELECT 
            t.id AS id,
            t.customerId AS customerId,
            t.type AS type,
            t.amount AS amount,
            t.invoiceId AS invoiceId,
            t.notes AS notes,
            t.dateTime AS dateTime,
            inv.invoiceNumber AS invoiceNumber
        FROM customer_transactions t
        LEFT JOIN SalesInvoices inv ON t.invoiceId = inv.id
        WHERE t.customerId = :customerId
        ORDER BY t.dateTime DESC
    """)
    fun getTransactionsWithInvoiceForCustomer(customerId: Long): Flow<List<TransactionWithInvoice>>
}
