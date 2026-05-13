package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY COALESCE(date, created_at) DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE strftime('%Y-%m', datetime(COALESCE(date, created_at) / 1000, 'unixepoch')) = :month
        ORDER BY COALESCE(date, created_at) DESC
    """)
    fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE strftime('%Y', datetime(COALESCE(date, created_at) / 1000, 'unixepoch')) = :year
        AND type = 'EXPENSE'
        ORDER BY COALESCE(date, created_at) DESC
    """)
    suspend fun getExpensesByYear(year: String): List<TransactionEntity>
}
