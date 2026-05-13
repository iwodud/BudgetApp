package com.example.budgetapp.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.budgetapp.BudgetApp
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val app = LocalContext.current.applicationContext as BudgetApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app.container))
    val state by viewModel.state.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deleteCandidate by remember { mutableStateOf<Category?>(null) }

    if (showCategoryDialog) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = { showCategoryDialog = false; editingCategory = null },
            onSave = { cat -> viewModel.saveCategory(cat); showCategoryDialog = false; editingCategory = null }
        )
    }

    deleteCandidate?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Usuń kategorię") },
            text = { Text("Usunąć kategorię \"${cat.name}\"? Transakcje zachowają jej nazwę.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(cat); deleteCandidate = null }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Anuluj") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ustawienia") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingCategory = null; showCategoryDialog = true }) {
                Icon(Icons.Default.Add, "Dodaj kategorię")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ciemny motyw", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                    }
                }
            }

            item {
                Text("Kategorie wydatków", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(state.categories.filter { it.type == CategoryType.EXPENSE }) { cat ->
                CategoryRow(cat, onEdit = { editingCategory = cat; showCategoryDialog = true }, onDelete = { deleteCandidate = cat })
            }

            item {
                Text("Kategorie przychodów", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.categories.filter { it.type == CategoryType.INCOME }) { cat ->
                CategoryRow(cat, onEdit = { editingCategory = cat; showCategoryDialog = true }, onDelete = { deleteCandidate = cat })
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("BudgetApp v1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium)
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edytuj", modifier = Modifier.size(18.dp))
                }
                if (!category.isDefault) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Usuń", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(category: Category?, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var selectedType by remember(category) { mutableStateOf(category?.type ?: CategoryType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Nowa kategoria" else "Edytuj kategorię") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, singleLine = true)
                if (category == null) {
                    Text("Typ", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(Category(id = category?.id ?: 0L, name = name, type = selectedType, isDefault = category?.isDefault ?: false, colorHex = category?.colorHex ?: "#9E9E9E"))
                }
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
