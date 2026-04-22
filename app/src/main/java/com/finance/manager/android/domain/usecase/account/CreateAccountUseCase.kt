package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.repository.AccountRepository
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        accountName: String,
        accountType: AccountType,
        initialBalance: Double,
        isHidden: Boolean,
        currencyId: Int? = null,
    ): AppResult<Long> {
        val normalizedName = accountName.trim()
        if (normalizedName.isBlank()) return AppResult.Error("帳戶名稱不可空白")
        if (accountRepository.existsByName(normalizedName)) return AppResult.Error("帳戶名稱不可重複")

        return AppResult.Success(
            accountRepository.insert(
                Account(
                    accountName = normalizedName,
                    accountType = accountType,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    isHidden = isHidden,
                    currencyId = currencyId,
                )
            )
        )
    }
}

