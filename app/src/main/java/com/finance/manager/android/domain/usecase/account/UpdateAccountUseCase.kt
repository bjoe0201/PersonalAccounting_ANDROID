package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        accountId: Int,
        accountName: String,
        accountType: AccountType,
        initialBalance: Double,
        isHidden: Boolean,
        currencyId: Int? = null,
    ): AppResult<Unit> {
        val existing = accountRepository.getById(accountId) ?: return AppResult.Error("找不到帳戶")
        val normalizedName = accountName.trim()
        if (normalizedName.isBlank()) return AppResult.Error("帳戶名稱不可空白")
        if (accountRepository.existsByName(normalizedName, excludeId = accountId)) {
            return AppResult.Error("帳戶名稱不可重複")
        }

        val transactionCount = accountRepository.countTransactions(accountId)
        if (transactionCount > 0 && initialBalance != existing.initialBalance) {
            return AppResult.Error("已有交易的帳戶不可修改初始餘額")
        }

        val updatedCurrentBalance = if (transactionCount == 0) initialBalance else existing.currentBalance
        accountRepository.update(
            existing.copy(
                accountName = normalizedName,
                accountType = accountType,
                initialBalance = initialBalance,
                currentBalance = updatedCurrentBalance,
                isHidden = isHidden,
                currencyId = currencyId ?: existing.currencyId,
            )
        )
        return AppResult.Success(Unit)
    }
}

