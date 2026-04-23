package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeByType(categoryType: String): Flow<List<Category>>
    suspend fun getByType(categoryType: String): List<Category>
    fun observeAllWithUsage(): Flow<List<Category>>
    suspend fun getById(id: Int): Category?
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun setHidden(id: Int, hidden: Boolean)
    suspend fun delete(id: Int)
    suspend fun countUsage(id: Int): Int
    suspend fun countChildren(id: Int): Int
}

