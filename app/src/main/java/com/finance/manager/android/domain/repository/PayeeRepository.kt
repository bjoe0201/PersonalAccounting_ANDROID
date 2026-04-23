package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Payee
import kotlinx.coroutines.flow.Flow

interface PayeeRepository {
    suspend fun getNameById(payeeId: Int): String?
    suspend fun findByName(name: String): Int?
    suspend fun insert(name: String): Int

    fun observeAllWithStats(): Flow<List<Payee>>
    suspend fun getDomainById(id: Int): Payee?
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean
    suspend fun insertDomain(payee: Payee): Long
    suspend fun update(payee: Payee)
    suspend fun setHidden(id: Int, hidden: Boolean)
    suspend fun delete(id: Int)
    suspend fun countUsage(id: Int): Int
}

