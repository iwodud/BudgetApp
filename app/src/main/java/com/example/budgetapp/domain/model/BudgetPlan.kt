package com.example.budgetapp.domain.model

data class BudgetPlan(
    val id: Long = 0,
    val categoryId: Long,
    val month: String,
    val plannedAmount: Double
)
