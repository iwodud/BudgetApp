package com.example.budgetapp.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val selectedCategoryId: Long? = null,
    val selectedJarId: Long? = null,
    val description: String = "",
    val date: Long? = null,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val savingsJars: List<SavingsJar> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

class AddEditTransactionViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val savingsJarRepo: SavingsJarRepository,
    private val transactionId: Long = -1L
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val expenseCats = categoryRepo.getAllCategoriesOnce().filter { it.type == CategoryType.EXPENSE }
            val incomeCats = categoryRepo.getAllCategoriesOnce().filter { it.type == CategoryType.INCOME }
            val jars = savingsJarRepo.getAllSavingsJars().first()
            _state.update { it.copy(expenseCategories = expenseCats, incomeCategories = incomeCats, savingsJars = jars) }

            if (transactionId > 0) {
                val t = transactionRepo.getTransactionById(transactionId)
                if (t != null) {
                    _state.update { it.copy(
                        type = t.type,
                        amountText = t.amount.toString(),
                        selectedCategoryId = t.categoryId,
                        selectedJarId = t.savingsJarId,
                        description = t.description ?: "",
                        date = t.date,
                        isEditing = true
                    )}
                }
            }
        }
    }

    fun setType(type: TransactionType) {
        _state.update { it.copy(type = type, selectedCategoryId = null, selectedJarId = null) }
    }

    fun setAmount(text: String) { _state.update { it.copy(amountText = text, error = null) } }
    fun setCategory(id: Long) { _state.update { it.copy(selectedCategoryId = id) } }
    fun setJar(id: Long) { _state.update { it.copy(selectedJarId = id) } }
    fun setDescription(text: String) { _state.update { it.copy(description = text) } }
    fun setDate(timestamp: Long?) { _state.update { it.copy(date = timestamp) } }

    fun save() {
        val s = _state.value
        val amount = s.amountText.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.update { it.copy(error = "Podaj prawidłową kwotę") }
            return
        }
        val needsCategory = s.type == TransactionType.EXPENSE || s.type == TransactionType.INCOME
        if (needsCategory && s.selectedCategoryId == null) {
            _state.update { it.copy(error = "Wybierz kategorię") }
            return
        }
        val needsJar = s.type == TransactionType.SAVE_TO_JAR || s.type == TransactionType.WITHDRAW_FROM_JAR
        if (needsJar && s.selectedJarId == null) {
            _state.update { it.copy(error = "Wybierz skarbonkę") }
            return
        }

        viewModelScope.launch {
            val transaction = Transaction(
                id = if (s.isEditing) transactionId else 0L,
                type = s.type,
                amount = amount,
                categoryId = s.selectedCategoryId,
                savingsJarId = s.selectedJarId,
                description = s.description.takeIf { it.isNotBlank() },
                date = s.date
            )
            if (s.isEditing) {
                transactionRepo.updateTransaction(transaction)
            } else {
                transactionRepo.insertTransaction(transaction)
                if (s.type == TransactionType.SAVE_TO_JAR && s.selectedJarId != null) {
                    savingsJarRepo.addToAmount(s.selectedJarId, amount)
                } else if (s.type == TransactionType.WITHDRAW_FROM_JAR && s.selectedJarId != null) {
                    savingsJarRepo.addToAmount(s.selectedJarId, -amount)
                }
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        fun factory(container: AppContainer, transactionId: Long = -1L) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AddEditTransactionViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.savingsJarRepository,
                    transactionId
                ) as T
            }
        }
    }
}
