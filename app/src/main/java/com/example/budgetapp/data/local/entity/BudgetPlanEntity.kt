package com.example.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.BudgetPlan

@Entity(tableName = "budget_plans")
data class BudgetPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val month: String,
    @ColumnInfo(name = "planned_amount") val plannedAmount: Double
)

fun BudgetPlanEntity.toDomain() = BudgetPlan(
    id = id,
    categoryId = categoryId,
    month = month,
    plannedAmount = plannedAmount
)

fun BudgetPlan.toEntity() = BudgetPlanEntity(
    id = id,
    categoryId = categoryId,
    month = month,
    plannedAmount = plannedAmount
)
