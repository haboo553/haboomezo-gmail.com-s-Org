package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.entity.CategoryEntity
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.repository.CategoryRepository
import com.markety.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductFormState(
    val id: Long = 0,
    val isEditMode: Boolean = false,
    val name: String = "",
    val barcode: String = "",
    val imagePath: String? = null,
    val categoryId: Long? = null,
    val purchasePrice: String = "",
    val sellingPrice: String = "",
    val quantity: String = "0",
    val minimumQuantity: String = "5",
    val expiryDate: String = "",
    val supplierName: String = "",
    val notes: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AddEditProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.allCategories.collect { cats ->
                _formState.value = _formState.value.copy(
                    categories = cats,
                    categoryId = _formState.value.categoryId ?: cats.firstOrNull()?.id
                )
            }
        }
    }

    fun loadProduct(productId: Long) {
        if (productId <= 0) return
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true)
            val product = productRepository.getProductById(productId)
            if (product != null) {
                _formState.value = _formState.value.copy(
                    id = product.id,
                    isEditMode = true,
                    name = product.name,
                    barcode = product.barcode,
                    imagePath = product.imagePath,
                    categoryId = product.categoryId,
                    purchasePrice = product.purchasePrice.toString(),
                    sellingPrice = product.sellingPrice.toString(),
                    quantity = product.quantity.toString(),
                    minimumQuantity = product.minimumQuantity.toString(),
                    expiryDate = product.expiryDate ?: "",
                    supplierName = product.supplierName ?: "",
                    notes = product.notes ?: "",
                    isLoading = false
                )
            } else {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = "المنتج غير موجود"
                )
            }
        }
    }

    fun setInitialBarcode(code: String) {
        if (!_formState.value.isEditMode && code.isNotBlank()) {
            _formState.value = _formState.value.copy(barcode = code, errorMessage = null)
        }
    }

    fun onNameChange(v: String) { _formState.value = _formState.value.copy(name = v, errorMessage = null) }
    fun onBarcodeChange(v: String) { _formState.value = _formState.value.copy(barcode = v, errorMessage = null) }
    fun onImagePathChange(path: String?) { _formState.value = _formState.value.copy(imagePath = path) }
    fun removeImage() {
        val currentPath = _formState.value.imagePath
        com.markety.app.util.ImageStorageUtil.deleteImageFile(currentPath)
        _formState.value = _formState.value.copy(imagePath = null)
    }
    fun onCategoryChange(catId: Long) { _formState.value = _formState.value.copy(categoryId = catId) }
    fun onPurchasePriceChange(v: String) { _formState.value = _formState.value.copy(purchasePrice = v, errorMessage = null) }
    fun onSellingPriceChange(v: String) { _formState.value = _formState.value.copy(sellingPrice = v, errorMessage = null) }
    fun onQuantityChange(v: String) { _formState.value = _formState.value.copy(quantity = v, errorMessage = null) }
    fun onMinimumQuantityChange(v: String) { _formState.value = _formState.value.copy(minimumQuantity = v, errorMessage = null) }
    fun onExpiryDateChange(v: String) { _formState.value = _formState.value.copy(expiryDate = v) }
    fun onSupplierNameChange(v: String) { _formState.value = _formState.value.copy(supplierName = v) }
    fun onNotesChange(v: String) { _formState.value = _formState.value.copy(notes = v) }

    fun saveProduct() {
        val s = _formState.value
        val name = s.name.trim()
        val barcode = s.barcode.trim()
        val catId = s.categoryId

        if (name.isEmpty()) {
            _formState.value = s.copy(errorMessage = "يرجى إدخال اسم الصنف")
            return
        }
        if (catId == null) {
            _formState.value = s.copy(errorMessage = "يرجى اختيار فئة للصنف")
            return
        }

        val pPrice = s.purchasePrice.toDoubleOrNull() ?: 0.0
        val sPrice = s.sellingPrice.toDoubleOrNull() ?: 0.0
        val qty = s.quantity.toIntOrNull() ?: 0
        val minQty = s.minimumQuantity.toIntOrNull() ?: 0

        if (sPrice < pPrice) {
            _formState.value = s.copy(errorMessage = "سعر البيع يجب أن يكون مساوياً أو أكبر من سعر الشراء")
            return
        }

        viewModelScope.launch {
            try {
                _formState.value = s.copy(isLoading = true, errorMessage = null)

                // Check for duplicate barcode in SQLite database
                if (barcode.isNotBlank()) {
                    val excludeId = if (s.isEditMode) s.id else null
                    val isTaken = productRepository.isBarcodeTaken(barcode, excludeId)
                    if (isTaken) {
                        _formState.value = s.copy(
                            isLoading = false,
                            errorMessage = "هذا الباركود مسجل بالفعل لمنتج آخر"
                        )
                        return@launch
                    }
                }

                val product = ProductEntity(
                    id = s.id,
                    name = name,
                    barcode = barcode.ifBlank { null },
                    imagePath = s.imagePath,
                    categoryId = catId,
                    purchasePrice = pPrice,
                    sellingPrice = sPrice,
                    quantity = qty,
                    minimumQuantity = minQty,
                    expiryDate = s.expiryDate.ifBlank { null },
                    supplierName = s.supplierName.ifBlank { null },
                    notes = s.notes.ifBlank { null },
                    isActive = true
                )

                if (s.isEditMode) {
                    productRepository.updateProduct(product)
                } else {
                    productRepository.addProduct(product)
                }

                _formState.value = _formState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "خطأ أثناء الحفظ"
                )
            }
        }
    }
}
