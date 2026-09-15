package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.SupplierWithBalance
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.local.entity.SupplierTransactionEntity
import com.markety.app.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SuppliersViewModel(
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val suppliers: StateFlow<List<SupplierWithBalance>> = _searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                supplierRepository.getSuppliersWithBalance()
            } else {
                supplierRepository.searchSuppliersWithBalance(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSupplierDebts: StateFlow<Double> = supplierRepository.getTotalSupplierDebts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _selectedSupplierId = MutableStateFlow<Long?>(null)
    val selectedSupplierId: StateFlow<Long?> = _selectedSupplierId.asStateFlow()

    val selectedSupplierTransactions: StateFlow<List<SupplierTransactionEntity>> = _selectedSupplierId
        .flatMapLatest { id ->
            if (id != null) {
                supplierRepository.getTransactionsForSupplier(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSupplier(id: Long?) {
        _selectedSupplierId.value = id
    }

    fun addSupplier(name: String, phone: String, address: String = "", notes: String? = null) {
        viewModelScope.launch {
            val result = supplierRepository.addSupplier(name, phone, address, notes)
            result.onSuccess {
                _successMessage.value = "تمت إضافة المورد بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل إضافة المورد"
            }
        }
    }

    fun updateSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            val result = supplierRepository.updateSupplier(supplier)
            result.onSuccess {
                _successMessage.value = "تم تعديل بيانات المورد بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل تعديل المورد"
            }
        }
    }

    fun softDeleteSupplier(id: Long) {
        viewModelScope.launch {
            val result = supplierRepository.softDeleteSupplier(id)
            result.onSuccess {
                _successMessage.value = "تم حذف المورد بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل حذف المورد"
            }
        }
    }

    fun paySupplier(supplierId: Long, amount: Double, notes: String? = null) {
        viewModelScope.launch {
            val result = supplierRepository.paySupplier(supplierId, amount, notes)
            result.onSuccess {
                _successMessage.value = "تم تسجيل دفعة للمورد بمبلغ ${String.format("%.2f", amount)} ج.م بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل تسجيل الدفعة"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    class Factory(private val repository: SupplierRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SuppliersViewModel::class.java)) {
                return SuppliersViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
