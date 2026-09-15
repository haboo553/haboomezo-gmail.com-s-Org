package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.DailyOperationStatus
import com.markety.app.data.repository.DailyOperationRepository
import com.markety.app.data.repository.SalesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LiveDayMetrics(
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val invoicesCount: Int = 0,
    val expensesTotal: Double = 0.0,
    val netSales: Double = 0.0,
    val expectedCash: Double = 0.0
)

data class DailyOperationUiState(
    val currentOperation: DailyOperationEntity? = null,
    val isLoading: Boolean = false,
    val isOpening: Boolean = false,
    val isClosing: Boolean = false,
    val openingCashInput: String = "",
    val actualCashInput: String = "",
    val notesInput: String = "",
    val liveMetrics: LiveDayMetrics = LiveDayMetrics(),
    val closedSummary: DailyOperationEntity? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isOpen: Boolean
        get() = currentOperation != null && currentOperation.status == DailyOperationStatus.OPEN

    val actualCashParsed: Double
        get() = actualCashInput.toDoubleOrNull() ?: 0.0

    // difference = actualCash - expectedCash (positive = surplus, negative = deficit)
    val difference: Double
        get() = actualCashParsed - liveMetrics.expectedCash
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyOperationViewModel(
    private val dailyOperationRepository: DailyOperationRepository,
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyOperationUiState(isLoading = true))
    val uiState: StateFlow<DailyOperationUiState> = _uiState.asStateFlow()

    init {
        observeCurrentOperation()
    }

    private fun observeCurrentOperation() {
        viewModelScope.launch {
            dailyOperationRepository.getOpenOperationFlow().collectLatest { openOp ->
                _uiState.update { it.copy(currentOperation = openOp, isLoading = false) }

                if (openOp != null) {
                    observeLiveMetrics(openOp)
                } else {
                    _uiState.update { it.copy(liveMetrics = LiveDayMetrics()) }
                }
            }
        }
    }

    private fun observeLiveMetrics(operation: DailyOperationEntity) {
        val now = System.currentTimeMillis() + 86400000L // cover today
        val start = operation.openedAt

        viewModelScope.launch {
            combine(
                salesRepository.getTodaySalesTotal(start, now),
                salesRepository.getTodayCashTotal(start, now),
                salesRepository.getTodayCreditTotal(start, now),
                salesRepository.getTodayInvoicesCount(start, now),
                dailyOperationRepository.getTotalExpensesForOperationFlow(operation.id)
            ) { totalSales, cashSales, creditSales, invoicesCount, expensesTotal ->
                val netSales = totalSales - expensesTotal
                val expectedCash = operation.openingCash + cashSales - expensesTotal

                LiveDayMetrics(
                    totalSales = totalSales,
                    cashSales = cashSales,
                    creditSales = creditSales,
                    invoicesCount = invoicesCount,
                    expensesTotal = expensesTotal,
                    netSales = netSales,
                    expectedCash = expectedCash
                )
            }.collect { metrics ->
                _uiState.update { it.copy(liveMetrics = metrics) }
            }
        }
    }

    fun onOpeningCashChanged(input: String) {
        _uiState.update { it.copy(openingCashInput = input, errorMessage = null) }
    }

    fun onActualCashChanged(input: String) {
        _uiState.update { it.copy(actualCashInput = input, errorMessage = null) }
    }

    fun onNotesChanged(input: String) {
        _uiState.update { it.copy(notesInput = input) }
    }

    fun openNewDay() {
        val state = _uiState.value
        val amount = state.openingCashInput.toDoubleOrNull()

        if (amount == null || amount < 0) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال رصيد بداية صحيح (صفر أو أكثر).") }
            return
        }

        _uiState.update { it.copy(isOpening = true, errorMessage = null) }

        viewModelScope.launch {
            val result = dailyOperationRepository.openDay(
                openingCash = amount,
                notes = state.notesInput.ifBlank { null }
            )

            result.fold(
                onSuccess = { op ->
                    _uiState.update {
                        it.copy(
                            isOpening = false,
                            openingCashInput = "",
                            actualCashInput = "",
                            notesInput = "",
                            closedSummary = null,
                            successMessage = "تم فتح يوم التشغيل بنجاح برصيد بداية ${String.format("%.2f", op.openingCash)} ج.م"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isOpening = false,
                            errorMessage = err.message ?: "حدث خطأ أثناء فتح اليوم."
                        )
                    }
                }
            )
        }
    }

    fun closeDay() {
        val state = _uiState.value
        val operation = state.currentOperation

        if (operation == null || operation.status != DailyOperationStatus.OPEN) {
            _uiState.update { it.copy(errorMessage = "لا يوجد يوم تشغيل مفتوح لإغلاقه.") }
            return
        }

        val actualCash = state.actualCashInput.toDoubleOrNull()
        if (actualCash == null || actualCash < 0) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال النقدية الفعلية الموجودة في الدرج قبل إغلاق اليوم.") }
            return
        }

        _uiState.update { it.copy(isClosing = true, errorMessage = null) }

        viewModelScope.launch {
            val result = dailyOperationRepository.closeDay(
                dailyOperationId = operation.id,
                actualCash = actualCash,
                notes = state.notesInput.ifBlank { null }
            )

            result.fold(
                onSuccess = { closedOp ->
                    _uiState.update {
                        it.copy(
                            isClosing = false,
                            closedSummary = closedOp,
                            actualCashInput = "",
                            notesInput = "",
                            successMessage = "تم إغلاق اليوم بنجاح وحفظ كافة الأرقام نهائياً في قاعدة البيانات."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isClosing = false,
                            errorMessage = err.message ?: "فشل إغلاق اليوم."
                        )
                    }
                }
            )
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(closedSummary = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
