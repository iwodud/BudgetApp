package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(transaction: TransactionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(transactions: List<TransactionEntity>)
    @Update suspend fun update(transaction: TransactionEntity)
    @Delete suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY COALESCE(date, created_at) DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE strftime('%Y-%m', datetime(COALESCE(date, created_at) / 1000, 'unixepoch')) = :month
        ORDER BY COALESCE(date, created_at) DESC
    """)
    fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE strftime('%Y', datetime(COALESCE(date, created_at) / 1000, 'unixepoch')) = :year
        AND type = 'EXPENSE'
        ORDER BY COALESCE(date, created_at) DESC
    """)
    suspend fun getExpensesByYear(year: String): List<TransactionEntity>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(category: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(categories: List<CategoryEntity>): List<Long>
    @Update suspend fun update(category: CategoryEntity)
    @Delete suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY is_default DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY is_default DESC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY is_default DESC, name ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>
}

@Dao
interface BudgetPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(budgetPlan: BudgetPlanEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(budgetPlans: List<BudgetPlanEntity>)
    @Update suspend fun update(budgetPlan: BudgetPlanEntity)
    @Delete suspend fun delete(budgetPlan: BudgetPlanEntity)

    @Query("SELECT * FROM budget_plans WHERE month = :month")
    fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE month = :month")
    suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlanEntity>

    @Query("SELECT * FROM budget_plans WHERE category_id = :categoryId AND month = :month LIMIT 1")
    suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlanEntity?

    @Query("DELETE FROM budget_plans WHERE month = :month")
    suspend fun deleteAllForMonth(month: String)
}

@Dao
interface SavingsJarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(jar: SavingsJarEntity): Long
    @Update suspend fun update(jar: SavingsJarEntity)
    @Delete suspend fun delete(jar: SavingsJarEntity)

    @Query("SELECT * FROM savings_jars ORDER BY name ASC")
    fun getAllSavingsJars(): Flow<List<SavingsJarEntity>>

    @Query("SELECT * FROM savings_jars WHERE id = :id LIMIT 1")
    suspend fun getSavingsJarById(id: Long): SavingsJarEntity?

    @Query("SELECT * FROM savings_jars ORDER BY name ASC")
    suspend fun getAllSavingsJarsOnce(): List<SavingsJarEntity>

    @Query("UPDATE savings_jars SET current_amount = current_amount + :delta WHERE id = :id")
    suspend fun addToAmount(id: Long, delta: Double)
}
