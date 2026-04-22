package com.finance.manager.android.data.mapper

import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountMapper @Inject constructor() {

    fun toDomain(entity: AccountEntity): Account = Account(
        accountId = entity.accountId,
        accountName = entity.accountName,
        accountType = AccountType.fromCode(entity.accountType),
        initialBalance = entity.initialBalance,
        currentBalance = entity.currentBalance,
        isHidden = entity.isHidden,
        displayOrder = entity.displayOrder,
        currencyId = entity.currencyId,
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: Account): AccountEntity = AccountEntity(
        accountId = domain.accountId,
        accountName = domain.accountName,
        accountType = domain.accountType.code,
        initialBalance = domain.initialBalance,
        currentBalance = domain.currentBalance,
        isHidden = domain.isHidden,
        displayOrder = domain.displayOrder,
        currencyId = domain.currencyId,
        createdAt = domain.createdAt.ifBlank {
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        },
    )
}

