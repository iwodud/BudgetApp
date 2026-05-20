package com.example.budgetapp.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetapp.data.local.dao.*
import com.example.budgetapp.data.local.entity.*
import java.util.Calendar

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS savings_jar_budget_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                savings_jar_id INTEGER NOT NULL,
                month TEXT NOT NULL,
                planned_save_amount REAL NOT NULL DEFAULT 0.0,
                planned_withdraw_amount REAL NOT NULL DEFAULT 0.0
            )"""
        )
    }
}

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetPlanEntity::class, SavingsJarEntity::class, SavingsJarBudgetPlanEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetPlanDao(): BudgetPlanDao
    abstract fun savingsJarDao(): SavingsJarDao
    abstract fun savingsJarBudgetPlanDao(): SavingsJarBudgetPlanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "budget_database")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}

object DatabaseSeeder {
    suspend fun seed(db: AppDatabase) {
        val categoryDao = db.categoryDao()
        val transactionDao = db.transactionDao()
        val budgetPlanDao = db.budgetPlanDao()
        val savingsJarDao = db.savingsJarDao()

        val expenseCategories = listOf(
            CategoryEntity(name = "Jedzenie", type = "EXPENSE", isDefault = true, colorHex = "#4CAF50"),
            CategoryEntity(name = "Transport", type = "EXPENSE", isDefault = true, colorHex = "#2196F3"),
            CategoryEntity(name = "Zakupy", type = "EXPENSE", isDefault = true, colorHex = "#FF9800"),
            CategoryEntity(name = "Rozrywka", type = "EXPENSE", isDefault = true, colorHex = "#9C27B0"),
            CategoryEntity(name = "Rachunki", type = "EXPENSE", isDefault = true, colorHex = "#F44336"),
            CategoryEntity(name = "Zdrowie", type = "EXPENSE", isDefault = true, colorHex = "#009688"),
            CategoryEntity(name = "Inne", type = "EXPENSE", isDefault = true, colorHex = "#9E9E9E")
        )
        val incomeCategories = listOf(
            CategoryEntity(name = "Wynagrodzenie", type = "INCOME", isDefault = true, colorHex = "#1B5E20"),
            CategoryEntity(name = "Freelance", type = "INCOME", isDefault = true, colorHex = "#FFC107"),
            CategoryEntity(name = "Inne przychody", type = "INCOME", isDefault = true, colorHex = "#8BC34A")
        )

        val expenseIds = categoryDao.insertAll(expenseCategories)
        val incomeIds = categoryDao.insertAll(incomeCategories)
        val catMap = buildMap {
            expenseCategories.zip(expenseIds).forEach { (cat, id) -> put(cat.name, id) }
            incomeCategories.zip(incomeIds).forEach { (cat, id) -> put(cat.name, id) }
        }

        val cal = Calendar.getInstance()
        fun daysAgo(days: Int): Long {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -days)
            return cal.timeInMillis
        }

        val jar1Id = savingsJarDao.insert(SavingsJarEntity(name = "Wakacje", currentAmount = 0.0, goalAmount = 3000.0))
        val jar2Id = savingsJarDao.insert(SavingsJarEntity(name = "Fundusz awaryjny", currentAmount = 0.0, goalAmount = 5000.0))

        transactionDao.insertAll(listOf(
            TransactionEntity(type = "INCOME", amount = 4500.0, categoryId = catMap["Wynagrodzenie"], description = "Pensja - poprzedni miesiąc", date = daysAgo(35), createdAt = daysAgo(35)),
            TransactionEntity(type = "EXPENSE", amount = 280.0, categoryId = catMap["Jedzenie"], description = "Zakupy spożywcze", date = daysAgo(33), createdAt = daysAgo(33)),
            TransactionEntity(type = "EXPENSE", amount = 150.0, categoryId = catMap["Transport"], description = "Bilet miesięczny", date = daysAgo(31), createdAt = daysAgo(31)),
            TransactionEntity(type = "EXPENSE", amount = 420.0, categoryId = catMap["Rachunki"], description = "Prąd + internet", date = daysAgo(28), createdAt = daysAgo(28)),
            TransactionEntity(type = "EXPENSE", amount = 85.0, categoryId = catMap["Rozrywka"], description = "Kino", date = daysAgo(25), createdAt = daysAgo(25)),
            TransactionEntity(type = "EXPENSE", amount = 210.0, categoryId = catMap["Zakupy"], description = "Ubrania", date = daysAgo(22), createdAt = daysAgo(22)),
            TransactionEntity(type = "SAVE_TO_JAR", amount = 500.0, savingsJarId = jar1Id, description = "Oszczędności na wakacje", date = daysAgo(20), createdAt = daysAgo(20)),
            TransactionEntity(type = "INCOME", amount = 800.0, categoryId = catMap["Freelance"], description = "Projekt strony WWW", date = daysAgo(18), createdAt = daysAgo(18)),
            TransactionEntity(type = "EXPENSE", amount = 65.0, categoryId = catMap["Zdrowie"], description = "Apteka", date = daysAgo(15), createdAt = daysAgo(15)),
            TransactionEntity(type = "SAVE_TO_JAR", amount = 700.0, savingsJarId = jar2Id, description = "Fundusz awaryjny", date = daysAgo(12), createdAt = daysAgo(12)),
            TransactionEntity(type = "INCOME", amount = 4500.0, categoryId = catMap["Wynagrodzenie"], description = "Pensja", date = daysAgo(5), createdAt = daysAgo(5)),
            TransactionEntity(type = "EXPENSE", amount = 1200.0, categoryId = catMap["Rachunki"], description = "Czynsz", date = daysAgo(4), createdAt = daysAgo(4)),
            TransactionEntity(type = "EXPENSE", amount = 135.0, categoryId = catMap["Jedzenie"], description = "Zakupy tygodniowe", date = daysAgo(3), createdAt = daysAgo(3)),
            TransactionEntity(type = "EXPENSE", amount = 48.0, categoryId = catMap["Transport"], description = "Paliwo", date = daysAgo(2), createdAt = daysAgo(2)),
            TransactionEntity(type = "EXPENSE", amount = 90.0, categoryId = catMap["Zdrowie"], description = "Lekarz", date = daysAgo(1), createdAt = daysAgo(1)),
            TransactionEntity(type = "SAVE_TO_JAR", amount = 300.0, savingsJarId = jar1Id, description = "Dokładam na wakacje", date = daysAgo(1), createdAt = daysAgo(1))
        ))

        savingsJarDao.addToAmount(jar1Id, 800.0)
        savingsJarDao.addToAmount(jar2Id, 700.0)

        val c = Calendar.getInstance()
        val currentMonth = "${c.get(Calendar.YEAR)}-${(c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}"
        budgetPlanDao.insertAll(listOf(
            BudgetPlanEntity(categoryId = catMap["Jedzenie"]!!, month = currentMonth, plannedAmount = 600.0),
            BudgetPlanEntity(categoryId = catMap["Transport"]!!, month = currentMonth, plannedAmount = 200.0),
            BudgetPlanEntity(categoryId = catMap["Zakupy"]!!, month = currentMonth, plannedAmount = 300.0),
            BudgetPlanEntity(categoryId = catMap["Rozrywka"]!!, month = currentMonth, plannedAmount = 150.0),
            BudgetPlanEntity(categoryId = catMap["Rachunki"]!!, month = currentMonth, plannedAmount = 1500.0),
            BudgetPlanEntity(categoryId = catMap["Zdrowie"]!!, month = currentMonth, plannedAmount = 150.0)
        ))
    }
}
