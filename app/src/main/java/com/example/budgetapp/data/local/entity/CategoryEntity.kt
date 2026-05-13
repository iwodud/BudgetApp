package com.example.budgetapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#6750A4"
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = CategoryType.valueOf(type),
    isDefault = isDefault,
    colorHex = colorHex
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    type = type.name,
    isDefault = isDefault,
    colorHex = colorHex
)
