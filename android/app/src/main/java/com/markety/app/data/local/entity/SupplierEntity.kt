package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["name"]),
        Index(value = ["isActive"])
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
