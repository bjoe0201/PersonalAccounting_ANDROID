package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeById(accountId: Int): Flow<Account?>
    suspend fun getById(accountId: Int): Account?
    suspend fun getAll(): List<Account>
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun delete(accountId: Int)
    suspend fun countTransactions(accountId: Int): Int
}


