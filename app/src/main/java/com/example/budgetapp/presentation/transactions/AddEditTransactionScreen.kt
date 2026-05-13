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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.presentation.common.FormatUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(navController: NavController, transactionId: Long = -1L) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: AddEditTransactionViewModel = viewModel(
        key = "add_edit_$transactionId",
        factory = AddEditTransactionViewModel.factory(app.container, transactionId)
    )
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    val datePickerDialog = remember {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            val c = Calendar.getInstance()
            c.set(year, month, day, 12, 0, 0)
            viewModel.setDate(c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edytuj transakcję" else "Nowa transakcja") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Typ transakcji", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionType.entries) { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.label) }
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = { viewModel.setAmount(it) },
                label = { Text("Kwota (PLN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.error?.contains("kwot") == true
            )

            if (state.type == TransactionType.EXPENSE || state.type == TransactionType.INCOME) {
                val categories = if (state.type == TransactionType.EXPENSE) state.expenseCategories else state.incomeCategories
                Text("Kategoria", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { viewModel.setCategory(cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            if (state.type == TransactionType.SAVE_TO_JAR || state.type == TransactionType.WITHDRAW_FROM_JAR) {
                Text("Skarbonka", style = MaterialTheme.typography.labelLarge)
                if (state.savingsJars.isEmpty()) {
                    Text("Brak skarbonek. Dodaj skarbonkę w sekcji Oszczędności.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.savingsJars) { jar ->
                            FilterChip(
                                selected = state.selectedJarId == jar.id,
                                onClick = { viewModel.setJar(jar.id) },
                                label = { Text("${jar.name} (${FormatUtils.formatAmount(jar.currentAmount)})") }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.setDescription(it) },
                label = { Text("Opis (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(state.date?.let { FormatUtils.formatDate(it) } ?: "Wybierz datę (opcjonalnie)")
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isEditing) "Zapisz zmiany" else "Dodaj transakcję")
            }
        }
    }
}
