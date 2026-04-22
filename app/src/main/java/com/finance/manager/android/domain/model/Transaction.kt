package com.finance.manager.android.domain.model

import java.time.LocalDate

data class Transaction(
    val transactionId: Int = 0,
    val accountId: Int,
    val transactionDate: LocalDate,
    val amount: Double,
    val payeeId: Int? = null,
    val categoryId: Int? = null,
    val transferAccountId: Int? = null,
    val linkedTransactionId: Int? = null,
    val memo: String? = null,
    val clearedStatus: String = "U",
)

data class TransactionListItem(
    val transactionId: Int,
    val accountId: Int,
    val accountName: String,
    val transactionDate: LocalDate,
    val amount: Double,
    val payeeName: String? = null,
    val categoryName: String? = null,
    val transferAccountName: String? = null,
    val memo: String? = null,
    val clearedStatus: String = "U",
)

data class TransactionSplitItem(
    val splitId: Int = 0,
    val transactionId: Int,
    val categoryId: Int,
    val amount: Double,
    val memo: String? = null,
)

