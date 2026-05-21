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
            val net = mutableMapOf<Long, Double>()
            val unsettled = expenses.filter { !it.settled }
            val unsettledIds = unsettled.map { it.id }.toSet()
            for (expense in unsettled) net[expense.payerId] = (net[expense.payerId] ?: 0.0) + expense.totalAmount
            for (split in splits.filter { it.expenseId in unsettledIds }) net[split.personId] = (net[split.personId] ?: 0.0) - split.shareAmount
            persons.map { ZrzutkaPerson(id = it.id, name = it.name, balance = net[it.id] ?: 0.0) }
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

    override suspend fun settleAll() {
        val toSettle = expenseDao.getAllExpensesOnce().filter { !it.settled }.map { it.id }
        if (toSettle.isNotEmpty()) expenseDao.markSettled(toSettle)
    }

    private fun ZrzutkaExpenseEntity.toDomain(personMap: Map<Long, String>, splits: List<ZrzutkaSplitEntity>) = ZrzutkaExpense(
        id = id, description = description, totalAmount = totalAmount, date = date, payerId = payerId,
        payerName = personMap[payerId] ?: "Nieznany",
        splits = splits.map { s -> ZrzutkaSplit(s.personId, personMap[s.personId] ?: "Nieznany", s.shareAmount) },
        settled = settled
    )
}
