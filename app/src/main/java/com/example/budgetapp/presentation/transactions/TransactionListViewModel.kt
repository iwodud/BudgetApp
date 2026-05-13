package com.example.budgetapp.presentation.transactions

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

data class TransactionFilter(
    val month: String = FormatUtils.currentMonth(),
    val categoryId: Long? = null,
    val type: TransactionType? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val isLoading: Boolean = true
)

class TransactionListViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionListState())
    val state: StateFlow<TransactionListState> = _state.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())

    init {
        viewModelScope.launch {
            combine(
                _filter.flatMapLatest { f -> transactionRepo.getTransactionsByMonth(f.month) },
                categoryRepo.getAllCategories(),
                _filter
            ) { transactions, categories, filter ->
                val filtered = transactions.filter { t ->
                    (filter.categoryId == null || t.categoryId == filter.categoryId) &&
                    (filter.type == null || t.type == filter.type) &&
                    (filter.minAmount == null || t.amount >= filter.minAmount) &&
                    (filter.maxAmount == null || t.amount <= filter.maxAmount)
                }
                _state.update { it.copy(transactions = filtered, categories = categories, filter = filter, isLoading = false) }
            }.collect()
        }
    }

    fun setMonth(month: String) { _filter.update { it.copy(month = month) } }
    fun setCategoryFilter(categoryId: Long?) { _filter.update { it.copy(categoryId = categoryId) } }
    fun setTypeFilter(type: TransactionType?) { _filter.update { it.copy(type = type) } }
    fun setAmountRange(min: Double?, max: Double?) { _filter.update { it.copy(minAmount = min, maxAmount = max) } }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepo.deleteTransaction(transaction)
            if (transaction.type == TransactionType.SAVE_TO_JAR && transaction.savingsJarId != null) {
            } else if (transaction.type == TransactionType.WITHDRAW_FROM_JAR && transaction.savingsJarId != null) {
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TransactionListViewModel(container.transactionRepository, container.categoryRepository) as T
            }
        }
    }
}
