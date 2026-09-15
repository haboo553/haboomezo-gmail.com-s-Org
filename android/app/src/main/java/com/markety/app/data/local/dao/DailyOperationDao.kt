package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.DailyOperationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyOperationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOperation(operation: DailyOperationEntity): Long

    @Update
    suspend fun updateOperation(operation: DailyOperationEntity)

    @Query("SELECT * FROM DailyOperations WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpenOperationDirect(): DailyOperationEntity?

    @Query("SELECT * FROM DailyOperations WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getOpenOperationFlow(): Flow<DailyOperationEntity?>

    @Query("SELECT * FROM DailyOperations WHERE id = :id LIMIT 1")
    suspend fun getOperationByIdDirect(id: Long): DailyOperationEntity?

    @Query("SELECT * FROM DailyOperations WHERE id = :id LIMIT 1")
    fun getOperationByIdFlow(id: Long): Flow<DailyOperationEntity?>

    @Query("SELECT * FROM DailyOperations ORDER BY openedAt DESC")
    fun getAllOperations(): Flow<List<DailyOperationEntity>>

    @Query("SELECT * FROM DailyOperations WHERE status = 'CLOSED' ORDER BY closedAt DESC LIMIT 1")
    suspend fun getLatestClosedOperationDirect(): DailyOperationEntity?

    @Query("SELECT * FROM DailyOperations WHERE status = 'CLOSED' ORDER BY closedAt DESC LIMIT 1")
    fun getLatestClosedOperationFlow(): Flow<DailyOperationEntity?>

    @Query("SELECT COUNT(*) FROM DailyOperations WHERE status = 'OPEN'")
    suspend fun countOpenOperations(): Int
}
