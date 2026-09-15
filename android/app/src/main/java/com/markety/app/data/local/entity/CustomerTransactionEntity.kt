package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["invoiceId"]),
        Index(value = ["dateTime"]),
        Index(value = ["type"])
    ]
)
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val type: CustomerTransactionType,
    val amount: Double,
    val invoiceId: Long? = null,
    val notes: String? = null,
    val dateTime: Long = System.currentTimeMillis()
)
