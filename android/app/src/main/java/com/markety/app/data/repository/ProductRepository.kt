package com.markety.app.data.repository

import com.markety.app.data.local.dao.ProductDao
import com.markety.app.data.local.dao.ProductWithCategory
import com.markety.app.data.local.entity.MovementType
import com.markety.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val stockRepository: StockRepository
) {

    val allActiveProducts: Flow<List<ProductWithCategory>> = productDao.getAllActiveProducts()

    val activeProductsCount: Flow<Int> = productDao.getActiveProductsCount()
    val totalStockQuantity: Flow<Int> = productDao.getTotalStockQuantity()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val outOfStockCount: Flow<Int> = productDao.getOutOfStockCount()

    fun searchProducts(query: String): Flow<List<ProductWithCategory>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            productDao.getAllActiveProducts()
        } else {
            productDao.searchProducts(trimmed)
        }
    }

    suspend fun getProductById(id: Long): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun getProductWithCategoryById(id: Long): ProductWithCategory? {
        return productDao.getProductWithCategoryById(id)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        return productDao.getProductByBarcode(trimmed)
    }

    suspend fun getProductWithCategoryByBarcode(barcode: String): ProductWithCategory? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        return productDao.getProductWithCategoryByBarcode(trimmed)
    }

    suspend fun isBarcodeTaken(barcode: String, excludeProductId: Long? = null): Boolean {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return false
        val existing = productDao.findAnyProductByBarcode(trimmed)
        return existing != null && (excludeProductId == null || existing.id != excludeProductId)
    }

    /**
     * Requirement 9: Adding initial quantity must create an INITIAL stock movement.
     * Enforces unique barcode if barcode is provided.
     */
    suspend fun addProduct(product: ProductEntity): Long {
        val trimmedBarcode = product.barcode?.trim()?.ifEmpty { null }
        if (trimmedBarcode != null) {
            val existing = productDao.findAnyProductByBarcode(trimmedBarcode)
            if (existing != null) {
                throw IllegalArgumentException("هذا الباركود مسجل بالفعل لمنتج آخر")
            }
        }

        val sanitizedProduct = product.copy(barcode = trimmedBarcode)
        val productId = productDao.insertProduct(sanitizedProduct)
        if (sanitizedProduct.quantity > 0) {
            stockRepository.recordMovement(
                productId = productId,
                movementType = MovementType.INITIAL,
                quantity = sanitizedProduct.quantity,
                previousQuantity = 0,
                newQuantity = sanitizedProduct.quantity,
                purchasePrice = sanitizedProduct.purchasePrice,
                sellingPrice = sanitizedProduct.sellingPrice,
                notes = "رصيد افتتاحي للصنف عند إضافته"
            )
        }
        return productId
    }

    /**
     * Requirement 7 & 10: Updating a product. If quantity changed, records an ADJUSTMENT stock movement.
     * Prevents duplicate barcode with another product.
     */
    suspend fun updateProduct(updatedProduct: ProductEntity, updateReason: String? = null) {
        val existing = productDao.getProductById(updatedProduct.id)
            ?: throw IllegalStateException("المنتج غير موجود")

        val trimmedBarcode = updatedProduct.barcode?.trim()?.ifEmpty { null }
        if (trimmedBarcode != null) {
            val conflict = productDao.findAnyProductByBarcode(trimmedBarcode)
            if (conflict != null && conflict.id != updatedProduct.id) {
                throw IllegalArgumentException("هذا الباركود مسجل بالفعل لمنتج آخر")
            }
        }

        val oldQty = existing.quantity
        val newQty = updatedProduct.quantity

        if (oldQty != newQty) {
            val delta = newQty - oldQty
            stockRepository.recordMovement(
                productId = updatedProduct.id,
                movementType = MovementType.ADJUSTMENT,
                quantity = delta,
                previousQuantity = oldQty,
                newQuantity = newQty,
                purchasePrice = updatedProduct.purchasePrice,
                sellingPrice = updatedProduct.sellingPrice,
                notes = updateReason ?: "تعديل كمية المخزون"
            )
        }

        productDao.updateProduct(
            updatedProduct.copy(
                barcode = trimmedBarcode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Explicit stock adjustment method with audit trail.
     */
    suspend fun adjustQuantity(productId: Long, newQuantity: Int, reason: String) {
        val existing = productDao.getProductById(productId)
            ?: throw IllegalStateException("المنتج غير موجود")

        val oldQty = existing.quantity
        if (oldQty == newQuantity) return

        val delta = newQuantity - oldQty
        stockRepository.recordMovement(
            productId = productId,
            movementType = MovementType.ADJUSTMENT,
            quantity = delta,
            previousQuantity = oldQty,
            newQuantity = newQuantity,
            purchasePrice = existing.purchasePrice,
            sellingPrice = existing.sellingPrice,
            notes = reason.ifBlank { "تعديل جرد يدوي" }
        )

        productDao.updateProduct(
            existing.copy(
                quantity = newQuantity,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Requirement 8: Soft Delete (isActive = false).
     * Does NOT delete record or historic movements.
     */
    suspend fun softDeleteProduct(productId: Long) {
        productDao.softDeleteProduct(productId)
    }
}
