package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "StockMovements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["createdAt"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val movementType: MovementType,
    val quantity: Int,              // e.g. +50 or -5
    val previousQuantity: Int,      // e.g. 50
    val newQuantity: Int,           // e.g. 45
    val purchasePrice: Double,
    val sellingPrice: Double,
    val referenceId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
