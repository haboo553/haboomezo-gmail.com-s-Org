package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_invoices",
    indices = [
        Index(value = ["returnNumber"], unique = true),
        Index(value = ["originalInvoiceNumber"]),
        Index(value = ["dateTime"]),
        Index(value = ["type"])
    ]
)
data class ReturnInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnNumber: String,
    val type: ReturnType,
    val originalInvoiceNumber: String,
    val customerOrSupplierId: Long? = null,
    val totalAmount: Double,
    val reason: String? = null,
    val dateTime: Long = System.currentTimeMillis()
)
