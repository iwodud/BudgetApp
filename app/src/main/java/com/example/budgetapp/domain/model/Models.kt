package com.example.budgetapp.domain.model

enum class TransactionType(val label: String) {
    EXPENSE("Wydatek"),
    INCOME("Przychód"),
    SAVE_TO_JAR("Zaoszczędzenie"),
    WITHDRAW_FROM_JAR("Pobranie z oszczędności")
}

enum class CategoryType(val label: String) {
    EXPENSE("Wydatek"),
    INCOME("Przychód")
}

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

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val isDefault: Boolean = false,
    val colorHex: String = "#6750A4"
)

data class BudgetPlan(
    val id: Long = 0,
    val categoryId: Long,
    val month: String,
    val plannedAmount: Double
)

data class SavingsJar(
    val id: Long = 0,
    val name: String,
    val currentAmount: Double = 0.0,
    val goalAmount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

const val ZRZUTKA_SELF_ID = -1L
const val ZRZUTKA_SELF_NAME = "Ja"

data class ZrzutkaPerson(val id: Long, val name: String, val balance: Double = 0.0)

data class ZrzutkaExpense(
    val id: Long,
    val description: String,
    val totalAmount: Double,
    val date: Long,
    val payerId: Long,
    val payerName: String,
    val splits: List<ZrzutkaSplit>,
    val settled: Boolean
)

data class ZrzutkaSplit(val personId: Long, val personName: String, val shareAmount: Double)

data class SavingsJarBudgetPlan(
    val id: Long = 0,
    val savingsJarId: Long,
    val month: String,
    val plannedSaveAmount: Double = 0.0,
    val plannedWithdrawAmount: Double = 0.0
)
