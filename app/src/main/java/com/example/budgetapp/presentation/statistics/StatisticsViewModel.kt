package com.example.budgetapp.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryStats(val category: Category, val amount: Double, val percentage: Float)

data class StatisticsState(
    val selectedMonth: String = FormatUtils.currentMonth(),
    val selectedYear: String = FormatUtils.currentYear(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val largestCategory: String = "",
    val avgExpense: Double = 0.0,
    val categoryStats: List<CategoryStats> = emptyList(),
    val categories: List<Category> = emptyList()
)

class StatisticsViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    private val _month = MutableStateFlow(FormatUtils.currentMonth())

    init {
        viewModelScope.launch {
            combine(
                _month.flatMapLatest { transactionRepo.getTransactionsByMonth(it) },
                categoryRepo.getAllCategories(),
                _month
            ) { transactions, categories, month ->
                val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                val incomes = transactions.filter { it.type == TransactionType.INCOME }
                val total = expenses.sumOf { it.amount }
                val catMap = categories.associateBy { it.id }
                val grouped = expenses.groupBy { it.categoryId }
                    .mapValues { it.value.sumOf { t -> t.amount } }
                val catStats = grouped.mapNotNull { (catId, amount) ->
                    val cat = catMap[catId] ?: return@mapNotNull null
                    CategoryStats(cat, amount, if (total > 0) (amount / total * 100).toFloat() else 0f)
                }.sortedByDescending { it.amount }

                _state.update { it.copy(
                    selectedMonth = month,
                    totalExpense = total,
                    totalIncome = incomes.sumOf { t -> t.amount },
                    largestCategory = catStats.firstOrNull()?.category?.name ?: "—",
                    avgExpense = if (expenses.isNotEmpty()) total / expenses.size else 0.0,
                    categoryStats = catStats,
                    categories = categories
                )}
            }.collect()
        }
    }

    fun setMonth(month: String) { _month.update { month } }

    fun getExpensesByYearForExport(year: String, onResult: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            val result = transactionRepo.getExpensesByYear(year)
            onResult(result)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return StatisticsViewModel(container.transactionRepository, container.categoryRepository) as T
            }
        }
    }
}
