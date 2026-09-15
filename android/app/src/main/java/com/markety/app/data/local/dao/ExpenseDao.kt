package com.markety.app.data.local.dao

import androidx.room.*
import com.markety.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM Expenses WHERE dailyOperationId = :dailyOperationId ORDER BY date DESC")
    fun getExpensesByOperationId(dailyOperationId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM Expenses WHERE dailyOperationId = :dailyOperationId ORDER BY date DESC")
    suspend fun getExpensesByOperationIdDirect(dailyOperationId: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM Expenses WHERE dailyOperationId = :dailyOperationId")
    fun getTotalExpensesForOperation(dailyOperationId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM Expenses WHERE dailyOperationId = :dailyOperationId")
    suspend fun getTotalExpensesForOperationDirect(dailyOperationId: Long): Double

    @Query("SELECT * FROM Expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
}
