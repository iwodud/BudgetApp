package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ZrzutkaPersonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(person: ZrzutkaPersonEntity): Long
    @Delete suspend fun delete(person: ZrzutkaPersonEntity)
    @Query("SELECT * FROM zrzutka_persons ORDER BY name ASC") fun getAllPersons(): Flow<List<ZrzutkaPersonEntity>>
    @Query("SELECT * FROM zrzutka_persons ORDER BY name ASC") suspend fun getAllPersonsOnce(): List<ZrzutkaPersonEntity>
    @Query("DELETE FROM zrzutka_persons") suspend fun deleteAll()
}

@Dao
interface ZrzutkaExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(expense: ZrzutkaExpenseEntity): Long
    @Delete suspend fun delete(expense: ZrzutkaExpenseEntity)
    @Query("SELECT * FROM zrzutka_expenses ORDER BY date DESC") fun getAllExpenses(): Flow<List<ZrzutkaExpenseEntity>>
    @Query("SELECT * FROM zrzutka_expenses ORDER BY date DESC") suspend fun getAllExpensesOnce(): List<ZrzutkaExpenseEntity>
    @Query("UPDATE zrzutka_expenses SET settled = 1 WHERE id IN (:ids)") suspend fun markSettled(ids: List<Long>)
    @Query("DELETE FROM zrzutka_expenses") suspend fun deleteAll()
}

@Dao
interface ZrzutkaSplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(splits: List<ZrzutkaSplitEntity>)
    @Query("SELECT * FROM zrzutka_splits") fun getAllSplits(): Flow<List<ZrzutkaSplitEntity>>
    @Query("SELECT * FROM zrzutka_splits") suspend fun getAllSplitsOnce(): List<ZrzutkaSplitEntity>
    @Query("DELETE FROM zrzutka_splits WHERE person_id = :personId") suspend fun deleteAllSplitsForPerson(personId: Long)
}
