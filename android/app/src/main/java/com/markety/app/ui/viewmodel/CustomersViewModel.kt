package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.CustomerWithBalance
import com.markety.app.data.local.dao.TransactionWithInvoice
import com.markety.app.data.local.entity.CustomerEntity
import com.markety.app.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CustomersViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val customers: StateFlow<List<CustomerWithBalance>> = _searchQuery
        .debounce(250)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.getCustomersWithBalance()
            } else {
                customerRepository.searchCustomersWithBalance(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalDebts: StateFlow<Double> = customerRepository.getTotalCustomerDebts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    val selectedCustomerTransactions: StateFlow<List<TransactionWithInvoice>> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id != null) {
                customerRepository.getTransactionsWithInvoice(id)
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

    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    fun addCustomer(name: String, phone: String, address: String = "", notes: String? = null) {
        viewModelScope.launch {
            val result = customerRepository.addCustomer(name, phone, address, notes)
            result.onSuccess {
                _successMessage.value = "تمت إضافة العميل بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل إضافة العميل"
            }
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val result = customerRepository.updateCustomer(customer)
            result.onSuccess {
                _successMessage.value = "تم تعديل بيانات العميل بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل تعديل العميل"
            }
        }
    }

    fun softDeleteCustomer(id: Long) {
        viewModelScope.launch {
            val result = customerRepository.softDeleteCustomer(id)
            result.onSuccess {
                _successMessage.value = "تم حذف العميل بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل حذف العميل"
            }
        }
    }

    fun collectPayment(customerId: Long, amount: Double, notes: String? = null) {
        viewModelScope.launch {
            val result = customerRepository.collectPayment(customerId, amount, notes)
            result.onSuccess {
                _successMessage.value = "تم تحصيل مبلغ ${String.format("%.2f", amount)} ج.م بنجاح"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "فشل تحصيل الدفعة"
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    class Factory(private val repository: CustomerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomersViewModel::class.java)) {
                return CustomersViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
