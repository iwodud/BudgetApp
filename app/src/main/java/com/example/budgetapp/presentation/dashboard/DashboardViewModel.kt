package com.example.budgetapp.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.BudgetPlan
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardState(
    val currentMonth: String = FormatUtils.currentMonth(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgetPlans: List<BudgetPlan> = emptyList(),
    val totalPlanned: Double = 0.0,
    val totalSpent: Double = 0.0
)

class DashboardViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetPlanRepo: BudgetPlanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val month = FormatUtils.currentMonth()
        viewModelScope.launch {
            combine(
                transactionRepo.getTransactionsByMonth(month),
                categoryRepo.getAllCategories(),
                budgetPlanRepo.getBudgetPlansByMonth(month)
            ) { transactions, categories, plans ->
                val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                val incomes = transactions.filter { it.type == TransactionType.INCOME }
                val totalExpense = expenses.sumOf { it.amount }
                val totalIncome = incomes.sumOf { it.amount }
                val totalPlanned = plans.sumOf { it.plannedAmount }
                _state.update {
                    it.copy(
                        currentMonth = month,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        balance = totalIncome - totalExpense,
                        recentTransactions = transactions.take(8),
                        categories = categories,
                        budgetPlans = plans,
                        totalPlanned = totalPlanned,
                        totalSpent = totalExpense
                    )
                }
            }.collect()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.budgetPlanRepository
                ) as T
            }
        }
    }
}
