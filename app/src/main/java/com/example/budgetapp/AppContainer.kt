package com.example.budgetapp

import android.content.Context
import com.example.budgetapp.data.local.AppDatabase
import com.example.budgetapp.data.repository.*
import com.example.budgetapp.domain.repository.*

interface AppContainer {
    val database: AppDatabase
    val transactionRepository: TransactionRepository
    val categoryRepository: CategoryRepository
    val budgetPlanRepository: BudgetPlanRepository
    val savingsJarRepository: SavingsJarRepository
    val savingsJarBudgetPlanRepository: SavingsJarBudgetPlanRepository
}

class AppContainerImpl(context: Context) : AppContainer {
    override val database: AppDatabase = AppDatabase.getDatabase(context)
    override val transactionRepository: TransactionRepository = TransactionRepositoryImpl(database.transactionDao())
    override val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
    override val budgetPlanRepository: BudgetPlanRepository = BudgetPlanRepositoryImpl(database.budgetPlanDao())
    override val savingsJarRepository: SavingsJarRepository = SavingsJarRepositoryImpl(database.savingsJarDao())
    override val savingsJarBudgetPlanRepository: SavingsJarBudgetPlanRepository = SavingsJarBudgetPlanRepositoryImpl(database.savingsJarBudgetPlanDao())
}
