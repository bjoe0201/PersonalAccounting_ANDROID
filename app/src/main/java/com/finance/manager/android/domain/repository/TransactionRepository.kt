package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.model.TransactionSplitItem
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeByAccount(accountId: Int): Flow<List<TransactionListItem>>
    fun observeRecent(limit: Int): Flow<List<TransactionListItem>>
    suspend fun insert(transaction: Transaction): Long
    suspend fun getSplitsByTransactionId(transactionId: Int): List<TransactionSplitItem>
    suspend fun replaceSplits(transactionId: Int, splits: List<TransactionSplitItem>)
    suspend fun updateGeneral(transaction: Transaction)
    suspend fun updateLinkedTransactionId(transactionId: Int, linkedTransactionId: Int)
    suspend fun getById(transactionId: Int): Transaction?
    suspend fun delete(transactionId: Int)
}


