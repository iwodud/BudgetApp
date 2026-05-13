package com.example.budgetapp.domain.model

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long? = null,
    val description: String? = null,
    val date: Long? = null,
    val savingsJarId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
