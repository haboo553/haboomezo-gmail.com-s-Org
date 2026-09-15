package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.PurchaseInvoiceWithSupplierAndItems
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.repository.ProductRepository
import com.markety.app.data.repository.PurchaseItemRequest
import com.markety.app.data.repository.PurchaseRepository
import com.markety.app.data.repository.PurchaseResult
import com.markety.app.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PurchaseCartItem(
    val product: ProductEntity,
    val quantity: Int,
    val unitPurchasePrice: Double
) {
    val total: Double get() = quantity * unitPurchasePrice
}

class PurchasesViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    val purchases: StateFlow<List<PurchaseInvoiceWithSupplierAndItems>> = purchaseRepository
        .getAllPurchaseInvoices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSuppliers: StateFlow<List<SupplierEntity>> = supplierRepository
        .getAllActiveSuppliers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allProducts: StateFlow<List<ProductEntity>> = productRepository
        .getAllActiveProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Purchase Draft State
    private val _selectedSupplier = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplier: StateFlow<SupplierEntity?> = _selectedSupplier.asStateFlow()

    private val _cartItems = MutableStateFlow<List<PurchaseCartItem>>(emptyList())
    val cartItems: StateFlow<List<PurchaseCartItem>> = _cartItems.asStateFlow()

    private val _paidAmount = MutableStateFlow<Double>(0.0)
    val paidAmount: StateFlow<Double> = _paidAmount.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    val totalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _successResult = MutableStateFlow<PurchaseResult?>(null)
    val successResult: StateFlow<PurchaseResult?> = _successResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun selectSupplier(supplier: SupplierEntity?) {
        _selectedSupplier.value = supplier
    }

    fun setPaidAmount(amount: Double) {
        _paidAmount.value = amount
    }

    fun setNotes(notes: String) {
        _notes.value = notes
    }

    fun addProductToCart(product: ProductEntity, quantity: Int = 1, unitPurchasePrice: Double = product.purchasePrice) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val old = current[existingIndex]
            current[existingIndex] = old.copy(quantity = old.quantity + quantity, unitPurchasePrice = unitPurchasePrice)
        } else {
            current.add(PurchaseCartItem(product, quantity, unitPurchasePrice))
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeCartItem(productId)
            return
        }
        _cartItems.value = _cartItems.value.map {
            if (it.product.id == productId) it.copy(quantity = quantity) else it
        }
    }

    fun updateCartItemPrice(productId: Long, price: Double) {
        _cartItems.value = _cartItems.value.map {
            if (it.product.id == productId) it.copy(unitPurchasePrice = price) else it
        }
    }

    fun removeCartItem(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _paidAmount.value = 0.0
        _notes.value = ""
        _selectedSupplier.value = null
    }

    fun submitPurchase() {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _errorMessage.value = "يرجى إضافة أصناف لفاتورة الشراء."
            return
        }

        viewModelScope.launch {
            val requests = items.map {
                PurchaseItemRequest(
                    productId = it.product.id,
                    quantity = it.quantity,
                    unitPurchasePrice = it.unitPurchasePrice
                )
            }

            val result = purchaseRepository.createPurchase(
                supplierId = _selectedSupplier.value?.id,
                items = requests,
                paidAmount = _paidAmount.value,
                notes = _notes.value.ifBlank { null }
            )

            result.onSuccess { res ->
                _successResult.value = res
                clearCart()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل تسجيل فاتورة الشراء"
            }
        }
    }

    fun clearSuccessResult() {
        _successResult.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    class Factory(
        private val purchaseRepo: PurchaseRepository,
        private val productRepo: ProductRepository,
        private val supplierRepo: SupplierRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PurchasesViewModel::class.java)) {
                return PurchasesViewModel(purchaseRepo, productRepo, supplierRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
