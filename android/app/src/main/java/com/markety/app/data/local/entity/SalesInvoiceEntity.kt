package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "SalesInvoices",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["dateTime"]),
        Index(value = ["paymentType"]),
        Index(value = ["customerId"])
    ]
)
data class SalesInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val dateTime: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double = 0.0,
    val paymentType: PaymentType = PaymentType.CASH,
    val customerId: Long? = null,
    val notes: String? = null,
    val status: InvoiceStatus = InvoiceStatus.COMPLETED,
    val createdAt: Long = System.currentTimeMillis()
)
