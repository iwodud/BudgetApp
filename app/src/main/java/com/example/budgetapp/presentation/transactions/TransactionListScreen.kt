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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.navigation.Screen

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
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTransaction(t); deleteCandidate = null }) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transakcje") },
                actions = {
                    TextButton(onClick = { showFilters = !showFilters }) {
                        Text(if (showFilters) "Ukryj filtry" else "Filtry")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddEditTransaction.createRoute()) }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                MonthSelector(
                    month = state.filter.month,
                    onPrevious = { viewModel.setMonth(FormatUtils.previousMonth(state.filter.month)) },
                    onNext = { viewModel.setMonth(FormatUtils.nextMonth(state.filter.month)) }
                )
            }

            if (showFilters) {
                item {
                    FilterSection(
                        state = state,
                        onCategoryFilter = { viewModel.setCategoryFilter(it) },
                        onTypeFilter = { viewModel.setTypeFilter(it) }
                    )
                }
            }

            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (state.transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Brak transakcji", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.transactions, key = { it.id }) { transaction ->
                    val category = state.categories.find { it.id == transaction.categoryId }
                    TransactionItem(
                        transaction = transaction,
                        category = category,
                        onClick = { navController.navigate(Screen.AddEditTransaction.createRoute(transaction.id)) },
                        onDelete = { deleteCandidate = transaction }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    state: TransactionListState,
    onCategoryFilter: (Long?) -> Unit,
    onTypeFilter: (TransactionType?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Typ transakcji", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.filter.type == null,
                    onClick = { onTypeFilter(null) },
                    label = { Text("Wszystkie") }
                )
            }
            items(TransactionType.entries) { type ->
                FilterChip(
                    selected = state.filter.type == type,
                    onClick = { onTypeFilter(if (state.filter.type == type) null else type) },
                    label = { Text(type.label) }
                )
            }
        }

        if (state.categories.isNotEmpty()) {
            Text("Kategoria", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter.categoryId == null,
                        onClick = { onCategoryFilter(null) },
                        label = { Text("Wszystkie") }
                    )
                }
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.filter.categoryId == cat.id,
                        onClick = { onCategoryFilter(if (state.filter.categoryId == cat.id) null else cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE, TransactionType.SAVE_TO_JAR -> MaterialTheme.colorScheme.error
        TransactionType.INCOME, TransactionType.WITHDRAW_FROM_JAR -> Color(0xFF388E3C)
    }
    val amountPrefix = when (transaction.type) {
        TransactionType.EXPENSE, TransactionType.SAVE_TO_JAR -> "−"
        TransactionType.INCOME, TransactionType.WITHDRAW_FROM_JAR -> "+"
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    category?.name ?: transaction.type.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!transaction.description.isNullOrBlank()) {
                    Text(transaction.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (transaction.date != null) {
                    Text(FormatUtils.formatDate(transaction.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$amountPrefix${FormatUtils.formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
