package com.example.budgetapp.domain.model

enum class TransactionType(val label: String) {
    EXPENSE("Wydatek"),
    INCOME("Przychód"),
    SAVE_TO_JAR("Zaoszczędzenie"),
    WITHDRAW_FROM_JAR("Pobranie z oszczędności")
}
