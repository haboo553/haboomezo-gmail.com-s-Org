package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.ProductWithCategory
import com.markety.app.data.local.entity.StockMovementEntity
import com.markety.app.data.repository.ProductRepository
import com.markety.app.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val productWithCategory: ProductWithCategory? = null,
    val movements: List<StockMovementEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProductDetailViewModel(
    private val productId: Long,
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProductDetails()
        loadMovements()
    }

    private fun loadProductDetails() {
        viewModelScope.launch {
            try {
                val item = productRepository.getProductWithCategoryById(productId)
                _uiState.value = _uiState.value.copy(
                    productWithCategory = item,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "فشل في تحميل بيانات المنتج: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            stockRepository.getMovementsForProduct(productId).collect { list ->
                _uiState.value = _uiState.value.copy(movements = list)
            }
        }
    }

    fun adjustQuantity(newQuantity: Int, reason: String) {
        viewModelScope.launch {
            try {
                productRepository.adjustQuantity(productId, newQuantity, reason)
                loadProductDetails()
                _uiState.value = _uiState.value.copy(successMessage = "تم تحديث المخزون وتسجيل الحركة بنجاح")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "فشل تعديل المخزون: ${e.localizedMessage}")
            }
        }
    }

    fun deleteProduct() {
        viewModelScope.launch {
            try {
                productRepository.softDeleteProduct(productId)
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "فشل في حذف المنتج: ${e.localizedMessage}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
