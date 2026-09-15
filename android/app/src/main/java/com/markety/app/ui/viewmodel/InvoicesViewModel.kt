package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.InvoiceWithItems
import com.markety.app.data.local.entity.SalesInvoiceEntity
import com.markety.app.data.repository.SalesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoicesUiState(
    val invoices: List<SalesInvoiceEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedInvoiceWithItems: InvoiceWithItems? = null,
    val isLoadingDetail: Boolean = false
)

class InvoicesViewModel(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoicesUiState(isLoading = true))
    val uiState: StateFlow<InvoicesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInvoices()
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            salesRepository.getAllInvoices().collect { list ->
                _uiState.update {
                    it.copy(
                        invoices = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            loadInvoices()
            return
        }

        searchJob = viewModelScope.launch {
            salesRepository.searchInvoices(query).collect { list ->
                _uiState.update { it.copy(invoices = list) }
            }
        }
    }

    fun selectInvoice(invoiceId: Long) {
        _uiState.update { it.copy(isLoadingDetail = true) }
        viewModelScope.launch {
            val detail = salesRepository.getInvoiceWithItemsDirect(invoiceId)
            _uiState.update {
                it.copy(
                    selectedInvoiceWithItems = detail,
                    isLoadingDetail = false
                )
            }
        }
    }

    fun clearSelectedInvoice() {
        _uiState.update { it.copy(selectedInvoiceWithItems = null) }
    }
}
