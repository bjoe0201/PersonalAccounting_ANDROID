package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetAccountTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(accountId: Int): Flow<List<TransactionListItem>> = transactionRepository.observeByAccount(accountId)
}

