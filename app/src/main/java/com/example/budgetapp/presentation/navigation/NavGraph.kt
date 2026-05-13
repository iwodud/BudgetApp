package com.example.budgetapp.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.budgetapp.presentation.budget.BudgetScreen
import com.example.budgetapp.presentation.dashboard.DashboardScreen
import com.example.budgetapp.presentation.savings.SavingsScreen
import com.example.budgetapp.presentation.settings.SettingsScreen
import com.example.budgetapp.presentation.statistics.StatisticsScreen
import com.example.budgetapp.presentation.transactions.AddEditTransactionScreen
import com.example.budgetapp.presentation.transactions.TransactionListScreen

data class BottomNavItem(val label: String, val icon: ImageVector, val screen: Screen)

val bottomNavItems = listOf(
    BottomNavItem("Start", Icons.Default.Home, Screen.Dashboard),
    BottomNavItem("Transakcje", Icons.Default.List, Screen.Transactions),
    BottomNavItem("Statystyki", Icons.Default.PieChart, Screen.Statistics),
    BottomNavItem("Ustawienia", Icons.Default.Settings, Screen.Settings)
)

@Composable
fun AppNavGraph(isDarkMode: Boolean, onToggleDarkMode: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Transactions.route) { TransactionListScreen(navController) }
            composable(Screen.Statistics.route) { StatisticsScreen(navController) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
            composable(Screen.Budget.route) { BudgetScreen(navController) }
            composable(Screen.Savings.route) { SavingsScreen(navController) }
            composable(
                route = Screen.AddEditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStack ->
                val transactionId = backStack.arguments?.getLong("transactionId") ?: -1L
                AddEditTransactionScreen(navController, transactionId)
            }
        }
    }
}
