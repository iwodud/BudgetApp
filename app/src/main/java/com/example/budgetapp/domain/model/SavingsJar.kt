package com.example.budgetapp.domain.model

data class SavingsJar(
    val id: Long = 0,
    val name: String,
    val currentAmount: Double = 0.0,
    val goalAmount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
