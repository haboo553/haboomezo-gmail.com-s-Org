package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Expenses",
    foreignKeys = [
        ForeignKey(
            entity = DailyOperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyOperationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dailyOperationId"]),
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyOperationId: Long,
    val category: String, // e.g. فواتير ومرافق، إيجار، نقل وبضائع، صيانة، نظافة، وجبات، أخرى
    val amount: Double,
    val notes: String? = null,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
