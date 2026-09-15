package com.markety.app.data.repository

import com.markety.app.data.local.dao.StockMovementDao
import com.markety.app.data.local.entity.MovementType
import com.markety.app.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

class StockRepository(private val stockMovementDao: StockMovementDao) {

    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>> {
        return stockMovementDao.getMovementsForProduct(productId)
    }

    val recentMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getRecentMovements()

    suspend fun recordMovement(
        productId: Long,
        movementType: MovementType,
        quantity: Int,
        previousQuantity: Int,
        newQuantity: Int,
        purchasePrice: Double,
        sellingPrice: Double,
        notes: String? = null,
        referenceId: String? = null
    ): Long {
        val movement = StockMovementEntity(
            productId = productId,
            movementType = movementType,
            quantity = quantity,
            previousQuantity = previousQuantity,
            newQuantity = newQuantity,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            referenceId = referenceId,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )
        return stockMovementDao.insertMovement(movement)
    }
}
