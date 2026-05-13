package com.example.budgetapp.domain.repository

import com.example.budgetapp.domain.model.BudgetPlan
import kotlinx.coroutines.flow.Flow

interface BudgetPlanRepository {
    fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlan>>
    suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlan>
    suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlan?
    suspend fun insertBudgetPlan(budgetPlan: BudgetPlan): Long
    suspend fun updateBudgetPlan(budgetPlan: BudgetPlan)
    suspend fun deleteBudgetPlan(budgetPlan: BudgetPlan)
    suspend fun deleteAllForMonth(month: String)
    suspend fun copyFromPreviousMonth(fromMonth: String, toMonth: String)
}
