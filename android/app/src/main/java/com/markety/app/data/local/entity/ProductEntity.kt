package com.markety.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["categoryId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val imagePath: String? = null,
    val categoryId: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val minimumQuantity: Int,
    val expiryDate: String? = null,
    val supplierName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true
) {
    val isOutOfStock: Boolean
        get() = quantity <= 0

    val isLowStock: Boolean
        get() = quantity in 1..minimumQuantity

    val isAvailable: Boolean
        get() = quantity > minimumQuantity
}
