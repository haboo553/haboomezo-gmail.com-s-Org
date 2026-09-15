package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.PurchaseInvoiceEntity
import com.markety.app.data.local.entity.PurchaseItemEntity
import kotlinx.coroutines.flow.Flow

data class PurchaseInvoiceWithSupplierAndItems(
    @Embedded val invoice: PurchaseInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<PurchaseItemEntity>
)

@Dao
interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: PurchaseInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Transaction
    @Query("SELECT * FROM purchase_invoices ORDER BY dateTime DESC")
    fun getAllPurchaseInvoices(): Flow<List<PurchaseInvoiceWithSupplierAndItems>>

    @Transaction
    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getPurchaseInvoiceByIdDirect(id: Long): PurchaseInvoiceWithSupplierAndItems?

    @Transaction
    @Query("SELECT * FROM purchase_invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun getPurchaseInvoiceByNumber(invoiceNumber: String): PurchaseInvoiceWithSupplierAndItems?

    @Query("SELECT invoiceNumber FROM purchase_invoices WHERE invoiceNumber LIKE :prefix || '%' ORDER BY invoiceNumber DESC LIMIT 1")
    suspend fun getLatestInvoiceNumberForPrefix(prefix: String): String?

    @Query("SELECT COALESCE(SUM(total), 0.0) FROM purchase_invoices WHERE dateTime BETWEEN :startTime AND :endTime")
    fun getPurchasesTotalBetween(startTime: Long, endTime: Long): Flow<Double>
}
