package com.example.budgetapp.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.AppContainer
import com.example.budgetapp.domain.model.SavingsJar
import com.example.budgetapp.domain.repository.SavingsJarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SavingsState(
    val jars: List<SavingsJar> = emptyList(),
    val totalSaved: Double = 0.0,
    val isLoading: Boolean = true
)

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

    fun saveJar(jar: SavingsJar) {
        viewModelScope.launch {
            if (jar.id == 0L) savingsJarRepo.insertSavingsJar(jar)
            else savingsJarRepo.updateSavingsJar(jar)
        }
    }

    fun deleteJar(jar: SavingsJar) {
        viewModelScope.launch { savingsJarRepo.deleteSavingsJar(jar) }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SavingsViewModel(container.savingsJarRepository) as T
            }
        }
    }
}
