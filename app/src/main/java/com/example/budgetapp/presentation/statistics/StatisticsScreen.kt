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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.common.XlsxWriter
import com.example.budgetapp.presentation.transactions.MonthSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showExportMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statystyki") },
                actions = {
                    Box {
                        TextButton(onClick = { showExportMenu = true }) { Text("Eksport") }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(text = { Text("Historia miesięczna") }, onClick = {
                                showExportMenu = false
                                val cats = state.categories.associateBy { it.id }
                                val rows = listOf(listOf("Data", "Kategoria", "Opis", "Kwota")) +
                                    state.categoryStats.flatMap { cs ->
                                        emptyList<List<Any?>>()
                                    }
                                viewModel.getExpensesByYearForExport(FormatUtils.currentYear()) { transactions ->
                                    val monthTransactions = transactions.filter {
                                        FormatUtils.timestampToMonth(it.date ?: it.createdAt) == state.selectedMonth
                                    }
                                    val dataRows = monthTransactions.map { t ->
                                        listOf(
                                            t.date?.let { FormatUtils.formatDate(it) } ?: "",
                                            cats[t.categoryId]?.name ?: "",
                                            t.description ?: "",
                                            t.amount
                                        )
                                    }
                                    val header = listOf(listOf("Data", "Kategoria", "Opis", "Kwota (PLN)"))
                                    val uri = XlsxWriter.createAndShare(context, "historia_${state.selectedMonth}.xlsx",
                                        listOf(XlsxWriter.Sheet("Historia", header + dataRows)))
                                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }.let { Intent.createChooser(it, "Eksportuj") })
                                }
                            })
                            DropdownMenuItem(text = { Text("Podsumowanie kategorii") }, onClick = {
                                showExportMenu = false
                                val dataRows = state.categoryStats.map { cs -> listOf(cs.category.name, cs.amount) }
                                val header = listOf(listOf("Kategoria", "Suma wydatków (PLN)"))
                                val uri = XlsxWriter.createAndShare(context, "podsumowanie_${state.selectedMonth}.xlsx",
                                    listOf(XlsxWriter.Sheet("Kategorie", header + dataRows)))
                                context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }.let { Intent.createChooser(it, "Eksportuj") })
                            })
                            DropdownMenuItem(text = { Text("Podsumowanie roczne") }, onClick = {
                                showExportMenu = false
                                viewModel.getExpensesByYearForExport(state.selectedYear) { transactions ->
                                    val cats = state.categories.associateBy { it.id }
                                    val months = (1..12).map { m -> "${state.selectedYear}-${m.toString().padStart(2, '0')}" }
                                    val monthLabels = months.map { FormatUtils.monthToDisplay(it) }
                                    val header = listOf(listOf("Kategoria") + monthLabels + listOf("Razem"))
                                    val catNames = cats.values.filter { it.type.name == "EXPENSE" }.sortedBy { it.name }
                                    val dataRows = catNames.map { cat ->
                                        val monthAmounts = months.map { m ->
                                            transactions.filter { t ->
                                                t.categoryId == cat.id &&
                                                FormatUtils.timestampToMonth(t.date ?: t.createdAt) == m
                                            }.sumOf { it.amount }
                                        }
                                        listOf(cat.name) + monthAmounts + listOf(monthAmounts.sum())
                                    }
                                    val uri = XlsxWriter.createAndShare(context, "roczne_${state.selectedYear}.xlsx",
                                        listOf(XlsxWriter.Sheet("Rok ${state.selectedYear}", header + dataRows)))
                                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }.let { Intent.createChooser(it, "Eksportuj") })
                                }
                            })
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthSelector(
                    month = state.selectedMonth,
                    onPrevious = { viewModel.setMonth(FormatUtils.previousMonth(state.selectedMonth)) },
                    onNext = { viewModel.setMonth(FormatUtils.nextMonth(state.selectedMonth)) }
                )
            }

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
                            SimplePieChart(
                                segments = state.categoryStats.map {
                                    Pair(colorFromHex(it.category.colorHex), it.amount.toFloat())
                                },
                                modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                items(state.categoryStats) { cs ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).also {}, contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(color = colorFromHex(cs.category.colorHex))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(cs.category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${cs.percentage.toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(FormatUtils.formatAmount(cs.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Brak wydatków w tym miesiącu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
        segments.forEach { (color, value) ->
            val sweep = value / total * 360f
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = true)
            startAngle += sweep
        }
    }
}

fun colorFromHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF6750A4)
    }
}
