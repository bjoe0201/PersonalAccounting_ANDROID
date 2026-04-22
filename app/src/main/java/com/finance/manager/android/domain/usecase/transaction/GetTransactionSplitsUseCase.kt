package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.domain.model.TransactionSplitItem
import com.finance.manager.android.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionSplitsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: Int): List<TransactionSplitItem> =
        transactionRepository.getSplitsByTransactionId(transactionId)
}

