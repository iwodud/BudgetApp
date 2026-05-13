package com.example.budgetapp.presentation.statistics

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.domain.repository.CategoryRepository
import com.example.budgetapp.domain.repository.TransactionRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.common.XlsxWriter
import com.example.budgetapp.presentation.transactions.MonthSelector
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
            combine(_month.flatMapLatest { transactionRepo.getTransactionsByMonth(it) }, categoryRepo.getAllCategories(), _month) { transactions, categories, month ->
                val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
                val total = expenses.sumOf { it.amount }
                val catStats = expenses.groupBy { it.categoryId }.mapNotNull { (catId, list) ->
                    val cat = categories.find { it.id == catId } ?: return@mapNotNull null
                    CategoryStats(cat, list.sumOf { it.amount }, if (total > 0) (list.sumOf { it.amount } / total * 100).toFloat() else 0f)
                }.sortedByDescending { it.amount }
                _state.update { it.copy(selectedMonth = month, totalExpense = total, totalIncome = transactions.filter { t -> t.type == TransactionType.INCOME }.sumOf { t -> t.amount }, largestCategory = catStats.firstOrNull()?.category?.name ?: "—", avgExpense = if (expenses.isNotEmpty()) total / expenses.size else 0.0, categoryStats = catStats, categories = categories) }
            }.collect()
        }
    }

    fun setMonth(month: String) { _month.update { month } }

    fun getExpensesByYearForExport(year: String, onResult: (List<Transaction>) -> Unit) {
        viewModelScope.launch { onResult(transactionRepo.getExpensesByYear(year)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    fun shareFile(filename: String, sheets: List<XlsxWriter.Sheet>) {
        val uri = XlsxWriter.createAndShare(context, filename, sheets)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Eksportuj"))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                Box {
                    TextButton(onClick = { showExportMenu = true }) { Text("Eksport") }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(text = { Text("Historia miesięczna") }, onClick = {
                            showExportMenu = false
                            viewModel.getExpensesByYearForExport(FormatUtils.currentYear()) { transactions ->
                                val cats = state.categories.associateBy { it.id }
                                val rows = transactions.filter { FormatUtils.timestampToMonth(it.date ?: it.createdAt) == state.selectedMonth }
                                    .map { t -> listOf(t.date?.let { FormatUtils.formatDate(it) } ?: "", cats[t.categoryId]?.name ?: "", t.description ?: "", t.amount) }
                                shareFile("historia_${state.selectedMonth}.xlsx", listOf(XlsxWriter.Sheet("Historia", listOf(listOf("Data", "Kategoria", "Opis", "Kwota (PLN)")) + rows)))
                            }
                        })
                        DropdownMenuItem(text = { Text("Podsumowanie kategorii") }, onClick = {
                            showExportMenu = false
                            shareFile("podsumowanie_${state.selectedMonth}.xlsx", listOf(XlsxWriter.Sheet("Kategorie",
                                listOf(listOf("Kategoria", "Suma wydatków (PLN)")) + state.categoryStats.map { listOf(it.category.name, it.amount) })))
                        })
                        DropdownMenuItem(text = { Text("Podsumowanie roczne") }, onClick = {
                            showExportMenu = false
                            viewModel.getExpensesByYearForExport(state.selectedYear) { transactions ->
                                val cats = state.categories.associateBy { it.id }
                                val months = (1..12).map { m -> "${state.selectedYear}-${m.toString().padStart(2, '0')}" }
                                val catNames = cats.values.filter { it.type.name == "EXPENSE" }.sortedBy { it.name }
                                val dataRows = catNames.map { cat ->
                                    val monthAmounts = months.map { m -> transactions.filter { t -> t.categoryId == cat.id && FormatUtils.timestampToMonth(t.date ?: t.createdAt) == m }.sumOf { it.amount } }
                                    listOf(cat.name) + monthAmounts + listOf(monthAmounts.sum())
                                }
                                shareFile("roczne_${state.selectedYear}.xlsx", listOf(XlsxWriter.Sheet("Rok ${state.selectedYear}",
                                    listOf(listOf("Kategoria") + months.map { FormatUtils.monthToDisplay(it) } + listOf("Razem")) + dataRows)))
                            }
                        })
                    }
                }
            }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MonthSelector(state.selectedMonth, { viewModel.setMonth(FormatUtils.previousMonth(state.selectedMonth)) }, { viewModel.setMonth(FormatUtils.nextMonth(state.selectedMonth)) }) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Wydatki", FormatUtils.formatAmount(state.totalExpense), Modifier.weight(1f))
                    StatCard("Przychody", FormatUtils.formatAmount(state.totalIncome), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Największa kategoria", state.largestCategory, Modifier.weight(1f))
                    StatCard("Średni wydatek", FormatUtils.formatAmount(state.avgExpense), Modifier.weight(1f))
                }
            }
            if (state.categoryStats.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Podział wydatków", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(16.dp))
                            SimplePieChart(state.categoryStats.map { Pair(colorFromHex(it.category.colorHex), it.amount.toFloat()) }, Modifier.size(180.dp).align(Alignment.CenterHorizontally))
                        }
                    }
                }
                items(state.categoryStats) { cs ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color = colorFromHex(cs.category.colorHex)) }
                        Spacer(Modifier.width(8.dp))
                        Text(cs.category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${cs.percentage.toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(FormatUtils.formatAmount(cs.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Brak wydatków w tym miesiącu", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SimplePieChart(segments: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0f) return
    Canvas(modifier = modifier) {
        var startAngle = -90f
        segments.forEach { (color, value) -> val sweep = value / total * 360f; drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = true); startAngle += sweep }
    }
}

fun colorFromHex(hex: String): Color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF6750A4) }
