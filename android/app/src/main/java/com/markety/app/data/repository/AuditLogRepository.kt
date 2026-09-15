package com.markety.app.data.repository

import com.markety.app.data.local.dao.AuditLogDao
import com.markety.app.data.local.entity.AuditLogEntity
import com.markety.app.data.local.entity.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AuditLogRepository(
    private val auditLogDao: AuditLogDao
) {
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>> =
        auditLogDao.getRecentLogs(limit)

    suspend fun log(
        userRole: UserRole,
        action: String,
        details: String,
        referenceId: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val entity = AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                userRole = userRole,
                action = action,
                details = details,
                referenceId = referenceId
            )
            auditLogDao.insertLog(entity)
        } catch (e: Exception) {
            // Log failure silently so main operations are never blocked
        }
    }
}
