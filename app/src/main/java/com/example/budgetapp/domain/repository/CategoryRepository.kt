package com.example.budgetapp.domain.repository

import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun getAllCategoriesOnce(): List<Category>
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}
