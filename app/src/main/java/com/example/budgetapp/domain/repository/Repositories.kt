package com.example.budgetapp.domain.repository

import com.example.budgetapp.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByMonth(month: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getExpensesByYear(year: String): List<Transaction>
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getAllCategoriesOnce(): List<Category>
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

interface BudgetPlanRepository {
    fun getBudgetPlansByMonth(month: String): Flow<List<BudgetPlan>>
    suspend fun getBudgetPlansByMonthOnce(month: String): List<BudgetPlan>
    suspend fun getBudgetPlan(categoryId: Long, month: String): BudgetPlan?
    suspend fun insertBudgetPlan(budgetPlan: BudgetPlan): Long
    suspend fun updateBudgetPlan(budgetPlan: BudgetPlan)
    suspend fun deleteBudgetPlan(budgetPlan: BudgetPlan)
    suspend fun deleteAllForMonth(month: String)
    suspend fun deleteForCategories(month: String, categoryIds: List<Long>)
    suspend fun copyFromPreviousMonth(fromMonth: String, toMonth: String)
    suspend fun copyForCategories(fromMonth: String, toMonth: String, categoryIds: List<Long>)
}

interface SavingsJarBudgetPlanRepository {
    fun getPlansByMonth(month: String): Flow<List<SavingsJarBudgetPlan>>
    suspend fun getPlan(jarId: Long, month: String): SavingsJarBudgetPlan?
    suspend fun insertPlan(plan: SavingsJarBudgetPlan): Long
    suspend fun updatePlan(plan: SavingsJarBudgetPlan)
    suspend fun deleteAllForMonth(month: String)
    suspend fun copyFromPreviousMonth(fromMonth: String, toMonth: String)
}

interface ZrzutkaRepository {
    fun getPersonsWithBalance(): Flow<List<ZrzutkaPerson>>
    fun getAllPersonsPlain(): Flow<List<ZrzutkaPerson>>
    fun getAllExpenses(): Flow<List<ZrzutkaExpense>>
    suspend fun insertPerson(name: String)
    suspend fun deletePerson(person: ZrzutkaPerson)
    suspend fun insertExpense(description: String, totalAmount: Double, date: Long, payerId: Long, splits: List<Pair<Long, Double>>)
    suspend fun deleteExpense(expense: ZrzutkaExpense)
    suspend fun settleWithPerson(personId: Long)
}

interface SavingsJarRepository {
    fun getAllSavingsJars(): Flow<List<SavingsJar>>
    suspend fun getSavingsJarById(id: Long): SavingsJar?
    suspend fun insertSavingsJar(jar: SavingsJar): Long
    suspend fun updateSavingsJar(jar: SavingsJar)
    suspend fun deleteSavingsJar(jar: SavingsJar)
    suspend fun addToAmount(id: Long, delta: Double)
}
