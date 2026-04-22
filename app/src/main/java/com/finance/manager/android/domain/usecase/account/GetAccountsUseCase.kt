package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = accountRepository.observeAccounts()
}

