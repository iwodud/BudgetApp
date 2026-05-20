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
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.BudgetPlanRepository
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.SavingsJarBudgetPlanRepository
import com.example.budgetapp.domain.repository.SavingsJarRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.transactions.MonthSelector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BudgetTab(val label: String) {
    EXPENSE("Wydatki"),
    INCOME("Przychody"),
    SAVINGS("Oszczędności")
}

data class CategoryBudgetRow(val category: Category, val plan: BudgetPlan?, val actual: Double)

data class SavingsJarBudgetRow(
    val jar: SavingsJar,
    val plan: SavingsJarBudgetPlan?,
    val savedThisMonth: Double,
    val withdrawnThisMonth: Double
)

data class BudgetState(
    val selectedMonth: String = FormatUtils.currentMonth(),
    val selectedTab: BudgetTab = BudgetTab.EXPENSE,
    val expenseRows: List<CategoryBudgetRow> = emptyList(),
    val incomeRows: List<CategoryBudgetRow> = emptyList(),
    val savingsRows: List<SavingsJarBudgetRow> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetViewModel(
    private val budgetPlanRepo: BudgetPlanRepository,
    private val categoryRepo: CategoryRepository,
    private val transactionRepo: TransactionRepository,
    private val savingsJarRepo: SavingsJarRepository,
    private val jarBudgetPlanRepo: SavingsJarBudgetPlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()
    private val _month = MutableStateFlow(FormatUtils.currentMonth())

    init {
        viewModelScope.launch {
            val monthDependentFlow = _month.flatMapLatest { month ->
                combine(
                    budgetPlanRepo.getBudgetPlansByMonth(month),
                    jarBudgetPlanRepo.getPlansByMonth(month),
                    transactionRepo.getTransactionsByMonth(month)
                ) { plans, jarPlans, transactions -> Triple(plans, jarPlans, transactions) }
            }
            val staticFlow = combine(
                categoryRepo.getCategoriesByType(CategoryType.EXPENSE),
                categoryRepo.getCategoriesByType(CategoryType.INCOME),
                savingsJarRepo.getAllSavingsJars()
            ) { expCats, incCats, jars -> Triple(expCats, incCats, jars) }

            combine(monthDependentFlow, staticFlow, _month) { (plans, jarPlans, transactions), (expCats, incCats, jars), month ->
                val planMap = plans.associateBy { it.categoryId }
                val jarPlanMap = jarPlans.associateBy { it.savingsJarId }
                val expSpentMap = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
                val incEarnedMap = transactions.filter { it.type == TransactionType.INCOME }
                    .groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amount } }
                val savedMap = transactions.filter { it.type == TransactionType.SAVE_TO_JAR }
                    .groupBy { it.savingsJarId }.mapValues { e -> e.value.sumOf { it.amount } }
                val withdrawnMap = transactions.filter { it.type == TransactionType.WITHDRAW_FROM_JAR }
                    .groupBy { it.savingsJarId }.mapValues { e -> e.value.sumOf { it.amount } }

                _state.update {
                    it.copy(
                        selectedMonth = month,
                        expenseRows = expCats.map { cat -> CategoryBudgetRow(cat, planMap[cat.id], expSpentMap[cat.id] ?: 0.0) },
                        incomeRows = incCats.map { cat -> CategoryBudgetRow(cat, planMap[cat.id], incEarnedMap[cat.id] ?: 0.0) },
                        savingsRows = jars.map { jar -> SavingsJarBudgetRow(jar, jarPlanMap[jar.id], savedMap[jar.id] ?: 0.0, withdrawnMap[jar.id] ?: 0.0) },
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun setMonth(month: String) { _month.update { month } }

    fun setTab(tab: BudgetTab) { _state.update { it.copy(selectedTab = tab) } }

    fun saveCategoryPlan(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val month = _month.value
            val existing = budgetPlanRepo.getBudgetPlan(categoryId, month)
            if (existing != null) budgetPlanRepo.updateBudgetPlan(existing.copy(plannedAmount = amount))
            else budgetPlanRepo.insertBudgetPlan(BudgetPlan(categoryId = categoryId, month = month, plannedAmount = amount))
        }
    }

    fun saveJarPlan(jarId: Long, saveAmount: Double, withdrawAmount: Double) {
        viewModelScope.launch {
            val month = _month.value
            val existing = jarBudgetPlanRepo.getPlan(jarId, month)
            if (existing != null) jarBudgetPlanRepo.updatePlan(existing.copy(plannedSaveAmount = saveAmount, plannedWithdrawAmount = withdrawAmount))
            else jarBudgetPlanRepo.insertPlan(SavingsJarBudgetPlan(savingsJarId = jarId, month = month, plannedSaveAmount = saveAmount, plannedWithdrawAmount = withdrawAmount))
        }
    }

    fun copyFromPreviousMonth() {
        viewModelScope.launch {
            val currentMonth = _month.value
            val prevMonth = FormatUtils.previousMonth(currentMonth)
            when (_state.value.selectedTab) {
                BudgetTab.EXPENSE -> {
                    val ids = _state.value.expenseRows.map { it.category.id }
                    budgetPlanRepo.deleteForCategories(currentMonth, ids)
                    budgetPlanRepo.copyForCategories(prevMonth, currentMonth, ids)
                }
                BudgetTab.INCOME -> {
                    val ids = _state.value.incomeRows.map { it.category.id }
                    budgetPlanRepo.deleteForCategories(currentMonth, ids)
                    budgetPlanRepo.copyForCategories(prevMonth, currentMonth, ids)
                }
                BudgetTab.SAVINGS -> {
                    jarBudgetPlanRepo.deleteAllForMonth(currentMonth)
                    jarBudgetPlanRepo.copyFromPreviousMonth(prevMonth, currentMonth)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(
                    container.budgetPlanRepository,
                    container.categoryRepository,
                    container.transactionRepository,
                    container.savingsJarRepository,
                    container.savingsJarBudgetPlanRepository
                ) as T
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

    val prevMonthDisplay = FormatUtils.monthToDisplay(FormatUtils.previousMonth(state.selectedMonth))
    val currentMonthDisplay = FormatUtils.monthToDisplay(state.selectedMonth)

    if (showCopyDialog) {
        AlertDialog(
            onDismissRequest = { showCopyDialog = false },
            title = { Text("Kopiuj plan") },
            text = { Text("Skopiować plan ${state.selectedTab.label.lowercase()} z $prevMonthDisplay do $currentMonthDisplay? Obecny plan zostanie zastąpiony.") },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                BudgetTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MonthSelector(
                        state.selectedMonth,
                        { viewModel.setMonth(FormatUtils.previousMonth(state.selectedMonth)) },
                        { viewModel.setMonth(FormatUtils.nextMonth(state.selectedMonth)) }
                    )
                }
                if (state.isLoading) {
                    item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else {
                    when (state.selectedTab) {
                        BudgetTab.EXPENSE -> items(state.expenseRows) { row ->
                            CategoryBudgetRowCard(row, actualLabel = "Wydano", overBudgetIsError = true) { viewModel.saveCategoryPlan(row.category.id, it) }
                        }
                        BudgetTab.INCOME -> items(state.incomeRows) { row ->
                            CategoryBudgetRowCard(row, actualLabel = "Uzyskano", overBudgetIsError = false) { viewModel.saveCategoryPlan(row.category.id, it) }
                        }
                        BudgetTab.SAVINGS -> items(state.savingsRows) { row ->
                            SavingsJarBudgetRowCard(row) { saveAmt, withdrawAmt -> viewModel.saveJarPlan(row.jar.id, saveAmt, withdrawAmt) }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun CategoryBudgetRowCard(
    row: CategoryBudgetRow,
    actualLabel: String,
    overBudgetIsError: Boolean,
    onSave: (Double) -> Unit
) {
    var editText by remember(row.plan?.plannedAmount) { mutableStateOf(row.plan?.plannedAmount?.let { "%.2f".format(it) } ?: "") }
    val focusManager = LocalFocusManager.current
    val planned = editText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val progress = if (planned > 0) (row.actual / planned).coerceIn(0.0, 1.0).toFloat() else 0f
    val isAlert = overBudgetIsError && row.actual > planned && planned > 0

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(row.category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("$actualLabel: ${FormatUtils.formatAmount(row.actual)}", style = MaterialTheme.typography.bodySmall,
                    color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editText, onValueChange = { editText = it },
                    label = { Text("Plan (PLN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        editText.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onSave(it) }
                    }),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Button(onClick = {
                    focusManager.clearFocus()
                    editText.replace(",", ".").toDoubleOrNull()?.let { if (it > 0) onSave(it) }
                }) { Text("OK") }
            }
            if (planned > 0) {
                LinearProgressIndicator(
                    progress = { progress }, modifier = Modifier.fillMaxWidth(),
                    color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text("${(progress * 100).toInt()}% planu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SavingsJarBudgetRowCard(row: SavingsJarBudgetRow, onSave: (Double, Double) -> Unit) {
    var saveText by remember(row.plan?.plannedSaveAmount) { mutableStateOf(row.plan?.plannedSaveAmount?.let { if (it > 0) "%.2f".format(it) else "" } ?: "") }
    var withdrawText by remember(row.plan?.plannedWithdrawAmount) { mutableStateOf(row.plan?.plannedWithdrawAmount?.let { if (it > 0) "%.2f".format(it) else "" } ?: "") }
    val focusManager = LocalFocusManager.current

    val plannedSave = saveText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val plannedWithdraw = withdrawText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val saveProgress = if (plannedSave > 0) (row.savedThisMonth / plannedSave).coerceIn(0.0, 1.0).toFloat() else 0f
    val withdrawProgress = if (plannedWithdraw > 0) (row.withdrawnThisMonth / plannedWithdraw).coerceIn(0.0, 1.0).toFloat() else 0f

    fun doSave() {
        focusManager.clearFocus()
        onSave(plannedSave, plannedWithdraw)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(row.jar.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

            // Wpłaty section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wpłaty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Wpłacono: ${FormatUtils.formatAmount(row.savedThisMonth)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = saveText, onValueChange = { saveText = it },
                        label = { Text("Plan wpłat (PLN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                if (plannedSave > 0) {
                    LinearProgressIndicator(progress = { saveProgress }, modifier = Modifier.fillMaxWidth())
                    Text("${(saveProgress * 100).toInt()}% planu wpłat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // Wypłaty section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wypłaty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Pobrano: ${FormatUtils.formatAmount(row.withdrawnThisMonth)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = withdrawText, onValueChange = { withdrawText = it },
                        label = { Text("Plan wypłat (PLN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { doSave() }),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                if (plannedWithdraw > 0) {
                    LinearProgressIndicator(progress = { withdrawProgress }, modifier = Modifier.fillMaxWidth())
                    Text("${(withdrawProgress * 100).toInt()}% planu wypłat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = { doSave() }, modifier = Modifier.align(Alignment.End)) { Text("Zapisz") }
        }
    }
}
