package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear LIMIT 1")
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForMonthDirect(monthYear: String): MonthlyBudget?

    @Query("SELECT * FROM budgets ORDER BY monthYear DESC")
    fun getAllBudgets(): Flow<List<MonthlyBudget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSnapshot(): List<MonthlyBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthlyBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<MonthlyBudget>)

    @Query("DELETE FROM budgets WHERE monthYear = :monthYear")
    suspend fun deleteBudget(monthYear: String)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
