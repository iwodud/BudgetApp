package com.example.budgetapp.data.local.entity

import androidx.room.*
import com.example.budgetapp.domain.model.FwnPlannedExpense
import com.example.budgetapp.domain.model.FwnTransaction
import com.example.budgetapp.domain.model.FwnTransactionType

@Entity(tableName = "fwn_planned_expenses")
data class FwnPlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val month: Int
)

fun FwnPlannedExpenseEntity.toDomain() = FwnPlannedExpense(id = id, name = name, amount = amount, month = month)

@Entity(tableName = "fwn_transactions")
data class FwnTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "linked_expense_id") val linkedExpenseId: Long? = null
)

fun FwnTransactionEntity.toDomain() = FwnTransaction(
    id = id,
    type = FwnTransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    linkedExpenseId = linkedExpenseId
)
