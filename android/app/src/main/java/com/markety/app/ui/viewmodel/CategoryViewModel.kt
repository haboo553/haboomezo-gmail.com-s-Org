package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.CategoryWithCount
import com.markety.app.data.local.entity.CategoryEntity
import com.markety.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val categoriesWithCount: StateFlow<List<CategoryWithCount>> =
        categoryRepository.categoriesWithProductCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _error.value = "يرجى كتابة اسم الفئة"
            return
        }
        viewModelScope.launch {
            try {
                categoryRepository.insertCategory(trimmed)
                _message.value = "تمت إضافة الفئة بنجاح"
            } catch (e: Exception) {
                _error.value = "تعذر إضافة الفئة: ${e.localizedMessage}"
            }
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _error.value = "يرجى كتابة اسم الفئة"
            return
        }
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category.copy(name = trimmed))
                _message.value = "تم تحديث اسم الفئة بنجاح"
            } catch (e: Exception) {
                _error.value = "تعذر تعديل الفئة: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCategory(category: CategoryEntity, productCount: Int) {
        if (productCount > 0) {
            _error.value = "لا يمكن حذف الفئة لأنها تحتوي على $productCount أصناف مرتبطة بها."
            return
        }
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
                _message.value = "تم حذف الفئة بنجاح"
            } catch (e: Exception) {
                _error.value = "تعذر حذف الفئة: ${e.localizedMessage}"
            }
        }
    }

    fun clearFeedback() {
        _message.value = null
        _error.value = null
    }
}
