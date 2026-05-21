package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.*
import com.example.budgetapp.data.local.entity.*
import com.example.budgetapp.domain.model.*
import com.example.budgetapp.domain.repository.ZrzutkaRepository
import kotlinx.coroutines.flow.*

class ZrzutkaRepositoryImpl(
    private val personDao: ZrzutkaPersonDao,
    private val expenseDao: ZrzutkaExpenseDao,
    private val splitDao: ZrzutkaSplitDao
) : ZrzutkaRepository {

    override fun getPersonsWithBalance(): Flow<List<ZrzutkaPerson>> =
        combine(personDao.getAllPersons(), expenseDao.getAllExpenses(), splitDao.getAllSplits()) { persons, expenses, splits ->
            val splitsByExpense = splits.groupBy { it.expenseId }
            persons.map { person ->
                val unsettled = expenses.filter { !it.settled }
                val owedToMe = unsettled
                    .filter { it.payerId == ZRZUTKA_SELF_ID }
                    .sumOf { exp -> splitsByExpense[exp.id]?.filter { it.personId == person.id }?.sumOf { it.shareAmount } ?: 0.0 }
                val iOwe = unsettled
                    .filter { it.payerId == person.id }
                    .sumOf { exp -> splitsByExpense[exp.id]?.filter { it.personId == ZRZUTKA_SELF_ID }?.sumOf { it.shareAmount } ?: 0.0 }
                ZrzutkaPerson(id = person.id, name = person.name, balance = owedToMe - iOwe)
            }
        }

    override fun getAllPersonsPlain(): Flow<List<ZrzutkaPerson>> =
        personDao.getAllPersons().map { list -> list.map { ZrzutkaPerson(it.id, it.name) } }

    override fun getAllExpenses(): Flow<List<ZrzutkaExpense>> =
        combine(expenseDao.getAllExpenses(), splitDao.getAllSplits(), personDao.getAllPersons()) { expenses, splits, persons ->
            val personMap = persons.associate { it.id to it.name }
            val splitsByExpense = splits.groupBy { it.expenseId }
            expenses.map { exp -> exp.toDomain(personMap, splitsByExpense[exp.id] ?: emptyList()) }
        }

    override suspend fun insertPerson(name: String) {
        personDao.insert(ZrzutkaPersonEntity(name = name))
    }

    override suspend fun deletePerson(person: ZrzutkaPerson) {
        expenseDao.getAllExpensesOnce().filter { it.payerId == person.id }.forEach { expenseDao.delete(it) }
        splitDao.deleteAllSplitsForPerson(person.id)
        personDao.delete(ZrzutkaPersonEntity(id = person.id, name = person.name))
    }

    override suspend fun insertExpense(description: String, totalAmount: Double, date: Long, payerId: Long, splits: List<Pair<Long, Double>>) {
        val id = expenseDao.insert(ZrzutkaExpenseEntity(description = description, totalAmount = totalAmount, date = date, payerId = payerId))
        splitDao.insertAll(splits.map { (personId, amount) -> ZrzutkaSplitEntity(expenseId = id, personId = personId, shareAmount = amount) })
    }

    override suspend fun deleteExpense(expense: ZrzutkaExpense) {
        expenseDao.delete(ZrzutkaExpenseEntity(id = expense.id, description = expense.description, totalAmount = expense.totalAmount, date = expense.date, payerId = expense.payerId, settled = expense.settled))
    }

    override suspend fun settleWithPerson(personId: Long) {
        val expenses = expenseDao.getAllExpensesOnce()
        val splits = splitDao.getAllSplitsOnce()
        val splitsByExpense = splits.groupBy { it.expenseId }
        val toSettle = expenses.filter { exp ->
            !exp.settled && (
                (exp.payerId == ZRZUTKA_SELF_ID && splitsByExpense[exp.id]?.any { it.personId == personId } == true) ||
                (exp.payerId == personId && splitsByExpense[exp.id]?.any { it.personId == ZRZUTKA_SELF_ID } == true)
            )
        }.map { it.id }
        if (toSettle.isNotEmpty()) expenseDao.markSettled(toSettle)
    }

    private fun ZrzutkaExpenseEntity.toDomain(personMap: Map<Long, String>, splits: List<ZrzutkaSplitEntity>) = ZrzutkaExpense(
        id = id, description = description, totalAmount = totalAmount, date = date,
        payerId = payerId,
        payerName = if (payerId == ZRZUTKA_SELF_ID) ZRZUTKA_SELF_NAME else personMap[payerId] ?: "Nieznany",
        splits = splits.map { s -> ZrzutkaSplit(personId = s.personId, personName = if (s.personId == ZRZUTKA_SELF_ID) ZRZUTKA_SELF_NAME else personMap[s.personId] ?: "Nieznany", shareAmount = s.shareAmount) },
        settled = settled
    )
}
