package com.example.budgetapp.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val colorHex: String = "#6750A4"
)
