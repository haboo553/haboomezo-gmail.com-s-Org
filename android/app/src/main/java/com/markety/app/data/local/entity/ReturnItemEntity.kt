package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "return_items",
    foreignKeys = [
        ForeignKey(
            entity = ReturnInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnInvoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["returnInvoiceId"]),
        Index(value = ["productId"])
    ]
)
data class ReturnItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val returnInvoiceId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double
)
