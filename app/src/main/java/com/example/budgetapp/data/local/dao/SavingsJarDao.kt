package com.example.budgetapp.data.local.dao

import androidx.room.*
import com.example.budgetapp.data.local.entity.SavingsJarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsJarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jar: SavingsJarEntity): Long

    @Update
    suspend fun update(jar: SavingsJarEntity)

    @Delete
    suspend fun delete(jar: SavingsJarEntity)

    @Query("SELECT * FROM savings_jars ORDER BY name ASC")
    fun getAllSavingsJars(): Flow<List<SavingsJarEntity>>

    @Query("SELECT * FROM savings_jars WHERE id = :id LIMIT 1")
    suspend fun getSavingsJarById(id: Long): SavingsJarEntity?

    @Query("SELECT * FROM savings_jars ORDER BY name ASC")
    suspend fun getAllSavingsJarsOnce(): List<SavingsJarEntity>

    @Query("UPDATE savings_jars SET current_amount = current_amount + :delta WHERE id = :id")
    suspend fun addToAmount(id: Long, delta: Double)
}
