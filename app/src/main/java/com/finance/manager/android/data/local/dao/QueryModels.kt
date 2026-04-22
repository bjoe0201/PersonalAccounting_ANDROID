package com.finance.manager.android.data.local.dao

import androidx.room.ColumnInfo

data class CategoryAmountLocal(
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "parent_category_name") val parentCategoryName: String?,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
)

data class TransactionListItemLocal(
    @ColumnInfo(name = "transaction_id") val transactionId: Int,
    @ColumnInfo(name = "account_id") val accountId: Int,
    @ColumnInfo(name = "account_name") val accountName: String? = null,
    @ColumnInfo(name = "transaction_date") val transactionDate: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "payee_name") val payeeName: String? = null,
    @ColumnInfo(name = "category_name") val categoryName: String? = null,
    @ColumnInfo(name = "transfer_account_name") val transferAccountName: String? = null,
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "cleared_status") val clearedStatus: String,
)

