package com.example.budgetapp.presentation.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.transactions.MonthSelector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryBudgetRow(val category: Category, val plan: BudgetPlan?, val spent: Double)

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
                val spentMap = transactions.filter { it.type == TransactionType.EXPENSE }.groupBy { it.categoryId }.mapValues { it.value.sumOf { t -> t.amount } }
                _state.update { it.copy(selectedMonth = month, rows = categories.map { cat -> CategoryBudgetRow(cat, planMap[cat.id], spentMap[cat.id] ?: 0.0) }, isLoading = false) }
            }.collect()
        }
    }

    fun setMonth(month: String) { _month.update { month } }

    fun savePlan(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val month = _month.value
            val existing = budgetPlanRepo.getBudgetPlan(categoryId, month)
            if (existing != null) budgetPlanRepo.updateBudgetPlan(existing.copy(plannedAmount = amount))
            else budgetPlanRepo.insertBudgetPlan(BudgetPlan(categoryId = categoryId, month = month, plannedAmount = amount))
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val currentMonth = _month.value
            budgetPlanRepo.deleteAllForMonth(currentMonth)
            budgetPlanRepo.copyFromPreviousMonth(FormatUtils.previousMonth(currentMonth), currentMonth)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(container.budgetPlanRepository, container.categoryRepository, container.transactionRepository) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    var showCopyDialog by remember { mutableStateOf(false) }

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Kopiuj plan") },
            text = { Text("Skopiować plan z ${FormatUtils.monthToDisplay(FormatUtils.previousMonth(state.selectedMonth))} do ${FormatUtils.monthToDisplay(state.selectedMonth)}? Obecny plan zostanie zastąpiony.") },
            confirmButton = { TextButton(onClick = { viewModel.copyFromPreviousMonth(); showCopyDialog = false }) { Text("Kopiuj") } },
            dismissButton = { TextButton(onClick = { showCopyDialog = false }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Plan budżetu") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Wróć") } },
                actions = { IconButton(onClick = { showCopyDialog = true }) { Icon(Icons.Default.ContentCopy, "Kopiuj z poprzedniego miesiąca") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { MonthSelector(state.selectedMonth, { viewModel.setMonth(FormatUtils.previousMonth(state.selectedMonth)) }, { viewModel.setMonth(FormatUtils.nextMonth(state.selectedMonth)) }) }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                items(state.rows) { row -> BudgetRowCard(row) { amount -> viewModel.savePlan(row.category.id, amount) } }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun BudgetRowCard(row: CategoryBudgetRow, onSave: (Double) -> Unit) {
    var editText by remember(row.plan?.plannedAmount) { mutableStateOf(row.plan?.plannedAmount?.let { "%.2f".format(it) } ?: "") }
    val focusManager = LocalFocusManager.current
    val planned = editText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val progress = if (planned > 0) (row.spent / planned).coerceIn(0.0, 1.0).toFloat() else 0f
    val overBudget = row.spent > planned && planned > 0

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(row.category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Wydano: ${FormatUtils.formatAmount(row.spent)}", style = MaterialTheme.typography.bodySmall, color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = editText, onValueChange = { editText = it }, label = { Text("Plan (PLN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); editText.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onSave(it) } }),
                    modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = { focusManager.clearFocus(); editText.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onSave(it) } }) { Text("OK") }
            }
            if (planned > 0) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("${(progress * 100).toInt()}% planu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
