package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.local.entity.SupplierTransactionEntity
import kotlinx.coroutines.flow.Flow

data class SupplierWithBalance(
    @Embedded val supplier: SupplierEntity,
    val balance: Double = 0.0
)

@Dao
interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET isActive = 0 WHERE id = :id")
    suspend fun softDeleteSupplier(id: Long)

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun getSupplierByIdFlow(id: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchActiveSuppliers(query: String): Flow<List<SupplierEntity>>

    @Query("""
        SELECT 
            s.id AS id,
            s.name AS name,
            s.phone AS phone,
            s.address AS address,
            s.notes AS notes,
            s.createdAt AS createdAt,
            s.isActive AS isActive,
            COALESCE(SUM(CASE 
                WHEN t.type = 'PURCHASE' THEN t.amount 
                WHEN t.type = 'PAYMENT' THEN -t.amount 
                WHEN t.type = 'RETURN' THEN -t.amount 
                ELSE 0.0 
            END), 0.0) AS balance
        FROM suppliers s
        LEFT JOIN supplier_transactions t ON s.id = t.supplierId
        WHERE s.isActive = 1
        GROUP BY s.id
        ORDER BY s.name ASC
    """)
    fun getSuppliersWithBalance(): Flow<List<SupplierWithBalance>>

    @Query("""
        SELECT 
            s.id AS id,
            s.name AS name,
            s.phone AS phone,
            s.address AS address,
            s.notes AS notes,
            s.createdAt AS createdAt,
            s.isActive AS isActive,
            COALESCE(SUM(CASE 
                WHEN t.type = 'PURCHASE' THEN t.amount 
                WHEN t.type = 'PAYMENT' THEN -t.amount 
                WHEN t.type = 'RETURN' THEN -t.amount 
                ELSE 0.0 
            END), 0.0) AS balance
        FROM suppliers s
        LEFT JOIN supplier_transactions t ON s.id = t.supplierId
        WHERE s.isActive = 1 AND (s.name LIKE '%' || :query || '%' OR s.phone LIKE '%' || :query || '%')
        GROUP BY s.id
        ORDER BY s.name ASC
    """)
    fun searchSuppliersWithBalance(query: String): Flow<List<SupplierWithBalance>>

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN type = 'PURCHASE' THEN amount 
            WHEN type = 'PAYMENT' THEN -amount 
            WHEN type = 'RETURN' THEN -amount 
            ELSE 0.0 
        END), 0.0)
        FROM supplier_transactions
        WHERE supplierId = :supplierId
    """)
    fun getSupplierBalance(supplierId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN type = 'PURCHASE' THEN amount 
            WHEN type = 'PAYMENT' THEN -amount 
            WHEN type = 'RETURN' THEN -amount 
            ELSE 0.0 
        END), 0.0)
        FROM supplier_transactions
        WHERE supplierId = :supplierId
    """)
    suspend fun getSupplierBalanceDirect(supplierId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(CASE 
            WHEN t.type = 'PURCHASE' THEN t.amount 
            WHEN t.type = 'PAYMENT' THEN -t.amount 
            WHEN t.type = 'RETURN' THEN -t.amount 
            ELSE 0.0 
        END), 0.0)
        FROM supplier_transactions t
        INNER JOIN suppliers s ON t.supplierId = s.id
        WHERE s.isActive = 1
    """)
    fun getTotalSupplierDebts(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SupplierTransactionEntity): Long

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY dateTime DESC")
    fun getTransactionsForSupplier(supplierId: Long): Flow<List<SupplierTransactionEntity>>
}
