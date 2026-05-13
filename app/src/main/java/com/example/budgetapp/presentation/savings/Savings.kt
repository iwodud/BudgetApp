package com.example.budgetapp.presentation.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.AppContainer
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.SavingsJar
import com.example.budgetapp.domain.repository.SavingsJarRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SavingsState(val jars: List<SavingsJar> = emptyList(), val totalSaved: Double = 0.0, val isLoading: Boolean = true)

class SavingsViewModel(private val savingsJarRepo: SavingsJarRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavingsState())
    val state: StateFlow<SavingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            savingsJarRepo.getAllSavingsJars().collect { jars ->
                _state.update { it.copy(jars = jars, totalSaved = jars.sumOf { j -> j.currentAmount }, isLoading = false) }
            }
        }
    }

    fun saveJar(jar: SavingsJar) { viewModelScope.launch { if (jar.id == 0L) savingsJarRepo.insertSavingsJar(jar) else savingsJarRepo.updateSavingsJar(jar) } }
    fun deleteJar(jar: SavingsJar) { viewModelScope.launch { savingsJarRepo.deleteSavingsJar(jar) } }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SavingsViewModel(container.savingsJarRepository) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: SavingsViewModel = viewModel(factory = SavingsViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingJar by remember { mutableStateOf<SavingsJar?>(null) }
    var deleteCandidate by remember { mutableStateOf<SavingsJar?>(null) }

    if (showDialog) {
        JarDialog(editingJar, onDismiss = { showDialog = false; editingJar = null }, onSave = { jar -> viewModel.saveJar(jar); showDialog = false; editingJar = null })
    }
    deleteCandidate?.let { jar ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Usuń skarbonkę") }, text = { Text("Usunąć skarbonkę \"${jar.name}\"? Saldo zostanie utracone.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteJar(jar); deleteCandidate = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") } })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Oszczędności") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Wróć") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { editingJar = null; showDialog = true }) { Icon(Icons.Default.Add, "Nowa skarbonka") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Łączne oszczędności", style = MaterialTheme.typography.labelMedium)
                        Text(FormatUtils.formatAmount(state.totalSaved), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (state.jars.isEmpty() && !state.isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Brak skarbonek. Dodaj pierwszą!", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(state.jars, key = { it.id }) { jar -> JarCard(jar, onEdit = { editingJar = jar; showDialog = true }, onDelete = { deleteCandidate = jar }) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun JarCard(jar: SavingsJar, onEdit: () -> Unit, onDelete: () -> Unit) {
    val progress = if (jar.goalAmount != null && jar.goalAmount > 0) (jar.currentAmount / jar.goalAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(jar.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edytuj", modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp)) }
                }
            }
            Text(FormatUtils.formatAmount(jar.currentAmount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (jar.goalAmount != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Cel: ${FormatUtils.formatAmount(jar.goalAmount)} · ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun JarDialog(jar: SavingsJar?, onDismiss: () -> Unit, onSave: (SavingsJar) -> Unit) {
    var name by remember(jar) { mutableStateOf(jar?.name ?: "") }
    var goalText by remember(jar) { mutableStateOf(jar?.goalAmount?.let { "%.2f".format(it) } ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (jar == null) "Nowa skarbonka" else "Edytuj skarbonkę") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, singleLine = true)
                OutlinedTextField(value = goalText, onValueChange = { goalText = it }, label = { Text("Cel kwotowy (opcjonalnie)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(SavingsJar(id = jar?.id ?: 0L, name = name, currentAmount = jar?.currentAmount ?: 0.0, goalAmount = goalText.replace(",", ".").toDoubleOrNull(), createdAt = jar?.createdAt ?: System.currentTimeMillis())) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}
