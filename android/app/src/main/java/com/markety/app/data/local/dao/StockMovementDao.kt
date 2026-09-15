package com.markety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markety.app.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM StockMovements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM StockMovements ORDER BY createdAt DESC LIMIT 100")
    fun getRecentMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovements(movements: List<StockMovementEntity>)
}
