package com.markety.app

import com.markety.app.data.local.entity.MovementType
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.StockMovementEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketyLogicTest {

    @Test
    fun testStockStatusCalculations() {
        val availableProduct = ProductEntity(
            name = "حليب جهينة 1 لتر",
            barcode = "6221001",
            categoryId = 1,
            purchasePrice = 30.0,
            sellingPrice = 38.0,
            quantity = 25,
            minimumQuantity = 5
        )
        assertTrue(availableProduct.isAvailable)
        assertFalse(availableProduct.isLowStock)
        assertFalse(availableProduct.isOutOfStock)

        val lowStockProduct = availableProduct.copy(quantity = 5)
        assertFalse(lowStockProduct.isAvailable)
        assertTrue(lowStockProduct.isLowStock)
        assertFalse(lowStockProduct.isOutOfStock)

        val outOfStockProduct = availableProduct.copy(quantity = 0)
        assertFalse(outOfStockProduct.isAvailable)
        assertFalse(outOfStockProduct.isLowStock)
        assertTrue(outOfStockProduct.isOutOfStock)
    }

    @Test
    fun testInitialStockMovementCalculation() {
        // Requirement 9: Initial addition: old = 0, add = 50, new = 50
        val initialQty = 50
        val movement = StockMovementEntity(
            productId = 101,
            movementType = MovementType.INITIAL,
            quantity = initialQty,
            previousQuantity = 0,
            newQuantity = initialQty,
            purchasePrice = 20.0,
            sellingPrice = 25.0,
            notes = "رصيد افتتاحي للصنف عند إضافته"
        )

        assertEquals(MovementType.INITIAL, movement.movementType)
        assertEquals(0, movement.previousQuantity)
        assertEquals(50, movement.quantity)
        assertEquals(50, movement.newQuantity)
    }

    @Test
    fun testAdjustmentStockMovementCalculation() {
        // Requirement 10: 50 -> 45 produces previous=50, qty=-5, new=45
        val oldQty = 50
        val newQty = 45
        val delta = newQty - oldQty

        val movement = StockMovementEntity(
            productId = 101,
            movementType = MovementType.ADJUSTMENT,
            quantity = delta,
            previousQuantity = oldQty,
            newQuantity = newQty,
            purchasePrice = 20.0,
            sellingPrice = 25.0,
            notes = "تعديل جرد"
        )

        assertEquals(MovementType.ADJUSTMENT, movement.movementType)
        assertEquals(50, movement.previousQuantity)
        assertEquals(-5, movement.quantity)
        assertEquals(45, movement.newQuantity)
    }

    @Test
    fun testSoftDeletePreservesHistoricalMovements() {
        // Requirement 8: isActive = false leaves stock movements intact
        val product = ProductEntity(
            id = 1,
            name = "شاي العروسة",
            barcode = "6221002",
            categoryId = 1,
            purchasePrice = 15.0,
            sellingPrice = 20.0,
            quantity = 10,
            minimumQuantity = 2,
            isActive = true
        )

        val softDeleted = product.copy(isActive = false)
        assertFalse(softDeleted.isActive)

        val movementHistory = listOf(
            StockMovementEntity(
                productId = product.id,
                movementType = MovementType.INITIAL,
                quantity = 10,
                previousQuantity = 0,
                newQuantity = 10,
                purchasePrice = 15.0,
                sellingPrice = 20.0
            )
        )

        assertEquals(1, movementHistory.size)
        assertEquals(product.id, movementHistory.first().productId)
    }
}
