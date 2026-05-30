package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.FwnPlannedExpenseDao
import com.example.budgetapp.data.local.dao.FwnTransactionDao
import com.example.budgetapp.data.local.entity.FwnPlannedExpenseEntity
import com.example.budgetapp.data.local.entity.FwnTransactionEntity
import com.example.budgetapp.data.local.entity.toDomain
import com.example.budgetapp.domain.model.FwnPlannedExpense
import com.example.budgetapp.domain.model.FwnTransaction
import com.example.budgetapp.domain.model.FwnTransactionType
import com.example.budgetapp.domain.repository.FwnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FwnRepositoryImpl(
    private val plannedExpenseDao: FwnPlannedExpenseDao,
    private val transactionDao: FwnTransactionDao
) : FwnRepository {

    override fun getPlannedExpenses(): Flow<List<FwnPlannedExpense>> =
        plannedExpenseDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTransactions(): Flow<List<FwnTransaction>> =
        transactionDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insertPlannedExpense(name: String, amount: Double, month: Int) {
        plannedExpenseDao.insert(FwnPlannedExpenseEntity(name = name, amount = amount, month = month))
    }

    override suspend fun updatePlannedExpense(expense: FwnPlannedExpense) {
        plannedExpenseDao.update(FwnPlannedExpenseEntity(id = expense.id, name = expense.name, amount = expense.amount, month = expense.month))
    }

    override suspend fun deletePlannedExpense(expense: FwnPlannedExpense) {
        plannedExpenseDao.delete(FwnPlannedExpenseEntity(id = expense.id, name = expense.name, amount = expense.amount, month = expense.month))
    }

    override suspend fun addTransaction(type: FwnTransactionType, amount: Double, description: String, linkedExpenseId: Long?) {
        transactionDao.insert(FwnTransactionEntity(type = type.name, amount = amount, description = description, linkedExpenseId = linkedExpenseId))
    }

    override suspend fun deleteTransaction(transaction: FwnTransaction) {
        transactionDao.delete(FwnTransactionEntity(
            id = transaction.id, type = transaction.type.name, amount = transaction.amount,
            description = transaction.description, date = transaction.date, linkedExpenseId = transaction.linkedExpenseId
        ))
    }
}
