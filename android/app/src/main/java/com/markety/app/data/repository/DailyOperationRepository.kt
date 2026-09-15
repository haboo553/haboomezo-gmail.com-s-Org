package com.markety.app.data.repository

import androidx.room.withTransaction
import com.markety.app.data.local.MarketyDatabase
import com.markety.app.data.local.dao.DailyOperationDao
import com.markety.app.data.local.dao.ExpenseDao
import com.markety.app.data.local.dao.SalesDao
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.data.local.entity.DailyOperationStatus
import com.markety.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveDayCalculations(
    val operation: DailyOperationEntity,
    val totalSales: Double,
    val cashSales: Double,
    val creditSales: Double,
    val invoicesCount: Int,
    val expenses: Double,
    val netSales: Double,
    val expectedCash: Double
)

class DailyOperationRepository(
    private val database: MarketyDatabase,
    private val dailyOperationDao: DailyOperationDao,
    private val expenseDao: ExpenseDao,
    private val salesDao: SalesDao
) {

    fun getOpenOperationFlow(): Flow<DailyOperationEntity?> =
        dailyOperationDao.getOpenOperationFlow()

    suspend fun getOpenOperationDirect(): DailyOperationEntity? = withContext(Dispatchers.IO) {
        dailyOperationDao.getOpenOperationDirect()
    }

    fun getAllOperations(): Flow<List<DailyOperationEntity>> =
        dailyOperationDao.getAllOperations()

    fun getOperationByIdFlow(id: Long): Flow<DailyOperationEntity?> =
        dailyOperationDao.getOperationByIdFlow(id)

    fun getExpensesForOperation(operationId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByOperationId(operationId)

    fun getAllExpenses(): Flow<List<ExpenseEntity>> =
        expenseDao.getAllExpenses()

    fun getTotalExpensesForOperationFlow(operationId: Long): Flow<Double> =
        expenseDao.getTotalExpensesForOperation(operationId)

    suspend fun openDay(openingCash: Double, notes: String? = null): Result<DailyOperationEntity> = withContext(Dispatchers.IO) {
        try {
            if (openingCash < 0) {
                return@withContext Result.failure(IllegalArgumentException("رصيد البداية لا يمكن أن يكون سالباً."))
            }

            val existingOpen = dailyOperationDao.getOpenOperationDirect()
            if (existingOpen != null) {
                return@withContext Result.failure(
                    IllegalStateException("يوجد يوم تشغيل مفتوح بالفعل (#${existingOpen.id} بتاريخ ${existingOpen.date}). يرجى إغلاق اليوم الحالي أولاً.")
                )
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDate = dateFormat.format(Date())
            val now = System.currentTimeMillis()

            val newOperation = DailyOperationEntity(
                date = todayDate,
                openingCash = openingCash,
                totalSales = 0.0,
                cashSales = 0.0,
                creditSales = 0.0,
                expenses = 0.0,
                netSales = 0.0,
                closingCash = openingCash,
                actualCash = 0.0,
                difference = 0.0,
                status = DailyOperationStatus.OPEN,
                openedAt = now,
                closedAt = null,
                notes = notes?.ifBlank { null }
            )

            val insertedId = dailyOperationDao.insertOperation(newOperation)
            Result.success(newOperation.copy(id = insertedId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        dailyOperationId: Long,
        category: String,
        amount: Double,
        notes: String? = null
    ): Result<ExpenseEntity> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("قيمة المصروف يجب أن تكون أكبر من صفر."))
            }

            if (category.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى اختيار أو كتابة تصنيف للمصروف."))
            }

            // Verify operation is active and OPEN
            val operation = dailyOperationDao.getOperationByIdDirect(dailyOperationId)
                ?: return@withContext Result.failure(IllegalArgumentException("يوم التشغيل غير موجود."))

            if (operation.status != DailyOperationStatus.OPEN) {
                return@withContext Result.failure(
                    IllegalStateException("لا يمكن إضافة مصروف ليوم تشغيل مغلق ومحفوظ.")
                )
            }

            val now = System.currentTimeMillis()
            val expense = ExpenseEntity(
                dailyOperationId = dailyOperationId,
                category = category.trim(),
                amount = amount,
                notes = notes?.trim()?.ifBlank { null },
                date = now,
                createdAt = now
            )

            val insertedId = expenseDao.insertExpense(expense)
            Result.success(expense.copy(id = insertedId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes Day Closing in an Atomic SQLite Transaction:
     * 1. Fetches current operation and validates status == OPEN.
     * 2. Computes all financial metrics directly from Room:
     *    - Total sales from SalesInvoices between openedAt and now
     *    - Cash sales
     *    - Credit sales
     *    - Total expenses from Expenses table
     *    - Net sales = Total sales - Expenses
     *    - Closing cash (expected in register) = Opening cash + Cash sales - Expenses
     *    - Difference = Actual cash - Closing cash
     * 3. Locks and updates operation to CLOSED.
     * 4. Prevents future modifications.
     */
    suspend fun closeDay(
        dailyOperationId: Long,
        actualCash: Double,
        notes: String? = null
    ): Result<DailyOperationEntity> = withContext(Dispatchers.IO) {
        try {
            if (actualCash < 0) {
                return@withContext Result.failure(IllegalArgumentException("المبلغ الفعلي في الخزينة لا يمكن أن يكون سالباً."))
            }

            val closedOperation = database.withTransaction {
                val operation = dailyOperationDao.getOperationByIdDirect(dailyOperationId)
                    ?: throw IllegalArgumentException("يوم التشغيل برقم $dailyOperationId غير موجود.")

                if (operation.status == DailyOperationStatus.CLOSED) {
                    throw IllegalStateException("هذا اليوم مغلق بالفعل ومحفوظ، ولا يمكن إعادة إغلاقه أو تعديل أرقامه.")
                }

                val now = System.currentTimeMillis()
                val startTime = operation.openedAt
                val endTime = now

                // Calculate strictly from Room database queries
                val totalSales = salesDao.getSalesTotalBetweenDirect(startTime, endTime)
                val cashSales = salesDao.getCashTotalBetweenDirect(startTime, endTime)
                val creditSales = salesDao.getCreditTotalBetweenDirect(startTime, endTime)
                val expensesTotal = expenseDao.getTotalExpensesForOperationDirect(operation.id)

                val netSales = totalSales - expensesTotal
                val closingCashExpected = operation.openingCash + cashSales - expensesTotal
                val difference = actualCash - closingCashExpected

                val updatedOperation = operation.copy(
                    totalSales = totalSales,
                    cashSales = cashSales,
                    creditSales = creditSales,
                    expenses = expensesTotal,
                    netSales = netSales,
                    closingCash = closingCashExpected,
                    actualCash = actualCash,
                    difference = difference,
                    status = DailyOperationStatus.CLOSED,
                    closedAt = now,
                    notes = notes?.ifBlank { null } ?: operation.notes
                )

                dailyOperationDao.updateOperation(updatedOperation)
                updatedOperation
            }

            Result.success(closedOperation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
