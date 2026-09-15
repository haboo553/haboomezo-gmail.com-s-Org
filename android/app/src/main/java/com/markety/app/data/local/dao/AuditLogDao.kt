package com.markety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.markety.app.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC")
    fun getLogsByAction(action: String): Flow<List<AuditLogEntity>>
}
