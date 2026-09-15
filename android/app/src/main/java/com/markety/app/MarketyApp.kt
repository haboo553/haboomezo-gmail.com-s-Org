package com.markety.app

import android.app.Application
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.repository.AuditLogRepository
import com.markety.app.data.repository.CategoryRepository
import com.markety.app.data.repository.CustomerRepository
import com.markety.app.data.repository.DailyOperationRepository
import com.markety.app.data.repository.ProductRepository
import com.markety.app.data.repository.PurchaseRepository
import com.markety.app.data.repository.ReturnRepository
import com.markety.app.data.repository.SalesRepository
import com.markety.app.data.repository.StockRepository
import com.markety.app.data.repository.SupplierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MarketyApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { MarketyDatabase.getDatabase(this, applicationScope) }

    val stockRepository by lazy { StockRepository(database.stockMovementDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val productRepository by lazy { ProductRepository(database.productDao(), stockRepository) }
    val salesRepository by lazy { SalesRepository(database, database.salesDao(), database.productDao(), database.stockMovementDao()) }
    val dailyOperationRepository by lazy { DailyOperationRepository(database, database.dailyOperationDao(), database.expenseDao(), database.salesDao()) }
    val customerRepository by lazy { CustomerRepository(database, database.customerDao()) }
    val supplierRepository by lazy { SupplierRepository(database, database.supplierDao()) }
    val purchaseRepository by lazy { PurchaseRepository(database, database.purchaseDao(), database.productDao(), database.stockMovementDao(), database.supplierDao()) }
    val returnRepository by lazy { ReturnRepository(database, database.returnDao(), database.salesDao(), database.purchaseDao(), database.productDao(), database.stockMovementDao(), database.customerDao(), database.supplierDao()) }
    val auditLogRepository by lazy { AuditLogRepository(database.auditLogDao()) }
}
