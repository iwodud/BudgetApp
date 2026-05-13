package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.TransactionDao
import com.example.budgetapp.data.local.entity.toDomain
import com.example.budgetapp.data.local.entity.toEntity
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(private val dao: TransactionDao) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { it.map { e -> e.toDomain() } }

    override fun getTransactionsByMonth(month: String): Flow<List<Transaction>> =
        dao.getTransactionsByMonth(month).map { it.map { e -> e.toDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        dao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        dao.update(transaction.toEntity())

    override suspend fun deleteTransaction(transaction: Transaction) =
        dao.delete(transaction.toEntity())

    override suspend fun getExpensesByYear(year: String): List<Transaction> =
        dao.getExpensesByYear(year).map { it.toDomain() }
}
