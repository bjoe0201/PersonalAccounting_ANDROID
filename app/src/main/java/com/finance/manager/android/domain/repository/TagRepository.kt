package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeAllWithUsage(): Flow<List<Tag>>
    suspend fun getById(id: Int): Tag?
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean
    suspend fun insert(tag: Tag): Long
    suspend fun update(tag: Tag)
    suspend fun setHidden(id: Int, hidden: Boolean)
    suspend fun delete(id: Int)
    suspend fun countUsage(id: Int): Int
}

