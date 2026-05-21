package com.example.budgetapp.presentation.zrzutka

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.ZrzutkaRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ZrzutkaHistoriaState(
    val expenses: List<ZrzutkaExpense> = emptyList(),
    val persons: List<ZrzutkaPerson> = emptyList()
)

class ZrzutkaHistoriaViewModel(private val repo: ZrzutkaRepository) : ViewModel() {
    private val _state = MutableStateFlow(ZrzutkaHistoriaState())
    val state: StateFlow<ZrzutkaHistoriaState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.getAllExpenses(), repo.getAllPersonsPlain()) { expenses, persons ->
                ZrzutkaHistoriaState(expenses = expenses, persons = persons)
            }.collect { _state.value = it }
        }
    }

    fun deleteExpense(expense: ZrzutkaExpense) = viewModelScope.launch { repo.deleteExpense(expense) }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ZrzutkaHistoriaViewModel(container.zrzutkaRepository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZrzutkaHistoriaScreen(navController: NavController, initialPersonId: Long = -1L) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: ZrzutkaHistoriaViewModel = viewModel(factory = ZrzutkaHistoriaViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    var selectedPersonId by remember(initialPersonId) { mutableStateOf(initialPersonId) }
    var deleteCandidate by remember { mutableStateOf<ZrzutkaExpense?>(null) }

    val filtered = remember(state.expenses, selectedPersonId) {
        if (selectedPersonId == -1L) state.expenses
        else state.expenses.filter { exp ->
            exp.payerId == selectedPersonId || exp.splits.any { it.personId == selectedPersonId }
        }
    }

    deleteCandidate?.let { exp ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Usuń wydatek") },
            text = { Text("Usunąć wydatek \"${exp.description}\"?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteExpense(exp); deleteCandidate = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Historia") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = selectedPersonId == -1L, onClick = { selectedPersonId = -1L }, label = { Text("Wszystkie") })
                }
                items(state.persons) { person ->
                    FilterChip(
                        selected = selectedPersonId == person.id,
                        onClick = { selectedPersonId = if (selectedPersonId == person.id) -1L else person.id },
                        label = { Text(person.name) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak wydatków", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { expense ->
                        ExpenseCard(expense = expense, onDelete = { deleteCandidate = expense })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: ZrzutkaExpense, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(expense.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (expense.settled) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("Rozliczone", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("Zapłacił: ${expense.payerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.splits.isNotEmpty()) {
                    Text(
                        expense.splits.joinToString(", ") { "${it.personName}: ${FormatUtils.formatAmount(it.shareAmount)}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(FormatUtils.formatAmount(expense.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(FormatUtils.formatDate(expense.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
