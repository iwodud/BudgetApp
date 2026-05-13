package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.*
import com.example.budgetapp.data.local.entity.*
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(private val dao: TransactionDao) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions().map { it.map { e -> e.toDomain() } }
    override fun getTransactionsByMonth(month: String): Flow<List<Transaction>> = dao.getTransactionsByMonth(month).map { it.map { e -> e.toDomain() } }
    override suspend fun getTransactionById(id: Long): Transaction? = dao.getTransactionById(id)?.toDomain()
    override suspend fun insertTransaction(transaction: Transaction): Long = dao.insert(transaction.toEntity())
    override suspend fun updateTransaction(transaction: Transaction) = dao.update(transaction.toEntity())
    override suspend fun deleteTransaction(transaction: Transaction) = dao.delete(transaction.toEntity())
    override suspend fun getExpensesByYear(year: String): List<Transaction> = dao.getExpensesByYear(year).map { it.toDomain() }
}

class CategoryRepositoryImpl(private val dao: CategoryDao) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = dao.getAllCategories().map { it.map { e -> e.toDomain() } }
    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = dao.getCategoriesByType(type.name).map { it.map { e -> e.toDomain() } }
    override suspend fun getAllCategoriesOnce(): List<Category> = dao.getAllCategoriesOnce().map { it.toDomain() }
    override suspend fun insertCategory(category: Category): Long = dao.insert(category.toEntity())
    override suspend fun updateCategory(category: Category) = dao.update(category.toEntity())
    override suspend fun deleteCategory(category: Category) = dao.delete(category.toEntity())
}

class BudgetPlanRepositoryImpl(private val dao: BudgetPlanDao) : BudgetPlanRepository {
    override fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlan>> = dao.getBudgetPlansByMonth(month).map { it.map { e -> e.toDomain() } }
    override suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlan> = dao.getBudgetPlansByMonthOnce(month).map { it.toDomain() }
    override suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlan? = dao.getBudgetPlan(categoryId, month)?.toDomain()
    override suspend fun insertBudgetPlan(budgetPlan: BudgetPlan): Long = dao.insert(budgetPlan.toEntity())
    override suspend fun updateBudgetPlan(budgetPlan: BudgetPlan) = dao.update(budgetPlan.toEntity())
    override suspend fun deleteBudgetPlan(budgetPlan: BudgetPlan) = dao.delete(budgetPlan.toEntity())
    override suspend fun deleteAllForMonth(month: String) = dao.deleteAllForMonth(month)
    override suspend fun copyFromPreviousMonth(fromMonth: String, toMonth: String) {
        val previous = dao.getBudgetPlansByMonthOnce(fromMonth)
        dao.insertAll(previous.map { BudgetPlanEntity(categoryId = it.categoryId, month = toMonth, plannedAmount = it.plannedAmount) })
    }
}

class SavingsJarRepositoryImpl(private val dao: SavingsJarDao) : SavingsJarRepository {
    override fun getAllSavingsJars(): Flow<List<SavingsJar>> = dao.getAllSavingsJars().map { it.map { e -> e.toDomain() } }
    override suspend fun getSavingsJarById(id: Long): SavingsJar? = dao.getSavingsJarById(id)?.toDomain()
    override suspend fun insertSavingsJar(jar: SavingsJar): Long = dao.insert(jar.toEntity())
    override suspend fun updateSavingsJar(jar: SavingsJar) = dao.update(jar.toEntity())
    override suspend fun deleteSavingsJar(jar: SavingsJar) = dao.delete(jar.toEntity())
    override suspend fun addToAmount(id: Long, delta: Double) = dao.addToAmount(id, delta)
}
