package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplier_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["dateTime"]),
        Index(value = ["type"])
    ]
)
data class SupplierTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    val type: SupplierTransactionType,
    val amount: Double,
    val purchaseInvoiceId: Long? = null,
    val notes: String? = null,
    val dateTime: Long = System.currentTimeMillis()
)
