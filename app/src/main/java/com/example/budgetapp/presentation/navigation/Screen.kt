package com.example.budgetapp.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object Budget : Screen("budget")
    object Savings : Screen("savings")

    object AddEditTransaction : Screen("add_edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long = -1L) = "add_edit_transaction/$transactionId"
    }

    object AddEditSavingsJar : Screen("add_edit_savings_jar/{jarId}") {
        fun createRoute(jarId: Long = -1L) = "add_edit_savings_jar/$jarId"
    }
}
