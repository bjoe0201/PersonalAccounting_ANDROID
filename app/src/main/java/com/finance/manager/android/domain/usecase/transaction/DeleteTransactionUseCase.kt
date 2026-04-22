package com.finance.manager.android.domain.usecase.transaction

import androidx.room.withTransaction
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import com.finance.manager.android.domain.usecase.snapshot.UpdateSnapshotUseCase
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val db: AppDatabase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val updateSnapshotUseCase: UpdateSnapshotUseCase,
) {
    suspend operator fun invoke(transactionId: Int): AppResult<Unit> {
        val transaction = transactionRepository.getById(transactionId) ?: return AppResult.Error("找不到交易")
        val account = accountRepository.getById(transaction.accountId) ?: return AppResult.Error("找不到帳戶")
        db.withTransaction {
            transaction.linkedTransactionId?.let { linkedId ->
                val linkedTransaction = transactionRepository.getById(linkedId)
                if (linkedTransaction != null) {
                    val linkedAccount = accountRepository.getById(linkedTransaction.accountId)
                    transactionRepository.delete(linkedId)
                    if (linkedAccount != null) {
                        updateSnapshotUseCase(linkedAccount, linkedTransaction.transactionDate, -linkedTransaction.amount)
                    }
                }
            }
            transactionRepository.delete(transactionId)
            updateSnapshotUseCase(account, transaction.transactionDate, -transaction.amount)
        }
        return AppResult.Success(Unit)
    }
}

