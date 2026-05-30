package com.example.budgetapp

import android.app.Application
import android.content.Context
import com.example.budgetapp.data.local.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgetApp : Application() {

    val container: AppContainer by lazy { AppContainerImpl(this) }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("seeded", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseSeeder.seed(container.database)
                prefs.edit().putBoolean("seeded", true).apply()
            }
        }
        if (!prefs.getBoolean("fwn_seeded", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseSeeder.seedFwn(container.database)
                prefs.edit().putBoolean("fwn_seeded", true).apply()
            }
        }
    }
}
