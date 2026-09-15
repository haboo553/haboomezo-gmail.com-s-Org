import { ProjectFile } from '../types';

export const ANDROID_FILES: ProjectFile[] = [
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/MarketyDatabase.kt',
    category: 'Database',
    description: 'قاعدة بيانات Room الأساسية فوق SQLite مع دعم TypeConverters وبذر الفئات الافتراضية',
    code: `package com.markety.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.markety.app.data.local.dao.*
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CategoryEntity::class, ProductEntity::class, StockMovementEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MarketyDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao

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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/entity/ProductEntity.kt',
    category: 'Entity',
    description: 'كيان المنتج مع Foreign Key مرتبط بـ CategoryEntity وفهارس على barcode و name و categoryId ودعم Soft Delete',
    code: `package com.markety.app.data.local.entity

import androidx.room.*

@Entity(
    tableName = "Products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["name"]),
        Index(value = ["categoryId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String,
    val imagePath: String? = null,
    val categoryId: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val minimumQuantity: Int,
    val expiryDate: String? = null,
    val supplierName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true
) {
    val isOutOfStock: Boolean get() = quantity <= 0
    val isLowStock: Boolean get() = quantity in 1..minimumQuantity
    val isAvailable: Boolean get() = quantity > minimumQuantity
}`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/entity/StockMovementEntity.kt',
    category: 'Entity',
    description: 'كيان حركة المخزون مع Foreign Key مرتبط بـ ProductEntity وفهارس للبحث التاريخي',
    code: `package com.markety.app.data.local.entity

import androidx.room.*

@Entity(
    tableName = "StockMovements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["createdAt"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val movementType: MovementType,
    val quantity: Int,              // e.g. +50 or -5
    val previousQuantity: Int,      // e.g. 50
    val newQuantity: Int,           // e.g. 45
    val purchasePrice: Double,
    val sellingPrice: Double,
    val referenceId: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/entity/CategoryEntity.kt',
    category: 'Entity',
    description: 'كيان الفئات Categories في SQLite',
    code: `package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/dao/ProductDao.kt',
    category: 'DAO',
    description: 'واجهة استعلامات الأصناف: بحث سريع، إحصائيات Dashboard، الحذف غير المباشر Soft Delete',
    code: `package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM Products WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActiveProducts(): Flow<List<ProductWithCategory>>

    @Transaction
    @Query("SELECT * FROM Products WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY id DESC")
    fun searchProducts(query: String): Flow<List<ProductWithCategory>>

    @Query("UPDATE Products SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1")
    fun getActiveProductsCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM Products WHERE isActive = 1")
    fun getTotalStockQuantity(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1 AND quantity > 0 AND quantity <= minimumQuantity")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Products WHERE isActive = 1 AND quantity <= 0")
    fun getOutOfStockCount(): Flow<Int>
}`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/repository/ProductRepository.kt',
    category: 'Repository',
    description: 'مستودع الأصناف: تسجيل حركات INITIAL عند الإضافة و ADJUSTMENT عند التعديل تلقائياً مع الحفظ في SQLite',
    code: `package com.markety.app.data.repository

import com.markety.app.data.local.dao.*
import com.markety.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val stockRepository: StockRepository
) {
    suspend fun addProduct(product: ProductEntity): Long {
        val productId = productDao.insertProduct(product)
        if (product.quantity > 0) {
            stockRepository.recordMovement(
                productId = productId,
                movementType = MovementType.INITIAL,
                quantity = product.quantity,
                previousQuantity = 0,
                newQuantity = product.quantity,
                purchasePrice = product.purchasePrice,
                sellingPrice = product.sellingPrice,
                notes = "رصيد افتتاحي للصنف عند إضافته"
            )
        }
        return productId
    }

    suspend fun adjustQuantity(productId: Long, newQuantity: Int, reason: String) {
        val existing = productDao.getProductById(productId) ?: return
        val delta = newQuantity - existing.quantity
        stockRepository.recordMovement(
            productId = productId,
            movementType = MovementType.ADJUSTMENT,
            quantity = delta,
            previousQuantity = existing.quantity,
            newQuantity = newQuantity,
            purchasePrice = existing.purchasePrice,
            sellingPrice = existing.sellingPrice,
            notes = reason
        )
        productDao.updateProduct(existing.copy(quantity = newQuantity))
    }
}`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/ui/screens/DashboardScreen.kt',
    category: 'Screen',
    description: 'شاشة Dashboard عربية RTL تقرأ أرقام حقيقية مباشرة من SQLite عبر Flow',
    code: `// شاشة Dashboard بتصميم Material 3 و RTL كامل
// تعرض إجمالي الأصناف، الفئات، كمية المخزون، الأصناف المنخفضة، والأصناف التي نفدت`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/ui/screens/ProductDetailScreen.kt',
    category: 'Screen',
    description: 'شاشة تفاصيل الصنف مع جدول حركات المخزون (التاريخ | نوع الحركة | الكمية | قبل | بعد) وتعديل الكمية',
    code: `// تعرض تفاصيل الصنف وزر تعديل المخزون
// وجدول سجل حركات المخزون: التاريخ | نوع الحركة | الكمية | قبل | بعد`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/MainActivity.kt',
    category: 'Navigation',
    description: 'النشاط الرئيسي مع تفعيل RTL ودعم Material 3 و Navigation Compose',
    code: `package com.markety.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.markety.app.ui.navigation.MainAppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MarketyApp
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MainAppScaffold(app = app)
            }
        }
    }
}`
  },
  {
    path: 'android/app/src/test/java/com/markety/app/MarketyLogicTest.kt',
    category: 'Test',
    description: 'اختبارات Unit Tests لمنطق الرصيد الافتتاحي وتعديلات الجرد و Soft Delete',
    code: `// اختبارات الوحدة لمنطق المخزون وحالات متوفر / مخزون منخفض / نفد`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/entity/DailyOperationEntity.kt',
    category: 'Entity',
    description: 'كيان يوم التشغيل في SQLite مع رصيد البداية، المبيعات (كاش/آجل)، المصروفات، صافي المبيعات، الرصيد المتوقع والفعلي، والفرق وحالة OPEN/CLOSED',
    code: `package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DailyOperationStatus {
    OPEN,
    CLOSED
}

@Entity(
    tableName = "daily_operations",
    indices = [
        Index(value = ["date"]),
        Index(value = ["status"])
    ]
)
data class DailyOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val openingCash: Double,
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val expenses: Double = 0.0,
    val netSales: Double = 0.0,
    val closingCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val status: DailyOperationStatus = DailyOperationStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val notes: String? = null
)`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/local/entity/ExpenseEntity.kt',
    category: 'Entity',
    description: 'كيان المصروفات مع Foreign Key مرتبط بـ DailyOperationEntity وخاصية CASCADE on delete',
    code: `package com.markety.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = DailyOperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyOperationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dailyOperationId"]),
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyOperationId: Long,
    val category: String,
    val amount: Double,
    val notes: String? = null,
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/data/repository/DailyOperationRepository.kt',
    category: 'Repository',
    description: 'مستودع منطق العمليات اليومية مع إغلاق اليوم داخل Room Transaction وحساب المتوقع والفعلي بدقة ومنع التعديل بعد الإغلاق',
    code: `// كود مستودع إدارة يوم التشغيل وحساب المبيعات والمصروفات ومعاملات الإغلاق الذرية في SQLite`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/ui/screens/DailyOperationScreen.kt',
    category: 'Screen',
    description: 'شاشة «اليوم»: فتح يوم جديد، بطاقات المبيعات (كاش/آجل/فواتير)، المصروفات، الصافي، المتوقع، إدخال الفعلي، حساب الفرق وإغلاق اليوم',
    code: `// شاشة إدارة اليوم في Jetpack Compose بتصميم RTL كامل وحسابات فورية`
  },
  {
    path: 'android/app/src/main/java/com/markety/app/ui/screens/ExpensesScreen.kt',
    category: 'Screen',
    description: 'شاشة «المصروفات»: إضافة مصروف، التصنيف، القيمة، الملاحظات، التاريخ، والربط التلقائي باليوم المفتوح',
    code: `// شاشة المصروفات وإدارتها مع قائمة الفلترة والإضافة`
  }
];
