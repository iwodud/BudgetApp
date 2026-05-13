package com.example.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val description: String? = null,
    val date: Long? = null,
    @ColumnInfo(name = "savings_jar_id") val savingsJarId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    categoryId = categoryId,
    description = description,
    date = date,
    savingsJarId = savingsJarId,
    createdAt = createdAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    categoryId = categoryId,
    description = description,
    date = date,
    savingsJarId = savingsJarId,
    createdAt = createdAt
)
