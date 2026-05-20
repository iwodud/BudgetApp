package com.example.budgetapp.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.budgetapp.domain.model.BudgetPlan
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.navigation.Screen
import com.example.budgetapp.presentation.transactions.TransactionItem
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
        val month = FormatUtils.currentMonth()
        viewModelScope.launch {
            combine(
                transactionRepo.getTransactionsByMonth(month),
                categoryRepo.getAllCategories(),
                budgetPlanRepo.getBudgetPlansByMonth(month)
            ) { transactions, categories, plans ->
                val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                val incomes = transactions.filter { it.type == TransactionType.INCOME }
                val expenseCategoryIds = categories.filter { it.type == CategoryType.EXPENSE }.map { it.id }.toSet()
                _state.update { it.copy(
                    currentMonth = month,
                    totalIncome = incomes.sumOf { t -> t.amount },
                    totalExpense = expenses.sumOf { t -> t.amount },
                    balance = incomes.sumOf { t -> t.amount } - expenses.sumOf { t -> t.amount },
                    recentTransactions = transactions.take(8),
                    categories = categories,
                    budgetPlans = plans,
                    totalPlanned = plans.filter { it.categoryId in expenseCategoryIds }.sumOf { p -> p.plannedAmount },
                    totalSpent = expenses.sumOf { t -> t.amount }
                )}
            }.collect()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(container.transactionRepository, container.categoryRepository, container.budgetPlanRepository) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj transakcję")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(FormatUtils.monthToDisplay(state.currentMonth), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Przychody", state.totalIncome, Color(0xFF388E3C), Modifier.weight(1f))
                    SummaryCard("Wydatki", state.totalExpense, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Bilans miesiąca", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(FormatUtils.formatAmount(state.balance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = if (state.balance >= 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (state.totalPlanned > 0) {
                item { BudgetProgressCard(spent = state.totalSpent, planned = state.totalPlanned) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navController.navigate(Screen.Budget.route) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Plan budżetu")
                    }
                    OutlinedButton(onClick = { navController.navigate(Screen.Savings.route) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Savings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Oszczędności")
                    }
                }
            }
            if (state.recentTransactions.isNotEmpty()) {
                item { Text("Ostatnie transakcje", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                items(state.recentTransactions) { transaction ->
                    TransactionItem(transaction, state.categories.find { it.id == transaction.categoryId },
                        onClick = { navController.navigate(Screen.AddEditTransaction.createRoute(transaction.id)) })
                }
            } else {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Brak transakcji w tym miesiącu", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(FormatUtils.formatAmount(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BudgetProgressCard(spent: Double, planned: Double) {
    val progress = if (planned > 0) (spent / planned).coerceIn(0.0, 1.0).toFloat() else 0f
    val overBudget = spent > planned
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Budżet miesięczny", style = MaterialTheme.typography.labelMedium)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(),
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("${FormatUtils.formatAmount(spent)} z ${FormatUtils.formatAmount(planned)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
