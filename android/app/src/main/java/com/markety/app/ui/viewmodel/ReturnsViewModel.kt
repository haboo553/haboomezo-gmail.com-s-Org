package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.InvoiceWithItems
import com.markety.app.data.local.dao.PurchaseInvoiceWithSupplierAndItems
import com.markety.app.data.local.dao.ReturnInvoiceWithItems
import com.markety.app.data.local.entity.ReturnType
import com.markety.app.data.repository.PurchaseRepository
import com.markety.app.data.repository.ReturnItemRequest
import com.markety.app.data.repository.ReturnRepository
import com.markety.app.data.repository.ReturnResult
import com.markety.app.data.repository.SalesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReturnItemUiState(
    val productId: Long,
    val productName: String,
    val maxQuantity: Int,
    val returnQuantity: Int,
    val unitPrice: Double,
    val isSelected: Boolean = false
) {
    val total: Double get() = returnQuantity * unitPrice
}

class ReturnsViewModel(
    private val returnRepository: ReturnRepository,
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    val returns: StateFlow<List<ReturnInvoiceWithItems>> = returnRepository
        .getAllReturnInvoices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _returnType = MutableStateFlow(ReturnType.SALES_RETURN)
    val returnType: StateFlow<ReturnType> = _returnType.asStateFlow()

    private val _invoiceNumberQuery = MutableStateFlow("")
    val invoiceNumberQuery: StateFlow<String> = _invoiceNumberQuery.asStateFlow()

    private val _loadedSalesInvoice = MutableStateFlow<InvoiceWithItems?>(null)
    val loadedSalesInvoice: StateFlow<InvoiceWithItems?> = _loadedSalesInvoice.asStateFlow()

    private val _loadedPurchaseInvoice = MutableStateFlow<PurchaseInvoiceWithSupplierAndItems?>(null)
    val loadedPurchaseInvoice: StateFlow<PurchaseInvoiceWithSupplierAndItems?> = _loadedPurchaseInvoice.asStateFlow()

    private val _returnItems = MutableStateFlow<List<ReturnItemUiState>>(emptyList())
    val returnItems: StateFlow<List<ReturnItemUiState>> = _returnItems.asStateFlow()

    private val _reason = MutableStateFlow("")
    val reason: StateFlow<String> = _reason.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successResult = MutableStateFlow<ReturnResult?>(null)
    val successResult: StateFlow<ReturnResult?> = _successResult.asStateFlow()

    fun setReturnType(type: ReturnType) {
        _returnType.value = type
        resetInvoiceSearch()
    }

    fun setInvoiceNumberQuery(query: String) {
        _invoiceNumberQuery.value = query
    }

    fun setReason(reason: String) {
        _reason.value = reason
    }

    fun searchInvoice() {
        val query = _invoiceNumberQuery.value.trim()
        if (query.isBlank()) {
            _errorMessage.value = "يرجى كتابة رقم الفاتورة للبحث."
            return
        }

        viewModelScope.launch {
            if (_returnType.value == ReturnType.SALES_RETURN) {
                val invoice = salesRepository.getInvoiceWithItemsDirect(query.toLongOrNull() ?: -1L)
                if (invoice != null) {
                    _loadedSalesInvoice.value = invoice
                    _returnItems.value = invoice.items.map { item ->
                        ReturnItemUiState(
                            productId = item.productId,
                            productName = item.productNameSnapshot,
                            maxQuantity = item.quantity,
                            returnQuantity = 1,
                            unitPrice = item.unitSellingPrice,
                            isSelected = false
                        )
                    }
                } else {
                    _errorMessage.value = "لم يتم العثور على فاتورة مبيعات بهذا الرقم."
                }
            } else {
                val purchase = purchaseRepository.getPurchaseInvoiceByNumber(query)
                if (purchase != null) {
                    _loadedPurchaseInvoice.value = purchase
                    _returnItems.value = purchase.items.map { item ->
                        ReturnItemUiState(
                            productId = item.productId,
                            productName = item.productNameSnapshot,
                            maxQuantity = item.quantity,
                            returnQuantity = 1,
                            unitPrice = item.unitPurchasePrice,
                            isSelected = false
                        )
                    }
                } else {
                    _errorMessage.value = "لم يتم العثور على فاتورة شراء بهذا الرقم."
                }
            }
        }
    }

    fun toggleItemSelection(productId: Long) {
        _returnItems.value = _returnItems.value.map {
            if (it.productId == productId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun updateReturnQuantity(productId: Long, qty: Int) {
        _returnItems.value = _returnItems.value.map {
            if (it.productId == productId) {
                val validQty = qty.coerceIn(1, it.maxQuantity)
                it.copy(returnQuantity = validQty)
            } else it
        }
    }

    fun submitReturn() {
        val selected = _returnItems.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _errorMessage.value = "يرجى تحديد الأصناف المراد إرجاعها."
            return
        }

        viewModelScope.launch {
            val requests = selected.map {
                ReturnItemRequest(
                    productId = it.productId,
                    quantity = it.returnQuantity,
                    unitPrice = it.unitPrice
                )
            }

            if (_returnType.value == ReturnType.SALES_RETURN) {
                val invoiceNum = _loadedSalesInvoice.value?.invoice?.invoiceNumber
                    ?: _invoiceNumberQuery.value
                val result = returnRepository.processSalesReturn(
                    invoiceNumber = invoiceNum,
                    itemsToReturn = requests,
                    reason = _reason.value.ifBlank { null }
                )
                result.onSuccess {
                    _successResult.value = it
                    resetInvoiceSearch()
                }.onFailure {
                    _errorMessage.value = it.message ?: "فشل معالجة مرتجع المبيعات"
                }
            } else {
                val invoiceNum = _loadedPurchaseInvoice.value?.invoice?.invoiceNumber
                    ?: _invoiceNumberQuery.value
                val result = returnRepository.processPurchaseReturn(
                    purchaseInvoiceNumber = invoiceNum,
                    itemsToReturn = requests,
                    reason = _reason.value.ifBlank { null }
                )
                result.onSuccess {
                    _successResult.value = it
                    resetInvoiceSearch()
                }.onFailure {
                    _errorMessage.value = it.message ?: "فشل معالجة مرتجع المشتريات"
                }
            }
        }
    }

    fun resetInvoiceSearch() {
        _invoiceNumberQuery.value = ""
        _loadedSalesInvoice.value = null
        _loadedPurchaseInvoice.value = null
        _returnItems.value = emptyList()
        _reason.value = ""
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successResult.value = null
    }

    class Factory(
        private val returnRepo: ReturnRepository,
        private val salesRepo: SalesRepository,
        private val purchaseRepo: PurchaseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReturnsViewModel::class.java)) {
                return ReturnsViewModel(returnRepo, salesRepo, purchaseRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
