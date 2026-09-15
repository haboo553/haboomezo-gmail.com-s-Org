package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.SaleItemEntity
import com.markety.app.data.local.entity.SalesInvoiceEntity
import kotlinx.coroutines.flow.Flow

data class InvoiceWithItems(
    @Embedded val invoice: SalesInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<SaleItemEntity>
)

@Dao
interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: SalesInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM SalesInvoices ORDER BY dateTime DESC")
    fun getAllInvoices(): Flow<List<SalesInvoiceEntity>>

    @Query("""
        SELECT * FROM SalesInvoices 
        WHERE invoiceNumber LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%'
        ORDER BY dateTime DESC
    """)
    fun searchInvoices(query: String): Flow<List<SalesInvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM SalesInvoices WHERE id = :id LIMIT 1")
    fun getInvoiceWithItemsById(id: Long): Flow<InvoiceWithItems?>

    @Transaction
    @Query("SELECT * FROM SalesInvoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceWithItemsByIdDirect(id: Long): InvoiceWithItems?

    @Query("SELECT * FROM SalesInvoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): SalesInvoiceEntity?

    @Query("SELECT invoiceNumber FROM SalesInvoices WHERE invoiceNumber LIKE :prefix || '%' ORDER BY invoiceNumber DESC LIMIT 1")
    suspend fun getLatestInvoiceNumberForPrefix(prefix: String): String?

    // Dashboard Statistics (Real SQLite Aggregation queries)
    @Query("SELECT COUNT(*) FROM SalesInvoices WHERE dateTime >= :startOfDay AND dateTime <= :endOfDay")
    fun getTodayInvoicesCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM SalesInvoices WHERE dateTime >= :startOfDay AND dateTime <= :endOfDay")
    fun getTodaySalesTotal(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(paidAmount), 0.0) FROM SalesInvoices WHERE dateTime >= :startOfDay AND dateTime <= :endOfDay AND paymentType = 'CASH'")
    fun getTodayCashTotal(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM SalesInvoices WHERE dateTime >= :startOfDay AND dateTime <= :endOfDay AND paymentType = 'CREDIT'")
    fun getTodayCreditTotal(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(si.quantity), 0) 
        FROM SaleItems si
        INNER JOIN SalesInvoices inv ON si.invoiceId = inv.id
        WHERE inv.dateTime >= :startOfDay AND inv.dateTime <= :endOfDay
    """)
    fun getTodayItemsSoldCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    // Direct Synchronous Queries for Day Closing Transaction
    @Query("SELECT COUNT(*) FROM SalesInvoices WHERE dateTime >= :startTime AND dateTime <= :endTime")
    suspend fun getInvoicesCountBetweenDirect(startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM SalesInvoices WHERE dateTime >= :startTime AND dateTime <= :endTime")
    suspend fun getSalesTotalBetweenDirect(startTime: Long, endTime: Long): Double

    @Query("SELECT COALESCE(SUM(paidAmount), 0.0) FROM SalesInvoices WHERE dateTime >= :startTime AND dateTime <= :endTime AND paymentType = 'CASH'")
    suspend fun getCashTotalBetweenDirect(startTime: Long, endTime: Long): Double

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM SalesInvoices WHERE dateTime >= :startTime AND dateTime <= :endTime AND paymentType = 'CREDIT'")
    suspend fun getCreditTotalBetweenDirect(startTime: Long, endTime: Long): Double
}
