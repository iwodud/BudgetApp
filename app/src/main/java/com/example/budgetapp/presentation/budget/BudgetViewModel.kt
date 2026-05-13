package com.example.budgetapp.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.BudgetPlan
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryBudgetRow(
    val category: Category,
    val plan: BudgetPlan?,
    val spent: Double
)

data class BudgetState(
    val selectedMonth: String = FormatUtils.currentMonth(),
    val rows: List<CategoryBudgetRow> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetViewModel(
    private val budgetPlanRepo: BudgetPlanRepository,
    private val categoryRepo: CategoryRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()

    private val _month = MutableStateFlow(FormatUtils.currentMonth())

    init {
        viewModelScope.launch {
            combine(
                _month.flatMapLatest { budgetPlanRepo.getBudgetPlansByMonth(it) },
                categoryRepo.getCategoriesByType(CategoryType.EXPENSE),
                _month.flatMapLatest { transactionRepo.getTransactionsByMonth(it) },
                _month
            ) { plans, categories, transactions, month ->
                val planMap = plans.associateBy { it.categoryId }
                val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                val spentMap = expenses.groupBy { it.categoryId }
                    .mapValues { it.value.sumOf { t -> t.amount } }
                val rows = categories.map { cat ->
                    CategoryBudgetRow(cat, planMap[cat.id], spentMap[cat.id] ?: 0.0)
                }
                _state.update { it.copy(selectedMonth = month, rows = rows, isLoading = false) }
            }.collect()
        }
    }

    fun setMonth(month: String) { _month.update { month } }

    fun savePlan(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val month = _month.value
            val existing = budgetPlanRepo.getBudgetPlan(categoryId, month)
            if (existing != null) {
                budgetPlanRepo.updateBudgetPlan(existing.copy(plannedAmount = amount))
            } else {
                budgetPlanRepo.insertBudgetPlan(BudgetPlan(categoryId = categoryId, month = month, plannedAmount = amount))
            }
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val currentMonth = _month.value
            val prevMonth = FormatUtils.previousMonth(currentMonth)
            budgetPlanRepo.deleteAllForMonth(currentMonth)
            budgetPlanRepo.copyFromPreviousMonth(prevMonth, currentMonth)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(
                    container.budgetPlanRepository,
                    container.categoryRepository,
                    container.transactionRepository
                ) as T
            }
        }
    }
}
