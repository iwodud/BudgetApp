package com.example.budgetapp.domain.repository

import com.example.budgetapp.domain.model.SavingsJar
import kotlinx.coroutines.flow.Flow

interface SavingsJarRepository {
    fun getAllSavingsJars(): Flow<List<SavingsJar>>
    suspend fun getSavingsJarById(id: Long): SavingsJar?
    suspend fun insertSavingsJar(jar: SavingsJar): Long
    suspend fun updateSavingsJar(jar: SavingsJar)
    suspend fun deleteSavingsJar(jar: SavingsJar)
    suspend fun addToAmount(id: Long, delta: Double)
}
