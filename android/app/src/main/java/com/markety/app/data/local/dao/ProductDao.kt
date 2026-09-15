package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.CategoryEntity
import com.markety.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

data class ProductWithCategory(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM Products WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActiveProducts(): Flow<List<ProductWithCategory>>

    @Transaction
    @Query("""
        SELECT * FROM Products 
        WHERE isActive = 1 
        AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchProducts(query: String): Flow<List<ProductWithCategory>>

    @Transaction
    @Query("SELECT * FROM Products WHERE id = :id LIMIT 1")
    suspend fun getProductWithCategoryById(id: Long): ProductWithCategory?

    @Query("SELECT * FROM Products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM Products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Transaction
    @Query("SELECT * FROM Products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getProductWithCategoryByBarcode(barcode: String): ProductWithCategory?

    @Query("SELECT * FROM Products WHERE barcode = :barcode LIMIT 1")
    suspend fun findAnyProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    // Soft delete requirement: isActive = false, preserve historical stock movements
    @Query("UPDATE Products SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, updatedAt: Long = System.currentTimeMillis())

    // Dashboard Statistics (Real SQLite aggregation queries)
    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1")
    fun getActiveProductsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM Products WHERE isActive = 1")
    fun getTotalStockQuantity(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1 AND quantity > 0 AND quantity <= minimumQuantity")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1 AND quantity <= 0")
    fun getOutOfStockCount(): Flow<Int>
}
