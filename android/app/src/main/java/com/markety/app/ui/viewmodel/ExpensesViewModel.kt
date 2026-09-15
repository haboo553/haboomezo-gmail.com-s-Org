package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.ExpenseEntity
import com.markety.app.data.repository.DailyOperationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val openOperation: DailyOperationEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalExpenses: Double = 0.0,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val showAddDialog: Boolean = false,
    val categoryInput: String = "فواتير ومرافق",
    val amountInput: String = "",
    val notesInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

val defaultExpenseCategories = listOf(
    "فواتير ومرافق",
    "إيجار المحل",
    "عمالة ومرتبات",
    "وجبات وضيافة",
    "نقل وبضائع",
    "نظافة ومستلزمات",
    "صيانة وتجهيزات",
    "مصروفات أخرى"
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val dailyOperationRepository: DailyOperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState(isLoading = true))
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dailyOperationRepository.getOpenOperationFlow().collectLatest { openOp ->
                _uiState.update { it.copy(openOperation = openOp, isLoading = false) }

                if (openOp != null) {
                    dailyOperationRepository.getExpensesForOperation(openOp.id).collect { list ->
                        _uiState.update {
                            it.copy(
                                expenses = list,
                                totalExpenses = list.sumOf { exp -> exp.amount }
                            )
                        }
                    }
                } else {
                    // When no open operation, show recent all expenses
                    dailyOperationRepository.getAllExpenses().collect { list ->
                        _uiState.update {
                            it.copy(
                                expenses = list,
                                totalExpenses = list.sumOf { exp -> exp.amount }
                            )
                        }
                    }
                }
            }
        }
    }

    fun openAddDialog() {
        if (_uiState.value.openOperation == null) {
            _uiState.update {
                it.copy(errorMessage = "لا يوجد يوم تشغيل مفتوح حالياً. يرجى فتح يوم جديد أولاً لتسجيل المصروفات وربطها به.")
            }
            return
        }

        _uiState.update {
            it.copy(
                showAddDialog = true,
                categoryInput = defaultExpenseCategories.first(),
                amountInput = "",
                notesInput = "",
                errorMessage = null
            )
        }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(categoryInput = category) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amountInput = amount, errorMessage = null) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun addExpense() {
        val state = _uiState.value
        val openOp = state.openOperation

        if (openOp == null) {
            _uiState.update { it.copy(errorMessage = "لا يوجد يوم تشغيل مفتوح حالياً.") }
            return
        }

        val amount = state.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر.") }
            return
        }

        if (state.categoryInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى اختيار تصنيف للمصروف.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = dailyOperationRepository.addExpense(
                dailyOperationId = openOp.id,
                category = state.categoryInput,
                amount = amount,
                notes = state.notesInput.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showAddDialog = false,
                            amountInput = "",
                            notesInput = "",
                            successMessage = "تم تسجيل المصروف وخصمه من وردية اليوم بنجاح."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.message ?: "فشل تسجيل المصروف."
                        )
                    }
                }
            )
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
