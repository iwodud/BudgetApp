package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.CategoryDao
import com.example.budgetapp.data.local.entity.toDomain
import com.example.budgetapp.data.local.entity.toEntity
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.CategoryType
import com.example.budgetapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(private val dao: CategoryDao) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        dao.getAllCategories().map { it.map { e -> e.toDomain() } }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
        dao.getCategoriesByType(type.name).map { it.map { e -> e.toDomain() } }

    override suspend fun getAllCategoriesOnce(): List<Category> =
        dao.getAllCategoriesOnce().map { it.toDomain() }

    override suspend fun insertCategory(category: Category): Long =
        dao.insert(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        dao.update(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        dao.delete(category.toEntity())
}
