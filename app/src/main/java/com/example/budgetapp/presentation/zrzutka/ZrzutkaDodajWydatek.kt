package com.example.budgetapp.presentation.zrzutka

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.ZrzutkaRepository
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DodajWydatekState(val persons: List<ZrzutkaPerson> = emptyList(), val saved: Boolean = false)

class ZrzutkaDodajWydatekViewModel(private val repo: ZrzutkaRepository) : ViewModel() {
    private val _state = MutableStateFlow(DodajWydatekState())
    val state: StateFlow<DodajWydatekState> = _state.asStateFlow()

    init { viewModelScope.launch { repo.getAllPersonsPlain().collect { _state.update { s -> s.copy(persons = it) } } } }

    fun save(description: String, totalAmount: Double, payerId: Long, splits: List<Pair<Long, Double>>) {
        viewModelScope.launch {
            repo.insertExpense(description, totalAmount, System.currentTimeMillis(), payerId, splits)
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ZrzutkaDodajWydatekViewModel(container.zrzutkaRepository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ZrzutkaDodajWydatekScreen(navController: NavController) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: ZrzutkaDodajWydatekViewModel = viewModel(factory = ZrzutkaDodajWydatekViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var payerId by remember { mutableStateOf(ZRZUTKA_SELF_ID) }
    var participants by remember { mutableStateOf(setOf(ZRZUTKA_SELF_ID)) }
    var equalSplit by remember { mutableStateOf(true) }
    var manualAmounts by remember { mutableStateOf(mapOf<Long, String>()) }

    LaunchedEffect(state.persons) {
        val allIds = setOf(ZRZUTKA_SELF_ID) + state.persons.map { it.id }.toSet()
        participants = allIds
        manualAmounts = allIds.associateWith { "" }
    }

    LaunchedEffect(state.saved) { if (state.saved) navController.navigateUp() }

    val totalAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val participantList = participants.toList()
    val equalShare = if (participantList.isNotEmpty()) totalAmount / participantList.size else 0.0

    fun personName(id: Long) = if (id == ZRZUTKA_SELF_ID) ZRZUTKA_SELF_NAME else state.persons.find { it.id == id }?.name ?: "?"

    val manualSum = manualAmounts.filter { participants.contains(it.key) }.values.sumOf { it.replace(",", ".").toDoubleOrNull() ?: 0.0 }
    val manualValid = !equalSplit && kotlin.math.abs(manualSum - totalAmount) <= 0.01
    val canSave = description.isNotBlank() && totalAmount > 0 && participants.isNotEmpty() && (equalSplit || manualValid)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Nowy wydatek") },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, "Wstecz") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            item {
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("Kwota (zł)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )
            }

            item {
                Text("Kto zapłacił", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allPayers = listOf(ZRZUTKA_SELF_ID) + state.persons.map { it.id }
                    allPayers.forEach { id ->
                        FilterChip(selected = payerId == id, onClick = { payerId = id }, label = { Text(personName(id)) })
                    }
                }
            }

            item {
                Text("Uczestnicy", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                val allIds = listOf(ZRZUTKA_SELF_ID) + state.persons.map { it.id }
                allIds.forEach { id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = participants.contains(id),
                            onCheckedChange = { checked -> participants = if (checked) participants + id else participants - id }
                        )
                        Text(personName(id), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Równy podział", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = equalSplit, onCheckedChange = { equalSplit = it })
                }
            }

            if (participants.isNotEmpty() && totalAmount > 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Podział", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        participants.toList().sortedBy { it }.forEach { id ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(personName(id), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                if (equalSplit) {
                                    Text(FormatUtils.formatAmount(equalShare))
                                } else {
                                    OutlinedTextField(
                                        value = manualAmounts[id] ?: "",
                                        onValueChange = { manualAmounts = manualAmounts + (id to it) },
                                        modifier = Modifier.width(130.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        suffix = { Text("zł") }
                                    )
                                }
                            }
                        }
                        if (!equalSplit && participants.isNotEmpty()) {
                            val diff = kotlin.math.abs(manualSum - totalAmount)
                            if (diff > 0.01) {
                                Text(
                                    "Suma udziałów (${FormatUtils.formatAmount(manualSum)}) ≠ kwota (${FormatUtils.formatAmount(totalAmount)})",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val splits = if (equalSplit) {
                            participants.map { it to equalShare }
                        } else {
                            participants.map { id -> id to (manualAmounts[id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0) }
                        }
                        viewModel.save(description.trim(), totalAmount, payerId, splits)
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Zapisz") }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
