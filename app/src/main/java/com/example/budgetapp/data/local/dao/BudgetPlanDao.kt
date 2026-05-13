package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.BudgetPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budgetPlan: BudgetPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgetPlans: List<BudgetPlanEntity>)

    @Update
    suspend fun update(budgetPlan: BudgetPlanEntity)

    @Delete
    suspend fun delete(budgetPlan: BudgetPlanEntity)

    @Query("SELECT * FROM budget_plans WHERE month = :month")
    fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlanEntity>>

    @Query("SELECT * FROM budget_plans WHERE month = :month")
    suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlanEntity>

    @Query("SELECT * FROM budget_plans WHERE category_id = :categoryId AND month = :month LIMIT 1")
    suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlanEntity?

    @Query("DELETE FROM budget_plans WHERE month = :month")
    suspend fun deleteAllForMonth(month: String)
}
