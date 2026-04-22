package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.data.local.dao.TransactionSplitDao
import com.finance.manager.android.data.local.entity.TransactionSplitEntity
import com.finance.manager.android.data.mapper.TransactionMapper
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.model.TransactionSplitItem
import com.finance.manager.android.domain.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionSplitDao: TransactionSplitDao,
    private val transactionMapper: TransactionMapper,
) : TransactionRepository {

    override fun observeByAccount(accountId: Int): Flow<List<TransactionListItem>> =
        transactionDao.observeByAccount(accountId).map { items -> items.map(transactionMapper::toListItem) }

    override fun observeRecent(limit: Int): Flow<List<TransactionListItem>> =
        transactionDao.observeRecent(limit).map { items -> items.map(transactionMapper::toListItem) }

    override suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transactionMapper.toEntity(transaction))

    override suspend fun getSplitsByTransactionId(transactionId: Int): List<TransactionSplitItem> =
        transactionSplitDao.getByTransactionId(transactionId).map {
            TransactionSplitItem(
                splitId = it.splitId,
                transactionId = it.transactionId,
                categoryId = it.categoryId ?: 0,
                amount = it.amount,
                memo = it.memo,
            )
        }.filter { it.categoryId != 0 }

    override suspend fun replaceSplits(transactionId: Int, splits: List<TransactionSplitItem>) {
        transactionSplitDao.deleteByTransactionId(transactionId)
        if (splits.isNotEmpty()) {
            transactionSplitDao.insertAll(
                splits.map {
                    TransactionSplitEntity(
                        transactionId = transactionId,
                        categoryId = it.categoryId,
                        amount = it.amount,
                        memo = it.memo,
                        createdAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    )
                }
            )
        }
    }

    override suspend fun updateGeneral(transaction: Transaction) {
        transactionDao.updateGeneralTransaction(
            transactionId = transaction.transactionId,
            transactionDate = transaction.transactionDate.toString(),
            amount = transaction.amount,
            payeeId = transaction.payeeId,
            categoryId = transaction.categoryId,
            memo = transaction.memo,
            clearedStatus = transaction.clearedStatus,
        )
    }

    override suspend fun updateLinkedTransactionId(transactionId: Int, linkedTransactionId: Int) {
        transactionDao.updateLinkedTransactionId(transactionId, linkedTransactionId)
    }

    override suspend fun getById(transactionId: Int): Transaction? =
        transactionDao.getById(transactionId)?.let {
            Transaction(
                transactionId = it.transactionId,
                accountId = it.accountId,
                transactionDate = LocalDate.parse(it.transactionDate),
                amount = it.amount,
                payeeId = it.payeeId,
                categoryId = it.categoryId,
                transferAccountId = it.transferAccountId,
                linkedTransactionId = it.linkedTransactionId,
                memo = it.memo,
                clearedStatus = it.clearedStatus,
            )
        }

    override suspend fun delete(transactionId: Int) {
        transactionDao.deleteById(transactionId)
    }
}


