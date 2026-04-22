package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.AccountDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.data.mapper.AccountMapper
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val accountMapper: AccountMapper,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAccounts().map { accounts -> accounts.map(accountMapper::toDomain) }

    override fun observeById(accountId: Int): Flow<Account?> =
        accountDao.observeById(accountId).map { entity -> entity?.let(accountMapper::toDomain) }

    override suspend fun getById(accountId: Int): Account? =
        accountDao.getById(accountId)?.let(accountMapper::toDomain)

    override suspend fun getAll(): List<Account> =
        accountDao.getAll().map(accountMapper::toDomain)

    override suspend fun existsByName(name: String, excludeId: Int?): Boolean =
        accountDao.existsByName(name = name, excludeId = excludeId)

    override suspend fun insert(account: Account): Long =
        accountDao.insert(accountMapper.toEntity(account))

    override suspend fun update(account: Account) {
        accountDao.update(accountMapper.toEntity(account))
    }

    override suspend fun delete(accountId: Int) {
        accountDao.deleteById(accountId)
    }

    override suspend fun countTransactions(accountId: Int): Int =
        transactionDao.countByAccountId(accountId)
}


