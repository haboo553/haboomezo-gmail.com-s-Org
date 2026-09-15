package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["action"]),
        Index(value = ["userRole"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userRole: UserRole,
    val action: String,
    val details: String,
    val referenceId: String? = null
)
