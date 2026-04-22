package com.finance.manager.android.data.mapper

import com.finance.manager.android.data.local.dao.TransactionListItemLocal
import com.finance.manager.android.data.local.entity.TransactionEntity
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.model.TransactionListItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionMapper @Inject constructor() {

    fun toEntity(domain: Transaction): TransactionEntity = TransactionEntity(
        transactionId = domain.transactionId,
        accountId = domain.accountId,
        transactionDate = domain.transactionDate.toString(),
        amount = domain.amount,
        payeeId = domain.payeeId,
        categoryId = domain.categoryId,
        transferAccountId = domain.transferAccountId,
        linkedTransactionId = domain.linkedTransactionId,
        memo = domain.memo,
        clearedStatus = domain.clearedStatus,
        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    )

    fun toListItem(local: TransactionListItemLocal): TransactionListItem = TransactionListItem(
        transactionId = local.transactionId,
        accountId = local.accountId,
        accountName = local.accountName.orEmpty(),
        transactionDate = LocalDate.parse(local.transactionDate),
        amount = local.amount,
        payeeName = local.payeeName,
        categoryName = local.categoryName,
        transferAccountName = local.transferAccountName,
        memo = local.memo,
        clearedStatus = local.clearedStatus,
    )
}

