package com.markety.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.markety.app.data.local.dao.ProductDao
import com.markety.app.data.local.dao.SalesDao
import com.markety.app.data.local.dao.StockMovementDao
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.SalesInvoiceEntity
import com.markety.app.data.repository.CustomerRepository
import com.markety.app.data.repository.DailyOperationRepository
import com.markety.app.data.repository.PurchaseRepository
import com.markety.app.data.repository.SupplierRepository
import com.markety.app.util.CsvExporter
import com.markety.app.util.PdfExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class TopSellingProductUi(
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double
)

class ReportsViewModel(
    private val salesDao: SalesDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository,
    private val dailyOperationRepository: DailyOperationRepository
) : ViewModel() {

    // Timeframe: 0 = Today, 1 = This Month, 2 = All Time
    private val _selectedTimeframe = MutableStateFlow(0)
    val selectedTimeframe: StateFlow<Int> = _selectedTimeframe.asStateFlow()

    private val timeRange: Flow<Pair<Long, Long>> = _selectedTimeframe.map { timeframe ->
        val calendar = Calendar.getInstance()
        when (timeframe) {
            0 -> { // Today
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            1 -> { // This Month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                val end = System.currentTimeMillis()
                Pair(start, end)
            }
            else -> { // All Time
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    val salesInvoices: StateFlow<List<SalesInvoiceEntity>> = salesDao.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCustomerDebts: StateFlow<Double> = customerRepository.getTotalCustomerDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSupplierDebts: StateFlow<Double> = supplierRepository.getTotalSupplierDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lowStockProducts: StateFlow<List<ProductEntity>> = productDao.getLowStockProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Metrics Flow based on selected timeframe
    val salesTotal: StateFlow<Double> = timeRange.flatMapLatest { (start, end) ->
        salesDao.getTodaySalesTotal(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cashSalesTotal: StateFlow<Double> = timeRange.flatMapLatest { (start, end) ->
        salesDao.getTodayCashTotal(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val creditSalesTotal: StateFlow<Double> = timeRange.flatMapLatest { (start, end) ->
        salesDao.getTodayCreditTotal(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val invoicesCount: StateFlow<Int> = timeRange.flatMapLatest { (start, end) ->
        salesDao.getTodayInvoicesCount(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val purchasesTotal: StateFlow<Double> = timeRange.flatMapLatest { (start, end) ->
        purchaseRepository.getPurchasesTotalBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _exportedPdfFile = MutableStateFlow<File?>(null)
    val exportedPdfFile: StateFlow<File?> = _exportedPdfFile.asStateFlow()

    private val _exportedCsvFile = MutableStateFlow<File?>(null)
    val exportedCsvFile: StateFlow<File?> = _exportedCsvFile.asStateFlow()

    fun setTimeframe(timeframe: Int) {
        _selectedTimeframe.value = timeframe
    }

    fun exportReportToPdf(context: Context) {
        viewModelScope.launch {
            val title = when (_selectedTimeframe.value) {
                0 -> "المبيعات والأرباح اليومية"
                1 -> "التقرير المالي الشهري"
                else -> "التقرير الشامل"
            }

            val rangeStr = when (_selectedTimeframe.value) {
                0 -> "اليوم"
                1 -> "هذا الشهر"
                else -> "كل الفترات"
            }

            val metrics = listOf(
                "إجمالي المبيعات" to "${String.format(Locale.US, "%.2f", salesTotal.value)} ج.م",
                "المبيعات النقدية (كاش)" to "${String.format(Locale.US, "%.2f", cashSalesTotal.value)} ج.م",
                "المبيعات الآجلة" to "${String.format(Locale.US, "%.2f", creditSalesTotal.value)} ج.م",
                "إجمالي المشتريات" to "${String.format(Locale.US, "%.2f", purchasesTotal.value)} ج.م",
                "إجمالي ديون العملاء" to "${String.format(Locale.US, "%.2f", totalCustomerDebts.value)} ج.م",
                "مستحقات الموردين" to "${String.format(Locale.US, "%.2f", totalSupplierDebts.value)} ج.م"
            )

            val headers = listOf("رقم الفاتورة", "التاريخ", "النوع", "الإجمالي", "المدفوع", "المتبقي")
            val rows = salesInvoices.value.take(40).map { inv ->
                listOf(
                    inv.invoiceNumber,
                    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(inv.dateTime)),
                    if (inv.paymentType.name == "CASH") "كاش" else "آجل",
                    "${inv.total} ج.م",
                    "${inv.paidAmount} ج.م",
                    "${inv.remainingAmount} ج.م"
                )
            }

            val pdf = PdfExporter.exportReportPdf(context, title, rangeStr, metrics, headers, rows)
            _exportedPdfFile.value = pdf
        }
    }

    fun exportSalesToCsv(context: Context) {
        viewModelScope.launch {
            val headers = listOf("رقم الفاتورة", "التاريخ والوقت", "طريقة الدفع", "المجموع", "الخصم", "الإجمالي", "المدفوع", "المتبقي")
            val rows = salesInvoices.value.map { inv ->
                listOf(
                    inv.invoiceNumber,
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(inv.dateTime)),
                    if (inv.paymentType.name == "CASH") "كاش" else "آجل",
                    inv.subtotal.toString(),
                    inv.discount.toString(),
                    inv.total.toString(),
                    inv.paidAmount.toString(),
                    inv.remainingAmount.toString()
                )
            }
            val csv = CsvExporter.exportToCsv(context, "Markety_Sales_Report", headers, rows)
            _exportedCsvFile.value = csv
        }
    }

    fun clearExportedFiles() {
        _exportedPdfFile.value = null
        _exportedCsvFile.value = null
    }

    class Factory(
        private val salesDao: SalesDao,
        private val productDao: ProductDao,
        private val stockMovementDao: StockMovementDao,
        private val customerRepository: CustomerRepository,
        private val supplierRepository: SupplierRepository,
        private val purchaseRepository: PurchaseRepository,
        private val dailyOperationRepository: DailyOperationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
                return ReportsViewModel(salesDao, productDao, stockMovementDao, customerRepository, supplierRepository, purchaseRepository, dailyOperationRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
