package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_invoices",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["dateTime"])
    ]
)
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val supplierId: Long?,
    val dateTime: Long = System.currentTimeMillis(),
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
