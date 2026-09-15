package com.markety.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.markety.app.data.local.dao.AuditLogDao
import com.markety.app.data.local.dao.CategoryDao
import com.markety.app.data.local.dao.CustomerDao
import com.markety.app.data.local.dao.DailyOperationDao
import com.markety.app.data.local.dao.ExpenseDao
import com.markety.app.data.local.dao.ProductDao
import com.markety.app.data.local.dao.PurchaseDao
import com.markety.app.data.local.dao.ReturnDao
import com.markety.app.data.local.dao.SalesDao
import com.markety.app.data.local.dao.StockMovementDao
import com.markety.app.data.local.dao.SupplierDao
import com.markety.app.data.local.entity.AuditLogEntity
import com.markety.app.data.local.entity.CategoryEntity
import com.markety.app.data.local.entity.CustomerEntity
import com.markety.app.data.local.entity.CustomerTransactionEntity
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.ExpenseEntity
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.PurchaseInvoiceEntity
import com.markety.app.data.local.entity.PurchaseItemEntity
import com.markety.app.data.local.entity.ReturnInvoiceEntity
import com.markety.app.data.local.entity.ReturnItemEntity
import com.markety.app.data.local.entity.SaleItemEntity
import com.markety.app.data.local.entity.SalesInvoiceEntity
import com.markety.app.data.local.entity.StockMovementEntity
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.local.entity.SupplierTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        SalesInvoiceEntity::class,
        SaleItemEntity::class,
        DailyOperationEntity::class,
        ExpenseEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        SupplierEntity::class,
        SupplierTransactionEntity::class,
        PurchaseInvoiceEntity::class,
        PurchaseItemEntity::class,
        ReturnInvoiceEntity::class,
        ReturnItemEntity::class,
        AuditLogEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MarketyDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun salesDao(): SalesDao
    abstract fun dailyOperationDao(): DailyOperationDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun returnDao(): ReturnDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: MarketyDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MarketyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketyDatabase::class.java,
                    "MarketyDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial supermarket categories for first launch
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val categoryDao = database.categoryDao()
                        val defaultCategories = listOf(
                            CategoryEntity(name = "مشروبات"),
                            CategoryEntity(name = "بسكويت وحلويات"),
                            CategoryEntity(name = "ألبان وجبن"),
                            CategoryEntity(name = "منظفات وعناية منزلية"),
                            CategoryEntity(name = "معلبات وأغذية محفوظة"),
                            CategoryEntity(name = "زيوت وسمن"),
                            CategoryEntity(name = "مياه معدنية وغازية"),
                            CategoryEntity(name = "عصائر ومشروبات طبيعية"),
                            CategoryEntity(name = "بقوليات وحبوب"),
                            CategoryEntity(name = "توابل وبهارات")
                        )
                        categoryDao.insertCategories(defaultCategories)
                    }
                }
            }
        }
    }
}
