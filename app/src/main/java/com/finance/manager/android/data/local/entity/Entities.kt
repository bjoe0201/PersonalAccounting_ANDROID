package com.finance.manager.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("user_id")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "account_id") val accountId: Int = 0,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "account_type") val accountType: String,
    @ColumnInfo(name = "initial_balance") val initialBalance: Double = 0.0,
    @ColumnInfo(name = "current_balance") val currentBalance: Double = 0.0,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
    @ColumnInfo(name = "currency_id") val currencyId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "transactions", indices = [Index("account_id"), Index("transaction_date"), Index("linked_transaction_id")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transaction_id") val transactionId: Int = 0,
    @ColumnInfo(name = "account_id") val accountId: Int,
    @ColumnInfo(name = "transaction_date") val transactionDate: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "payee_id") val payeeId: Int? = null,
    @ColumnInfo(name = "category_id") val categoryId: Int? = null,
    @ColumnInfo(name = "transfer_account_id") val transferAccountId: Int? = null,
    @ColumnInfo(name = "linked_transaction_id") val linkedTransactionId: Int? = null,
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "check_number") val checkNumber: String? = null,
    @ColumnInfo(name = "cleared_status") val clearedStatus: String = "U",
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "transaction_splits", indices = [Index("transaction_id"), Index("linked_transaction_id")])
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "split_id") val splitId: Int = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int? = null,
    @ColumnInfo(name = "transfer_account_id") val transferAccountId: Int? = null,
    @ColumnInfo(name = "linked_transaction_id") val linkedTransactionId: Int? = null,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "categories", indices = [Index("parent_category_id"), Index("category_type")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id") val categoryId: Int = 0,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_type") val categoryType: String,
    @ColumnInfo(name = "parent_category_id") val parentCategoryId: Int? = null,
    @ColumnInfo(name = "tax_line") val taxLine: String? = null,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "currencies", indices = [Index(value = ["currency_code"], unique = true)])
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "currency_id") val currencyId: Int = 0,
    @ColumnInfo(name = "currency_name") val currencyName: String,
    @ColumnInfo(name = "currency_symbol") val currencySymbol: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "shortcut_letter") val shortcutLetter: String? = null,
    @ColumnInfo(name = "rate_to_home") val rateToHome: Double = 1.0,
    @ColumnInfo(name = "rate_from_home") val rateFromHome: Double = 1.0,
    @ColumnInfo(name = "is_home_currency") val isHomeCurrency: Boolean = false,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

@Entity(tableName = "payees", indices = [Index(value = ["payee_name"], unique = true)])
data class PayeeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "payee_id") val payeeId: Int = 0,
    @ColumnInfo(name = "payee_name") val payeeName: String,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "tags", indices = [Index(value = ["tag_name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "tag_id") val tagId: Int = 0,
    @ColumnInfo(name = "tag_name") val tagName: String,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(
    tableName = "transaction_tags",
    indices = [Index(value = ["transaction_id", "tag_id"], unique = true)]
)
data class TransactionTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Int,
    @ColumnInfo(name = "tag_id") val tagId: Int,
)

@Entity(
    tableName = "transaction_split_tags",
    indices = [Index(value = ["split_id", "tag_id"], unique = true)]
)
data class TransactionSplitTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "split_id") val splitId: Int,
    @ColumnInfo(name = "tag_id") val tagId: Int,
)

@Entity(
    tableName = "account_monthly_balances",
    indices = [Index(value = ["account_id", "year", "month"], unique = true)]
)
data class AccountMonthlyBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "account_id") val accountId: Int,
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "month") val month: Int,
    @ColumnInfo(name = "begin_balance") val beginBalance: Double,
    @ColumnInfo(name = "end_balance") val endBalance: Double,
    @ColumnInfo(name = "total_income") val totalIncome: Double,
    @ColumnInfo(name = "total_expense") val totalExpense: Double,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
    @ColumnInfo(name = "user_id") val userId: String = "local",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

