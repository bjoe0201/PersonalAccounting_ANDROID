package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeByType(categoryType: String): Flow<List<Category>>
    suspend fun getByType(categoryType: String): List<Category>
}

