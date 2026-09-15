package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.ProductWithCategory
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.repository.CategoryRepository
import com.markety.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StockFilterOption(val arabicLabel: String) {
    ALL("الكل"),
    AVAILABLE("متوفر"),
    LOW_STOCK("مخزون منخفض"),
    OUT_OF_STOCK("نفد")
}

data class ProductsUiState(
    val products: List<ProductWithCategory> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: StockFilterOption = StockFilterOption.ALL,
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilterOption.ALL)
    val stockFilter: StateFlow<StockFilterOption> = _stockFilter.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val categories = categoryRepository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<ProductsUiState> = combine(
        _searchQuery.debounce(150),
        _stockFilter,
        _selectedCategoryId,
        productRepository.allActiveProducts
    ) { query, filter, catId, allProducts ->
        val filtered = allProducts.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.product.name.contains(query, ignoreCase = true) ||
                    item.product.barcode.contains(query, ignoreCase = true)

            val matchesCategory = catId == null || item.product.categoryId == catId

            val matchesFilter = when (filter) {
                StockFilterOption.ALL -> true
                StockFilterOption.AVAILABLE -> item.product.isAvailable
                StockFilterOption.LOW_STOCK -> item.product.isLowStock
                StockFilterOption.OUT_OF_STOCK -> item.product.isOutOfStock
            }

            matchesQuery && matchesCategory && matchesFilter
        }

        ProductsUiState(
            products = filtered,
            searchQuery = query,
            selectedFilter = filter,
            selectedCategoryId = catId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState(isLoading = true)
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelect(filter: StockFilterOption) {
        _stockFilter.value = filter
    }

    fun onCategorySelect(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                // Soft delete preserves all historical stock movements
                productRepository.softDeleteProduct(productId)
                _message.value = "تم نقل الصنف إلى المحذوفات بنجاح"
            } catch (e: Exception) {
                _error.value = "تعذر حذف الصنف: ${e.localizedMessage}"
            }
        }
    }

    fun adjustProductQuantity(productId: Long, newQuantity: Int, reason: String) {
        viewModelScope.launch {
            try {
                productRepository.adjustQuantity(productId, newQuantity, reason)
                _message.value = "تم تعديل كمية المخزون وتسجيل الحركة بنجاح"
            } catch (e: Exception) {
                _error.value = "خطأ في تعديل المخزون: ${e.localizedMessage}"
            }
        }
    }

    fun clearFeedback() {
        _message.value = null
        _error.value = null
    }
}
