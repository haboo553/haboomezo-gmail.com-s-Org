package com.markety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.DailyOperationStatus
import com.markety.app.data.repository.CategoryRepository
import com.markety.app.data.repository.DailyOperationRepository
import com.markety.app.data.repository.ProductRepository
import com.markety.app.data.repository.SalesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DashboardUiState(
    val totalProducts: Int = 0,
    val totalCategories: Int = 0,
    val totalQuantity: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    // Real Sales Metrics from SQLite
    val todaySalesTotal: Double = 0.0,
    val todayInvoicesCount: Int = 0,
    val todayCashTotal: Double = 0.0,
    val todayCreditTotal: Double = 0.0,
    val todayItemsSoldCount: Int = 0,
    // Day Operation Status from Room
    val isDayOpen: Boolean = false,
    val currentDayOperation: DailyOperationEntity? = null,
    val openingCash: Double = 0.0,
    val todayExpensesTotal: Double = 0.0,
    val netSalesTotal: Double = 0.0,
    val expectedCashInDrawer: Double = 0.0,
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val salesRepository: SalesRepository,
    private val dailyOperationRepository: DailyOperationRepository
) : ViewModel() {

    private val startOfDay: Long
    private val endOfDay: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        endOfDay = cal.timeInMillis
    }

    private val inventoryFlow = combine(
        productRepository.activeProductsCount,
        categoryRepository.totalCategoriesCount,
        productRepository.totalStockQuantity,
        productRepository.lowStockCount,
        productRepository.outOfStockCount
    ) { products, categories, totalQty, lowStock, outOfStock ->
        InventoryStats(products, categories, totalQty, lowStock, outOfStock)
    }

    private val salesFlow = combine(
        salesRepository.getTodaySalesTotal(startOfDay, endOfDay),
        salesRepository.getTodayInvoicesCount(startOfDay, endOfDay),
        salesRepository.getTodayCashTotal(startOfDay, endOfDay),
        salesRepository.getTodayCreditTotal(startOfDay, endOfDay),
        salesRepository.getTodayItemsSoldCount(startOfDay, endOfDay)
    ) { salesTotal, invoicesCount, cashTotal, creditTotal, itemsSold ->
        SalesStats(salesTotal, invoicesCount, cashTotal, creditTotal, itemsSold)
    }

    private val operationFlow = dailyOperationRepository.getOpenOperationFlow().flatMapLatest { openOp ->
        if (openOp != null) {
            dailyOperationRepository.getTotalExpensesForOperationFlow(openOp.id).combine(
                flowOf(openOp)
            ) { expenses, op ->
                DayOperationStats(
                    isOpen = true,
                    operation = op,
                    openingCash = op.openingCash,
                    expenses = expenses
                )
            }
        } else {
            flowOf(DayOperationStats(isOpen = false, operation = null, openingCash = 0.0, expenses = 0.0))
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        inventoryFlow,
        salesFlow,
        operationFlow
    ) { inv, sales, dayOp ->
        val netSales = sales.salesTotal - dayOp.expenses
        val expectedCash = if (dayOp.isOpen) {
            dayOp.openingCash + sales.cashTotal - dayOp.expenses
        } else {
            sales.cashTotal - dayOp.expenses
        }

        DashboardUiState(
            totalProducts = inv.products,
            totalCategories = inv.categories,
            totalQuantity = inv.totalQty,
            lowStockCount = inv.lowStock,
            outOfStockCount = inv.outOfStock,
            todaySalesTotal = sales.salesTotal,
            todayInvoicesCount = sales.invoicesCount,
            todayCashTotal = sales.cashTotal,
            todayCreditTotal = sales.creditTotal,
            todayItemsSoldCount = sales.itemsSold,
            isDayOpen = dayOp.isOpen,
            currentDayOperation = dayOp.operation,
            openingCash = dayOp.openingCash,
            todayExpensesTotal = dayOp.expenses,
            netSalesTotal = netSales,
            expectedCashInDrawer = expectedCash,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    private data class InventoryStats(
        val products: Int,
        val categories: Int,
        val totalQty: Int,
        val lowStock: Int,
        val outOfStock: Int
    )

    private data class SalesStats(
        val salesTotal: Double,
        val invoicesCount: Int,
        val cashTotal: Double,
        val creditTotal: Double,
        val itemsSold: Int
    )

    private data class DayOperationStats(
        val isOpen: Boolean,
        val operation: DailyOperationEntity?,
        val openingCash: Double,
        val expenses: Double
    )
}
