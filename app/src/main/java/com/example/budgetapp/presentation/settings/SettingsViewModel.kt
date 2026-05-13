package com.example.budgetapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType
import com.example.budgetapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val categories: List<Category> = emptyList(),
    val isDarkMode: Boolean = false
)

class SettingsViewModel(private val categoryRepo: CategoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepo.getAllCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) { _state.update { it.copy(isDarkMode = enabled) } }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            if (category.id == 0L) categoryRepo.insertCategory(category)
            else categoryRepo.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepo.deleteCategory(category) }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(container.categoryRepository) as T
            }
        }
    }
}
