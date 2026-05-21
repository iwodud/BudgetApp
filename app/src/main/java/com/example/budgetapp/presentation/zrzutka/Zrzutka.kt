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

data class ZrzutkaState(val persons: List<ZrzutkaPerson> = emptyList())

class ZrzutkaViewModel(private val repo: ZrzutkaRepository) : ViewModel() {
    private val _state = MutableStateFlow(ZrzutkaState())
    val state: StateFlow<ZrzutkaState> = _state.asStateFlow()

    init { viewModelScope.launch { repo.getPersonsWithBalance().collect { _state.update { s -> s.copy(persons = it) } } } }

    fun addPerson(name: String) = viewModelScope.launch { repo.insertPerson(name) }
    fun deletePerson(person: ZrzutkaPerson) = viewModelScope.launch { repo.deletePerson(person) }
    fun settle(person: ZrzutkaPerson) = viewModelScope.launch { repo.settleWithPerson(person.id) }

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
    var deleteCandidate by remember { mutableStateOf<ZrzutkaPerson?>(null) }
    var settleCandidate by remember { mutableStateOf<ZrzutkaPerson?>(null) }

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

    settleCandidate?.let { person ->
        AlertDialog(
            onDismissRequest = { settleCandidate = null },
            title = { Text("Rozlicz") },
            text = { Text("Oznaczyć wszystkie wspólne wydatki z ${person.name} jako rozliczone?") },
            confirmButton = { TextButton(onClick = { viewModel.settle(person); settleCandidate = null }) { Text("Rozlicz") } },
            dismissButton = { TextButton(onClick = { settleCandidate = null }) { Text("Anuluj") } }
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
        if (state.persons.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Dodaj osoby, z którymi się rozliczasz", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedButton(onClick = { navController.navigate(Screen.ZrzutkaHistoria.createRoute()) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.History, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Historia wydatków")
                    }
                }
                items(state.persons, key = { it.id }) { person ->
                    PersonCard(
                        person = person,
                        onClick = { navController.navigate(Screen.ZrzutkaHistoria.createRoute(person.id)) },
                        onSettle = { settleCandidate = person },
                        onDelete = { deleteCandidate = person }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonCard(person: ZrzutkaPerson, onClick: () -> Unit, onSettle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                when {
                    person.balance > 0.01 -> Text("Winien Ci ${FormatUtils.formatAmount(person.balance)}", color = Color(0xFF388E3C), style = MaterialTheme.typography.bodySmall)
                    person.balance < -0.01 -> Text("Jesteś winien ${FormatUtils.formatAmount(-person.balance)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> Text("Rozliczono", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (kotlin.math.abs(person.balance) > 0.01) {
                TextButton(onClick = onSettle) { Text("Rozlicz") }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
            }
        }
    }
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
