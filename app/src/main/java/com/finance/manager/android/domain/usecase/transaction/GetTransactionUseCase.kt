package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transactionId: Int): Transaction? = transactionRepository.getById(transactionId)
}

