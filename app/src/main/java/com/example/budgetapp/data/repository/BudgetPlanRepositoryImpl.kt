package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.BudgetPlanDao
import com.example.budgetapp.data.local.entity.BudgetPlanEntity
import com.example.budgetapp.data.local.entity.toDomain
import com.example.budgetapp.data.local.entity.toEntity
import com.example.budgetapp.domain.model.BudgetPlan
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetPlanRepositoryImpl(private val dao: BudgetPlanDao) : BudgetPlanRepository {

    override fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlan>> =
        dao.getBudgetPlansByMonth(month).map { it.map { e -> e.toDomain() } }

    override suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlan> =
        dao.getBudgetPlansByMonthOnce(month).map { it.toDomain() }

    override suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlan? =
        dao.getBudgetPlan(categoryId, month)?.toDomain()

    override suspend fun insertBudgetPlan(budgetPlan: BudgetPlan): Long =
        dao.insert(budgetPlan.toEntity())

    override suspend fun updateBudgetPlan(budgetPlan: BudgetPlan) =
        dao.update(budgetPlan.toEntity())

    override suspend fun deleteBudgetPlan(budgetPlan: BudgetPlan) =
        dao.delete(budgetPlan.toEntity())

    override suspend fun deleteAllForMonth(month: String) =
        dao.deleteAllForMonth(month)

    override suspend fun copyFromPreviousMonth(fromMonth: String, toMonth: String) {
        val previous = dao.getBudgetPlansByMonthOnce(fromMonth)
        val copies = previous.map { BudgetPlanEntity(categoryId = it.categoryId, month = toMonth, plannedAmount = it.plannedAmount) }
        dao.insertAll(copies)
    }
}
