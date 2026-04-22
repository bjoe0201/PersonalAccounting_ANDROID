package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject

class GetAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(accountId: Int): Account? = accountRepository.getById(accountId)
}

