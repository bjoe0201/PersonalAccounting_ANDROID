package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    operator fun invoke(accountId: Int): Flow<Account?> = accountRepository.observeById(accountId)
}

