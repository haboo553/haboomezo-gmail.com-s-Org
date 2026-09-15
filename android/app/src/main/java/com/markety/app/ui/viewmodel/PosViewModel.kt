package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.ProductWithCategory
import com.markety.app.data.local.entity.PaymentType
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.SalesInvoiceEntity
import com.markety.app.data.repository.ProductRepository
import com.markety.app.data.repository.SaleItemRequest
import com.markety.app.data.repository.SaleResult
import com.markety.app.data.repository.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CartItem(
    val product: ProductEntity,
    val categoryName: String? = null,
    val quantity: Int,
    val unitSellingPrice: Double
) {
    val total: Double
        get() = quantity * unitSellingPrice
}

data class PosUiState(
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<ProductWithCategory> = emptyList(),
    val isSearching: Boolean = false,
    val discount: Double = 0.0,
    val discountInput: String = "",
    val paidAmountInput: String = "",
    val paymentType: PaymentType = PaymentType.CASH,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successSaleResult: SaleResult? = null,
    val unregisteredBarcodeScanned: String? = null // For showing dialog when barcode is not found
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.total }

    val finalTotal: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val paidAmount: Double
        get() {
            val entered = paidAmountInput.toDoubleOrNull()
            return if (entered != null) {
                entered
            } else {
                if (paymentType == PaymentType.CASH) finalTotal else 0.0
            }
        }

    val changeAmount: Double
        get() {
            return if (paymentType == PaymentType.CASH && paidAmount > finalTotal) {
                paidAmount - finalTotal
            } else {
                0.0
            }
        }

    val remainingAmount: Double
        get() {
            return if (paymentType == PaymentType.CREDIT && paidAmount < finalTotal) {
                finalTotal - paidAmount
            } else {
                0.0
            }
        }
}

class PosViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            productRepository.searchProducts(query).collect { results ->
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
            }
        }
    }

    /**
     * Handles barcode scanned from the real CameraX ML Kit Barcode Scanner.
     */
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val foundProductWithCategory = withContext(Dispatchers.IO) {
                productRepository.getProductWithCategoryByBarcode(barcode)
            }

            if (foundProductWithCategory != null) {
                addProductToCart(foundProductWithCategory.product, foundProductWithCategory.category?.name)
            } else {
                _uiState.update { it.copy(unregisteredBarcodeScanned = barcode) }
            }
        }
    }

    fun dismissUnregisteredBarcodeDialog() {
        _uiState.update { it.copy(unregisteredBarcodeScanned = null) }
    }

    fun addProductToCart(product: ProductEntity, categoryName: String? = null) {
        if (!product.isActive) {
            _uiState.update { it.copy(errorMessage = "الصنف «${product.name}» غير متاح للبيع حالياً.") }
            return
        }

        if (product.quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "الصنف «${product.name}» نافد من المخزون.") }
            return
        }

        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst { it.product.id == product.id }

            if (existingIndex >= 0) {
                val currentItem = state.cartItems[existingIndex]
                val desiredQty = currentItem.quantity + 1

                if (desiredQty > product.quantity) {
                    return@update state.copy(
                        errorMessage = "الكمية المطلوبة من «${product.name}» أكبر من المخزون المتاح (المتاح: ${product.quantity})"
                    )
                }

                val updatedList = state.cartItems.toMutableList()
                updatedList[existingIndex] = currentItem.copy(quantity = desiredQty)
                state.copy(
                    cartItems = updatedList,
                    errorMessage = null,
                    searchQuery = "",
                    searchResults = emptyList()
                )
            } else {
                val newItem = CartItem(
                    product = product,
                    categoryName = categoryName,
                    quantity = 1,
                    unitSellingPrice = product.sellingPrice
                )
                state.copy(
                    cartItems = state.cartItems + newItem,
                    errorMessage = null,
                    searchQuery = "",
                    searchResults = emptyList()
                )
            }
        }
    }

    fun increaseQuantity(productId: Long) {
        _uiState.update { state ->
            val index = state.cartItems.indexOfFirst { it.product.id == productId }
            if (index < 0) return@update state

            val currentItem = state.cartItems[index]
            val maxStock = currentItem.product.quantity

            if (currentItem.quantity + 1 > maxStock) {
                return@update state.copy(
                    errorMessage = "الكمية المطلوبة من «${currentItem.product.name}» أكبر من المخزون المتاح (المتاح: $maxStock)"
                )
            }

            val updatedList = state.cartItems.toMutableList()
            updatedList[index] = currentItem.copy(quantity = currentItem.quantity + 1)
            state.copy(cartItems = updatedList, errorMessage = null)
        }
    }

    fun decreaseQuantity(productId: Long) {
        _uiState.update { state ->
            val index = state.cartItems.indexOfFirst { it.product.id == productId }
            if (index < 0) return@update state

            val currentItem = state.cartItems[index]
            val updatedList = state.cartItems.toMutableList()

            if (currentItem.quantity > 1) {
                updatedList[index] = currentItem.copy(quantity = currentItem.quantity - 1)
            } else {
                updatedList.removeAt(index)
            }

            state.copy(cartItems = updatedList, errorMessage = null)
        }
    }

    fun removeProductFromCart(productId: Long) {
        _uiState.update { state ->
            state.copy(
                cartItems = state.cartItems.filterNot { it.product.id == productId },
                errorMessage = null
            )
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                discount = 0.0,
                discountInput = "",
                paidAmountInput = "",
                notes = "",
                errorMessage = null
            )
        }
    }

    fun onDiscountChanged(input: String) {
        val parsed = input.toDoubleOrNull() ?: 0.0
        val sanitized = parsed.coerceAtLeast(0.0)

        _uiState.update { state ->
            if (sanitized > state.subtotal && state.subtotal > 0) {
                state.copy(
                    discountInput = input,
                    discount = state.subtotal,
                    errorMessage = "الخصم لا يمكن أن يتجاوز إجمالي الفاتورة (${state.subtotal} ج.م)"
                )
            } else {
                state.copy(
                    discountInput = input,
                    discount = sanitized,
                    errorMessage = null
                )
            }
        }
    }

    fun onPaidAmountChanged(input: String) {
        _uiState.update { it.copy(paidAmountInput = input, errorMessage = null) }
    }

    fun onPaymentTypeChanged(type: PaymentType) {
        _uiState.update { it.copy(paymentType = type, errorMessage = null) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Commits the sale transaction to Room.
     * Prevents duplicate clicks by locking `isSubmitting`.
     */
    fun completeSale() {
        val state = _uiState.value

        // Prevent double submit
        if (state.isSubmitting) return

        if (state.cartItems.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "السلة فارغة. يرجى إضافة أصناف قبل إتمام البيع.") }
            return
        }

        val saleItemRequests = state.cartItems.map {
            SaleItemRequest(
                productId = it.product.id,
                quantity = it.quantity,
                unitSellingPrice = it.unitSellingPrice
            )
        }

        val paidToUse = state.paidAmount

        // Validate Cash full payment requirement
        if (state.paymentType == PaymentType.CASH && paidToUse < state.finalTotal) {
            _uiState.update {
                it.copy(
                    errorMessage = "في الدفع الكاش، يجب دفع إجمالي الفاتورة كاملاً (${state.finalTotal} ج.م) أو أكثر."
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = salesRepository.completeSale(
                items = saleItemRequests,
                discount = state.discount,
                paidAmount = paidToUse,
                paymentType = state.paymentType,
                notes = state.notes.ifBlank { null }
            )

            result.fold(
                onSuccess = { saleResult ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successSaleResult = saleResult,
                            cartItems = emptyList(),
                            discount = 0.0,
                            discountInput = "",
                            paidAmountInput = "",
                            notes = "",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "حدث خطأ غير متوقع أثناء إتمام البيع."
                        )
                    }
                }
            )
        }
    }

    fun resetNewSale() {
        _uiState.update {
            it.copy(
                successSaleResult = null,
                cartItems = emptyList(),
                discount = 0.0,
                discountInput = "",
                paidAmountInput = "",
                notes = "",
                errorMessage = null
            )
        }
    }
}
