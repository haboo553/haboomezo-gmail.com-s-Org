package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.ReturnInvoiceEntity
import com.markety.app.data.local.entity.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

data class ReturnInvoiceWithItems(
    @Embedded val returnInvoice: ReturnInvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "returnInvoiceId"
    )
    val items: List<ReturnItemEntity>
)

@Dao
interface ReturnDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnInvoice(invoice: ReturnInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    @Transaction
    @Query("SELECT * FROM return_invoices ORDER BY dateTime DESC")
    fun getAllReturnInvoices(): Flow<List<ReturnInvoiceWithItems>>

    @Query("SELECT returnNumber FROM return_invoices WHERE returnNumber LIKE :prefix || '%' ORDER BY returnNumber DESC LIMIT 1")
    suspend fun getLatestReturnNumberForPrefix(prefix: String): String?
}
