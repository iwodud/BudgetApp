package com.example.budgetapp.presentation.fwn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.AppContainer
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.FwnPlannedExpense
import com.example.budgetapp.domain.model.FwnTransaction
import com.example.budgetapp.domain.model.FwnTransactionType
import com.example.budgetapp.domain.repository.FwnRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

private val MONTH_NAMES = listOf(
    "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
    "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
)
private val MONTH_SHORT = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

data class FwnState(
    val plannedExpenses: List<FwnPlannedExpense> = emptyList(),
    val transactions: List<FwnTransaction> = emptyList()
) {
    val balance: Double = transactions.sumOf { if (it.type == FwnTransactionType.DEPOSIT) it.amount else -it.amount }
    val yearlyTotal: Double = plannedExpenses.sumOf { it.amount }
    val monthlyRate: Double = yearlyTotal / 12.0
}

class FwnViewModel(private val repo: FwnRepository) : ViewModel() {
    private val _state = MutableStateFlow(FwnState())
    val state: StateFlow<FwnState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.getPlannedExpenses(), repo.getTransactions()) { expenses, txs ->
                FwnState(plannedExpenses = expenses, transactions = txs)
            }.collect { _state.value = it }
        }
    }

    fun addPlannedExpense(name: String, amount: Double, month: Int) =
        viewModelScope.launch { repo.insertPlannedExpense(name, amount, month) }

    fun deletePlannedExpense(expense: FwnPlannedExpense) =
        viewModelScope.launch { repo.deletePlannedExpense(expense) }

    fun addTransaction(type: FwnTransactionType, amount: Double, description: String, linkedExpenseId: Long? = null) =
        viewModelScope.launch { repo.addTransaction(type, amount, description, linkedExpenseId) }

    fun deleteTransaction(tx: FwnTransaction) =
        viewModelScope.launch { repo.deleteTransaction(tx) }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FwnViewModel(container.fwnRepository) as T
        }
    }
}

@Composable
fun FwnScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: FwnViewModel = viewModel(factory = FwnViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    val thisMonthPlanned = state.plannedExpenses.filter { it.month == currentMonth }.sumOf { it.amount }
    val isSufficient = state.balance + state.monthlyRate >= thisMonthPlanned

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var deleteExpenseCandidate by remember { mutableStateOf<FwnPlannedExpense?>(null) }
    var deleteTransactionCandidate by remember { mutableStateOf<FwnTransaction?>(null) }

    if (showAddExpenseDialog) {
        AddPlannedExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { name, amount, month ->
                viewModel.addPlannedExpense(name, amount, month)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            plannedExpenses = state.plannedExpenses,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { type, amount, description, linkedId ->
                viewModel.addTransaction(type, amount, description, linkedId)
                showAddTransactionDialog = false
            }
        )
    }

    deleteExpenseCandidate?.let { expense ->
        AlertDialog(
            onDismissRequest = { deleteExpenseCandidate = null },
            title = { Text("Usuń wydatek") },
            text = { Text("Usunąć \"${expense.name}\" z planowanych wydatków?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlannedExpense(expense); deleteExpenseCandidate = null }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteExpenseCandidate = null }) { Text("Anuluj") } }
        )
    }

    deleteTransactionCandidate?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTransactionCandidate = null },
            title = { Text("Usuń transakcję") },
            text = { Text("Usunąć tę transakcję z historii FWN?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTransaction(tx); deleteTransactionCandidate = null }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTransactionCandidate = null }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { showAddExpenseDialog = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Dodaj planowany wydatek")
                }
                FloatingActionButton(onClick = { showAddTransactionDialog = true }) {
                    Icon(Icons.Default.AccountBalance, contentDescription = "Wpłać / Wypłać")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FwnSummaryCard(
                    balance = state.balance,
                    monthlyRate = state.monthlyRate,
                    thisMonthPlanned = thisMonthPlanned,
                    isSufficient = isSufficient,
                    currentMonth = currentMonth
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Planowane wydatki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.yearlyTotal > 0) {
                        Text(
                            "${FormatUtils.formatAmount(state.yearlyTotal)}/rok",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.plannedExpenses.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Brak planowanych wydatków.\nDodaj np. OC, wakacje, przegląd auta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(state.plannedExpenses, key = { "expense_${it.id}" }) { expense ->
                    PlannedExpenseCard(expense = expense, onDelete = { deleteExpenseCandidate = expense })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                Text("Historia FWN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (state.transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Brak transakcji.\nDodaj pierwszą wpłatę, żeby zacząć budować fundusz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(state.transactions, key = { "tx_${it.id}" }) { tx ->
                    FwnTransactionCard(tx = tx, onDelete = { deleteTransactionCandidate = tx })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FwnSummaryCard(
    balance: Double,
    monthlyRate: Double,
    thisMonthPlanned: Double,
    isSufficient: Boolean,
    currentMonth: Int
) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errorColor = MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Saldo FWN",
                style = MaterialTheme.typography.labelMedium,
                color = onContainer.copy(alpha = 0.7f)
            )
            Text(
                FormatUtils.formatAmount(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) onContainer else errorColor
            )
            HorizontalDivider(color = onContainer.copy(alpha = 0.2f))
            Text(
                MONTH_NAMES[currentMonth - 1],
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.7f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthActionChip(
                    icon = Icons.Default.ArrowUpward,
                    label = "Wpłać w tym miesiącu",
                    amount = monthlyRate,
                    tint = onContainer,
                    modifier = Modifier.weight(1f)
                )
                MonthActionChip(
                    icon = Icons.Default.ArrowDownward,
                    label = "Wydatki w tym miesiącu",
                    amount = thisMonthPlanned,
                    tint = if (thisMonthPlanned > 0) errorColor else onContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
            }
            if (thisMonthPlanned > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (isSufficient) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSufficient) onContainer else errorColor
                    )
                    Text(
                        if (isSufficient) "Saldo wystarczy na ten miesiąc"
                        else "Saldo może nie wystarczyć na ten miesiąc",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSufficient) onContainer else errorColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: Double,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                FormatUtils.formatAmount(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

@Composable
private fun PlannedExpenseCard(expense: FwnPlannedExpense, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        MONTH_SHORT[expense.month - 1],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${FormatUtils.formatAmount(expense.amount)}/rok",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${FormatUtils.formatAmount(expense.amount / 12)}/mies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FwnTransactionCard(tx: FwnTransaction, onDelete: () -> Unit) {
    val isDeposit = tx.type == FwnTransactionType.DEPOSIT
    val positiveColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    val amountColor = if (isDeposit) positiveColor else MaterialTheme.colorScheme.error
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isDeposit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.description.ifBlank { if (isDeposit) "Wpłata" else "Wypłata" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    FormatUtils.formatDate(tx.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (isDeposit) "+" else "−") + FormatUtils.formatAmount(tx.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddPlannedExpenseDialog(onDismiss: () -> Unit, onSave: (String, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planowany wydatek") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa (np. Ubezpieczenie OC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Kwota roczna (zł)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Miesiąc", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0..2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0..3) {
                                val monthIdx = row * 4 + col + 1
                                FilterChip(
                                    selected = selectedMonth == monthIdx,
                                    onClick = { selectedMonth = monthIdx },
                                    label = { Text(MONTH_SHORT[monthIdx - 1], style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountStr.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                if (name.isNotBlank() && amount > 0) onSave(name.trim(), amount, selectedMonth)
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
fun AddTransactionDialog(
    plannedExpenses: List<FwnPlannedExpense>,
    onDismiss: () -> Unit,
    onSave: (FwnTransactionType, Double, String, Long?) -> Unit
) {
    var isDeposit by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var linkedExpense by remember { mutableStateOf<FwnPlannedExpense?>(null) }

    LaunchedEffect(linkedExpense) {
        linkedExpense?.let {
            amountStr = it.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            description = it.name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transakcja FWN") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isDeposit,
                        onClick = { isDeposit = true; linkedExpense = null },
                        label = { Text("Wpłata") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isDeposit,
                        onClick = { isDeposit = false },
                        label = { Text("Wypłata") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isDeposit && plannedExpenses.isNotEmpty()) {
                    Text(
                        "Powiąż z planowanym wydatkiem:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = linkedExpense == null,
                            onClick = { linkedExpense = null },
                            label = { Text("Bez powiązania") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        plannedExpenses.forEach { expense ->
                            FilterChip(
                                selected = linkedExpense?.id == expense.id,
                                onClick = { linkedExpense = expense },
                                label = {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(expense.name, modifier = Modifier.weight(1f))
                                        Text(
                                            FormatUtils.formatAmount(expense.amount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Kwota (zł)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isDeposit) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis (opcjonalnie)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountStr.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                if (amount > 0) {
                    onSave(
                        if (isDeposit) FwnTransactionType.DEPOSIT else FwnTransactionType.WITHDRAWAL,
                        amount,
                        description.trim(),
                        linkedExpense?.id
                    )
                }
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
