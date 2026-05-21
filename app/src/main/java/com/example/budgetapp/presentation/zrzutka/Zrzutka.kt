package com.example.budgetapp.presentation.zrzutka

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.ZrzutkaRepository
import com.example.budgetapp.presentation.common.FormatUtils
import com.example.budgetapp.presentation.navigation.Screen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ZrzutkaState(
    val persons: List<ZrzutkaPerson> = emptyList(),
    val settlement: List<ZrzutkaSettlement> = emptyList()
)

class ZrzutkaViewModel(private val repo: ZrzutkaRepository) : ViewModel() {
    private val _state = MutableStateFlow(ZrzutkaState())
    val state: StateFlow<ZrzutkaState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getPersonsWithBalance().collect { persons ->
                _state.update { it.copy(persons = persons, settlement = calculateSettlement(persons)) }
            }
        }
    }

    fun addPerson(name: String) = viewModelScope.launch { repo.insertPerson(name) }
    fun deletePerson(person: ZrzutkaPerson) = viewModelScope.launch { repo.deletePerson(person) }
    fun settleAll() = viewModelScope.launch { repo.settleAll() }

    private fun calculateSettlement(persons: List<ZrzutkaPerson>): List<ZrzutkaSettlement> {
        data class Entry(val id: Long, val name: String, var balance: Double)
        val debtors = persons.filter { it.balance < -0.01 }.map { Entry(it.id, it.name, it.balance) }.toMutableList()
        val creditors = persons.filter { it.balance > 0.01 }.map { Entry(it.id, it.name, it.balance) }.toMutableList()
        val result = mutableListOf<ZrzutkaSettlement>()
        var di = 0; var ci = 0
        while (di < debtors.size && ci < creditors.size) {
            val debtor = debtors[di]; val creditor = creditors[ci]
            val amount = minOf(-debtor.balance, creditor.balance)
            result.add(ZrzutkaSettlement(debtor.id, debtor.name, creditor.id, creditor.name, amount))
            debtor.balance += amount; creditor.balance -= amount
            if (-debtor.balance < 0.01) di++
            if (creditor.balance < 0.01) ci++
        }
        return result
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ZrzutkaViewModel(container.zrzutkaRepository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZrzutkaScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: ZrzutkaViewModel = viewModel(factory = ZrzutkaViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<ZrzutkaPerson?>(null) }

    if (showAddDialog) {
        AddPersonDialog(onDismiss = { showAddDialog = false }, onSave = { name -> viewModel.addPerson(name); showAddDialog = false })
    }

    deleteCandidate?.let { person ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Usuń osobę") },
            text = { Text("Usunąć ${person.name}? Wszystkie jej wydatki zostaną usunięte.") },
            confirmButton = { TextButton(onClick = { viewModel.deletePerson(person); deleteCandidate = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") } }
        )
    }

    if (showSettleDialog) {
        SettleDialog(
            settlement = state.settlement,
            onDismiss = { showSettleDialog = false },
            onConfirm = { viewModel.settleAll(); showSettleDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Dodaj osobę")
                }
                FloatingActionButton(onClick = { navController.navigate(Screen.ZrzutkaDodaj.route) }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj wydatek")
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navController.navigate(Screen.ZrzutkaHistoria.createRoute()) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.History, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Historia")
                    }
                    Button(
                        onClick = { showSettleDialog = true },
                        enabled = state.settlement.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rozlicz")
                    }
                }
            }
            if (state.persons.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("Dodaj osoby, z którymi się rozliczasz", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.persons, key = { it.id }) { person ->
                    PersonCard(person = person, onDelete = { deleteCandidate = person })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonCard(person: ZrzutkaPerson, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                when {
                    person.balance > 0.01 -> Text("Do otrzymania: ${FormatUtils.formatAmount(person.balance)}", color = Color(0xFF388E3C), style = MaterialTheme.typography.bodySmall)
                    person.balance < -0.01 -> Text("Do zapłaty: ${FormatUtils.formatAmount(-person.balance)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> Text("Rozliczono", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettleDialog(settlement: List<ZrzutkaSettlement>, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rozliczenie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settlement.isEmpty()) {
                    Text("Wszyscy są rozliczeni.")
                } else {
                    Text("Aby wyrównać salda:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    settlement.forEach { s ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${s.fromName} → ${s.toName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(FormatUtils.formatAmount(s.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Potwierdź rozliczenie") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa osoba") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Imię") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text("Dodaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
