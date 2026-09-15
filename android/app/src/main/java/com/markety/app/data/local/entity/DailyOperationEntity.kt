package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "DailyOperations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["date"]),
        Index(value = ["openedAt"])
    ]
)
data class DailyOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val openingCash: Double,
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val expenses: Double = 0.0,
    val netSales: Double = 0.0,
    val closingCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val status: DailyOperationStatus = DailyOperationStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val notes: String? = null
)
