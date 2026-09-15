package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

data class CategoryWithCount(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "productCount") val productCount: Int
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("""
        SELECT c.*, COUNT(p.id) as productCount 
        FROM Categories c 
        LEFT JOIN Products p ON c.id = p.categoryId AND p.isActive = 1 
        GROUP BY c.id 
        ORDER BY c.name ASC
    """)
    fun getCategoriesWithProductCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM Categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM Categories")
    fun getTotalCategoriesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
