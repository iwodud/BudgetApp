package com.example.budgetapp.data.local.entity

import androidx.room.*

@Entity(tableName = "zrzutka_persons")
data class ZrzutkaPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "zrzutka_expenses")
data class ZrzutkaExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "payer_id") val payerId: Long,
    val settled: Boolean = false
)

@Entity(
    tableName = "zrzutka_splits",
    foreignKeys = [ForeignKey(
        entity = ZrzutkaExpenseEntity::class,
        parentColumns = ["id"],
        childColumns = ["expense_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("expense_id")]
)
data class ZrzutkaSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "person_id") val personId: Long,
    @ColumnInfo(name = "share_amount") val shareAmount: Double
)
