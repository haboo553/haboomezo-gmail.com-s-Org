package com.markety.app.data.repository

import com.markety.app.data.local.dao.CategoryDao
import com.markety.app.data.local.dao.CategoryWithCount
import com.markety.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    val categoriesWithProductCount: Flow<List<CategoryWithCount>> =
        categoryDao.getCategoriesWithProductCount()

    val totalCategoriesCount: Flow<Int> = categoryDao.getTotalCategoriesCount()

    suspend fun getCategoryById(id: Long): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("اسم الفئة لا يمكن أن يكون فارغاً")
        val category = CategoryEntity(name = trimmed)
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val trimmed = category.name.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("اسم الفئة لا يمكن أن يكون فارغاً")
        categoryDao.updateCategory(category.copy(name = trimmed))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }
}
