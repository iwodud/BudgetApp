package com.example.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.SavingsJar

@Entity(tableName = "savings_jars")
data class SavingsJarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "current_amount") val currentAmount: Double = 0.0,
    @ColumnInfo(name = "goal_amount") val goalAmount: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

fun SavingsJarEntity.toDomain() = SavingsJar(
    id = id,
    name = name,
    currentAmount = currentAmount,
    goalAmount = goalAmount,
    createdAt = createdAt
)

fun SavingsJar.toEntity() = SavingsJarEntity(
    id = id,
    name = name,
    currentAmount = currentAmount,
    goalAmount = goalAmount,
    createdAt = createdAt
)
