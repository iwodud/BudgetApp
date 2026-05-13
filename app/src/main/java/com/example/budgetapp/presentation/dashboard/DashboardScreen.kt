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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.navigation.Screen
import com.example.budgetapp.presentation.transactions.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("BudgetApp") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj transakcję")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    FormatUtils.monthToDisplay(state.currentMonth),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        label = "Przychody",
                        amount = state.totalIncome,
                        color = Color(0xFF388E3C),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Wydatki",
                        amount = state.totalExpense,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Bilans miesiąca", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            FormatUtils.formatAmount(state.balance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.balance >= 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (state.totalPlanned > 0) {
                item {
                    BudgetProgressCard(spent = state.totalSpent, planned = state.totalPlanned)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.Budget.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Plan budżetu")
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.Savings.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Savings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Oszczędności")
                    }
                }
            }

            if (state.recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        "Ostatnie transakcje",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.recentTransactions) { transaction ->
                    val category = state.categories.find { it.id == transaction.categoryId }
                    TransactionItem(
                        transaction = transaction,
                        category = category,
                        onClick = { navController.navigate(Screen.AddEditTransaction.createRoute(transaction.id)) }
                    )
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Brak transakcji w tym miesiącu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
            Text(
                FormatUtils.formatAmount(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
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
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${FormatUtils.formatAmount(spent)} z ${FormatUtils.formatAmount(planned)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
