package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(accountId: Int): AppResult<Unit> {
        if (accountRepository.countTransactions(accountId) > 0) {
            return AppResult.Error("此帳戶已有交易記錄，暫不可刪除")
        }

        accountRepository.delete(accountId)
        return AppResult.Success(Unit)
    }
}

