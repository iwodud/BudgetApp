package com.example.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.*

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
    id = id, type = TransactionType.valueOf(type), amount = amount,
    categoryId = categoryId, description = description, date = date,
    savingsJarId = savingsJarId, createdAt = createdAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id, type = type.name, amount = amount,
    categoryId = categoryId, description = description, date = date,
    savingsJarId = savingsJarId, createdAt = createdAt
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#6750A4"
)

fun CategoryEntity.toDomain() = Category(
    id = id, name = name, type = CategoryType.valueOf(type),
    isDefault = isDefault, colorHex = colorHex
)

fun Category.toEntity() = CategoryEntity(
    id = id, name = name, type = type.name,
    isDefault = isDefault, colorHex = colorHex
)

@Entity(tableName = "budget_plans")
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val month: String,
    @ColumnInfo(name = "planned_amount") val plannedAmount: Double
)

fun BudgetPlanEntity.toDomain() = BudgetPlan(
    id = id, categoryId = categoryId, month = month, plannedAmount = plannedAmount
)

fun BudgetPlan.toEntity() = BudgetPlanEntity(
    id = id, categoryId = categoryId, month = month, plannedAmount = plannedAmount
)

@Entity(tableName = "savings_jars")
data class SavingsJarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "current_amount") val currentAmount: Double = 0.0,
    @ColumnInfo(name = "goal_amount") val goalAmount: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

fun SavingsJarEntity.toDomain() = SavingsJar(
    id = id, name = name, currentAmount = currentAmount,
    goalAmount = goalAmount, createdAt = createdAt
)

fun SavingsJar.toEntity() = SavingsJarEntity(
    id = id, name = name, currentAmount = currentAmount,
    goalAmount = goalAmount, createdAt = createdAt
)
