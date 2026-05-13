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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.transactions.MonthSelector

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
            confirmButton = {
                TextButton(onClick = { viewModel.copyFromPreviousMonth(); showCopyDialog = false }) { Text("Kopiuj") }
            },
            dismissButton = { TextButton(onClick = { showCopyDialog = false }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan budżetu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    IconButton(onClick = { showCopyDialog = true }) {
                        Icon(Icons.Default.ContentCopy, "Kopiuj z poprzedniego miesiąca")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                MonthSelector(
                    month = state.selectedMonth,
                    onPrevious = { viewModel.setMonth(FormatUtils.previousMonth(state.selectedMonth)) },
                    onNext = { viewModel.setMonth(FormatUtils.nextMonth(state.selectedMonth)) }
                )
            }

            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                items(state.rows) { row ->
                    BudgetRowCard(
                        row = row,
                        onSave = { amount -> viewModel.savePlan(row.category.id, amount) }
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun BudgetRowCard(row: CategoryBudgetRow, onSave: (Double) -> Unit) {
    var editText by remember(row.plan?.plannedAmount) {
        mutableStateOf(row.plan?.plannedAmount?.let { "%.2f".format(it) } ?: "")
    }
    val focusManager = LocalFocusManager.current
    val planned = editText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val progress = if (planned > 0) (row.spent / planned).coerceIn(0.0, 1.0).toFloat() else 0f
    val overBudget = row.spent > planned && planned > 0

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(row.category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "Wydano: ${FormatUtils.formatAmount(row.spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Plan (PLN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        val amount = editText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (amount > 0) onSave(amount)
                    }),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(onClick = {
                    focusManager.clearFocus()
                    val amount = editText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (amount > 0) onSave(amount)
                }) { Text("OK") }
            }
            if (planned > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    "${(progress * 100).toInt()}% planu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
