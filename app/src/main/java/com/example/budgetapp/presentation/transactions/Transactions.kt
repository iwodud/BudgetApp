package com.example.budgetapp.presentation.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.AppContainer
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.navigation.Screen
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

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { transactionRepo.deleteTransaction(transaction) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: TransactionListViewModel = viewModel(factory = TransactionListViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Transaction?>(null) }

    deleteCandidate?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Usuń transakcję") },
            text = { Text("Czy na pewno chcesz usunąć tę transakcję?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteTransaction(t); deleteCandidate = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Ukryj filtry" else "Filtry") }
            }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { MonthSelector(state.filter.month, { viewModel.setMonth(FormatUtils.previousMonth(state.filter.month)) }, { viewModel.setMonth(FormatUtils.nextMonth(state.filter.month)) }) }
            if (showFilters) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Typ transakcji", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = state.filter.type == null, onClick = { viewModel.setTypeFilter(null) }, label = { Text("Wszystkie") }) }
                            items(TransactionType.entries) { type ->
                                FilterChip(selected = state.filter.type == type, onClick = { viewModel.setTypeFilter(if (state.filter.type == type) null else type) }, label = { Text(type.label) })
                            }
                        }
                        if (state.categories.isNotEmpty()) {
                            Text("Kategoria", style = MaterialTheme.typography.labelMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { FilterChip(selected = state.filter.categoryId == null, onClick = { viewModel.setCategoryFilter(null) }, label = { Text("Wszystkie") }) }
                                items(state.categories) { cat ->
                                    FilterChip(selected = state.filter.categoryId == cat.id, onClick = { viewModel.setCategoryFilter(if (state.filter.categoryId == cat.id) null else cat.id) }, label = { Text(cat.name) })
                                }
                            }
                        }
                    }
                }
            }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.transactions.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Brak transakcji", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionItem(transaction, state.categories.find { it.id == transaction.categoryId },
                        onClick = { navController.navigate(Screen.AddEditTransaction.createRoute(transaction.id)) },
                        onDelete = { deleteCandidate = transaction })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        } // Column
    }
}

@Composable
fun MonthSelector(month: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.ArrowBack, "Poprzedni miesiąc") }
        Text(FormatUtils.monthToDisplay(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ArrowForward, "Następny miesiąc") }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, category: Category?, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE, TransactionType.SAVE_TO_JAR -> MaterialTheme.colorScheme.error
        TransactionType.INCOME, TransactionType.WITHDRAW_FROM_JAR -> if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    }
    val amountPrefix = when (transaction.type) {
        TransactionType.EXPENSE, TransactionType.SAVE_TO_JAR -> "−"
        TransactionType.INCOME, TransactionType.WITHDRAW_FROM_JAR -> "+"
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: transaction.type.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (!transaction.description.isNullOrBlank()) Text(transaction.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.date != null) Text(FormatUtils.formatDate(transaction.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$amountPrefix${FormatUtils.formatAmount(transaction.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = amountColor)
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
