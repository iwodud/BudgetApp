package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.FwnPlannedExpenseEntity
import com.example.budgetapp.data.local.entity.FwnTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FwnPlannedExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: FwnPlannedExpenseEntity): Long
    @Update suspend fun update(entity: FwnPlannedExpenseEntity)
    @Delete suspend fun delete(entity: FwnPlannedExpenseEntity)
    @Query("SELECT * FROM fwn_planned_expenses ORDER BY month ASC, name ASC")
    fun getAll(): Flow<List<FwnPlannedExpenseEntity>>
}

@Dao
interface FwnTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: FwnTransactionEntity): Long
    @Delete suspend fun delete(entity: FwnTransactionEntity)
    @Query("SELECT * FROM fwn_transactions ORDER BY date DESC")
    fun getAll(): Flow<List<FwnTransactionEntity>>
}
