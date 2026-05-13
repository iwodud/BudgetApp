package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.SavingsJarDao
import com.example.budgetapp.data.local.entity.toDomain
import com.example.budgetapp.data.local.entity.toEntity
import com.example.budgetapp.domain.model.SavingsJar
import com.example.budgetapp.domain.repository.SavingsJarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsJarRepositoryImpl(private val dao: SavingsJarDao) : SavingsJarRepository {

    override fun getAllSavingsJars(): Flow<List<SavingsJar>> =
        dao.getAllSavingsJars().map { it.map { e -> e.toDomain() } }

    override suspend fun getSavingsJarById(id: Long): SavingsJar? =
        dao.getSavingsJarById(id)?.toDomain()

    override suspend fun insertSavingsJar(jar: SavingsJar): Long =
        dao.insert(jar.toEntity())

    override suspend fun updateSavingsJar(jar: SavingsJar) =
        dao.update(jar.toEntity())

    override suspend fun deleteSavingsJar(jar: SavingsJar) =
        dao.delete(jar.toEntity())

    override suspend fun addToAmount(id: Long, delta: Double) =
        dao.addToAmount(id, delta)
}
