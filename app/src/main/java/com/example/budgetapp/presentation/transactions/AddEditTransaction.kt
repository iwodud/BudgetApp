package com.example.budgetapp.presentation.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.budgetapp.domain.repository.*
import com.example.budgetapp.presentation.common.FormatUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddEditState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val selectedCategoryId: Long? = null,
    val selectedJarId: Long? = null,
    val description: String = "",
    val date: Long? = null,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val savingsJars: List<SavingsJar> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

class AddEditTransactionViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val savingsJarRepo: SavingsJarRepository,
    private val transactionId: Long = -1L
) : ViewModel() {
    private val _state = MutableStateFlow(AddEditState())
    val state: StateFlow<AddEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val expenseCats = categoryRepo.getAllCategoriesOnce().filter { it.type == CategoryType.EXPENSE }
            val incomeCats = categoryRepo.getAllCategoriesOnce().filter { it.type == CategoryType.INCOME }
            val jars = savingsJarRepo.getAllSavingsJars().first()
            _state.update { it.copy(expenseCategories = expenseCats, incomeCategories = incomeCats, savingsJars = jars) }
            if (transactionId > 0) {
                val t = transactionRepo.getTransactionById(transactionId)
                if (t != null) _state.update { it.copy(type = t.type, amountText = t.amount.toString(), selectedCategoryId = t.categoryId, selectedJarId = t.savingsJarId, description = t.description ?: "", date = t.date, isEditing = true) }
            }
        }
    }

    fun setType(type: TransactionType) { _state.update { it.copy(type = type, selectedCategoryId = null, selectedJarId = null) } }
    fun setAmount(text: String) { _state.update { it.copy(amountText = text, error = null) } }
    fun setCategory(id: Long) { _state.update { it.copy(selectedCategoryId = id) } }
    fun setJar(id: Long) { _state.update { it.copy(selectedJarId = id) } }
    fun setDescription(text: String) { _state.update { it.copy(description = text) } }
    fun setDate(timestamp: Long?) { _state.update { it.copy(date = timestamp) } }

    fun save() {
        val s = _state.value
        val amount = s.amountText.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0) { _state.update { it.copy(error = "Podaj prawidłową kwotę") }; return }
        if ((s.type == TransactionType.EXPENSE || s.type == TransactionType.INCOME) && s.selectedCategoryId == null) { _state.update { it.copy(error = "Wybierz kategorię") }; return }
        if ((s.type == TransactionType.SAVE_TO_JAR || s.type == TransactionType.WITHDRAW_FROM_JAR) && s.selectedJarId == null) { _state.update { it.copy(error = "Wybierz skarbonkę") }; return }
        viewModelScope.launch {
            val transaction = Transaction(id = if (s.isEditing) transactionId else 0L, type = s.type, amount = amount, categoryId = s.selectedCategoryId, savingsJarId = s.selectedJarId, description = s.description.takeIf { it.isNotBlank() }, date = s.date)
            if (s.isEditing) transactionRepo.updateTransaction(transaction) else {
                transactionRepo.insertTransaction(transaction)
                if (s.type == TransactionType.SAVE_TO_JAR && s.selectedJarId != null) savingsJarRepo.addToAmount(s.selectedJarId, amount)
                else if (s.type == TransactionType.WITHDRAW_FROM_JAR && s.selectedJarId != null) savingsJarRepo.addToAmount(s.selectedJarId, -amount)
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        fun factory(container: AppContainer, transactionId: Long = -1L) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AddEditTransactionViewModel(container.transactionRepository, container.categoryRepository, container.savingsJarRepository, transactionId) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(navController: NavController, transactionId: Long = -1L) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: AddEditTransactionViewModel = viewModel(key = "add_edit_$transactionId", factory = AddEditTransactionViewModel.factory(app.container, transactionId))
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) { if (state.isSaved) navController.popBackStack() }

    val datePickerDialog = remember {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            val c = Calendar.getInstance(); c.set(year, month, day, 12, 0, 0); viewModel.setDate(c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edytuj transakcję" else "Nowa transakcja") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Wróć") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Typ transakcji", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionType.entries) { type ->
                    FilterChip(selected = state.type == type, onClick = { viewModel.setType(type) }, label = { Text(type.label) })
                }
            }
            OutlinedTextField(value = state.amountText, onValueChange = { viewModel.setAmount(it) }, label = { Text("Kwota (PLN)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = state.error?.contains("kwot") == true)
            if (state.type == TransactionType.EXPENSE || state.type == TransactionType.INCOME) {
                val categories = if (state.type == TransactionType.EXPENSE) state.expenseCategories else state.incomeCategories
                Text("Kategoria", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat -> FilterChip(selected = state.selectedCategoryId == cat.id, onClick = { viewModel.setCategory(cat.id) }, label = { Text(cat.name) }) }
                }
            }
            if (state.type == TransactionType.SAVE_TO_JAR || state.type == TransactionType.WITHDRAW_FROM_JAR) {
                Text("Skarbonka", style = MaterialTheme.typography.labelLarge)
                if (state.savingsJars.isEmpty()) {
                    Text("Brak skarbonek. Dodaj skarbonkę w sekcji Oszczędności.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.savingsJars) { jar -> FilterChip(selected = state.selectedJarId == jar.id, onClick = { viewModel.setJar(jar.id) }, label = { Text("${jar.name} (${FormatUtils.formatAmount(jar.currentAmount)})") }) }
                    }
                }
            }
            OutlinedTextField(value = state.description, onValueChange = { viewModel.setDescription(it) }, label = { Text("Opis (opcjonalnie)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(8.dp))
                Text(state.date?.let { FormatUtils.formatDate(it) } ?: "Wybierz datę (opcjonalnie)")
            }
            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isEditing) "Zapisz zmiany" else "Dodaj transakcję")
            }
        }
    }
}
