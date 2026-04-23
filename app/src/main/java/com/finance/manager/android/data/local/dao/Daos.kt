package com.finance.manager.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import com.finance.manager.android.data.local.entity.AppSettingEntity
import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.data.local.entity.CurrencyEntity
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.data.local.entity.TagEntity
import com.finance.manager.android.data.local.entity.TransactionEntity
import com.finance.manager.android.data.local.entity.TransactionSplitEntity
import com.finance.manager.android.data.local.entity.TransactionSplitTagEntity
import com.finance.manager.android.data.local.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY display_order, account_name")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE account_id = :accountId LIMIT 1")
    fun observeById(accountId: Int): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE is_hidden = 0 ORDER BY display_order, account_name")
    fun observeVisibleAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY display_order, account_name")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE account_id = :accountId")
    suspend fun getById(accountId: Int): AccountEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE account_name = :name AND (:excludeId IS NULL OR account_id != :excludeId))")
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("DELETE FROM accounts WHERE account_id = :accountId")
    suspend fun deleteById(accountId: Int)

    @Query("UPDATE accounts SET current_balance = current_balance + :delta WHERE account_id = :accountId")
    suspend fun incrementCurrentBalance(accountId: Int, delta: Double)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.transaction_id, t.account_id, a.account_name, t.transaction_date, t.amount,
               p.payee_name, c.category_name, ta.account_name AS transfer_account_name, t.memo, t.cleared_status
        FROM transactions t
        INNER JOIN accounts a ON a.account_id = t.account_id
        LEFT JOIN payees p ON p.payee_id = t.payee_id
        LEFT JOIN categories c ON c.category_id = t.category_id
        LEFT JOIN accounts ta ON ta.account_id = t.transfer_account_id
        WHERE t.account_id = :accountId
        ORDER BY t.transaction_date DESC, t.transaction_id DESC
        """
    )
    fun observeByAccount(accountId: Int): Flow<List<TransactionListItemLocal>>

    @Query(
        """
        SELECT t.transaction_id, t.account_id, a.account_name, t.transaction_date, t.amount,
               p.payee_name, c.category_name, ta.account_name AS transfer_account_name, t.memo, t.cleared_status
        FROM transactions t
        INNER JOIN accounts a ON a.account_id = t.account_id
        LEFT JOIN payees p ON p.payee_id = t.payee_id
        LEFT JOIN categories c ON c.category_id = t.category_id
        LEFT JOIN accounts ta ON ta.account_id = t.transfer_account_id
        ORDER BY t.transaction_date DESC, t.transaction_id DESC
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int): Flow<List<TransactionListItemLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long

    @Query(
        """
        UPDATE transactions
        SET transaction_date = :transactionDate,
            amount = :amount,
            payee_id = :payeeId,
            category_id = :categoryId,
            memo = :memo,
            cleared_status = :clearedStatus
        WHERE transaction_id = :transactionId
        """
    )
    suspend fun updateGeneralTransaction(
        transactionId: Int,
        transactionDate: String,
        amount: Double,
        payeeId: Int?,
        categoryId: Int?,
        memo: String?,
        clearedStatus: String,
    )

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getById(transactionId: Int): TransactionEntity?

    @Query("DELETE FROM transactions WHERE transaction_id = :transactionId")
    suspend fun deleteById(transactionId: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET linked_transaction_id = :linkedTransactionId WHERE transaction_id = :transactionId")
    suspend fun updateLinkedTransactionId(transactionId: Int, linkedTransactionId: Int)

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId OR transfer_account_id = :accountId")
    suspend fun countByAccountId(accountId: Int): Int

    @Query("SELECT MIN(transaction_date) FROM transactions WHERE account_id = :accountId")
    suspend fun getEarliestTransactionDate(accountId: Int): String?

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :startDate AND :endDate AND amount > 0"
    )
    suspend fun sumIncomeByDateRange(accountId: Int, startDate: String, endDate: String): Double

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :startDate AND :endDate AND amount < 0"
    )
    suspend fun sumExpenseByDateRange(accountId: Int, startDate: String, endDate: String): Double

    @Query(
        "SELECT COUNT(*) FROM transactions WHERE account_id = :accountId AND transaction_date BETWEEN :startDate AND :endDate"
    )
    suspend fun countByDateRange(accountId: Int, startDate: String, endDate: String): Int

    /**
     * Category expense breakdown for a date range.
     * Handles both direct-category and split-transaction expenses.
     * Excludes transfer transactions.
     */
    @Query(
        """
        SELECT c.category_id, c.category_name,
               pc.category_name AS parent_category_name,
               SUM(line_amount) AS total_amount
        FROM (
            SELECT t.transaction_id, t.category_id AS eff_category_id, t.amount AS line_amount
            FROM transactions t
            WHERE t.transaction_date BETWEEN :startDate AND :endDate
              AND t.transfer_account_id IS NULL
              AND t.amount < 0
              AND t.category_id IS NOT NULL
            UNION ALL
            SELECT s.transaction_id, s.category_id AS eff_category_id, s.amount AS line_amount
            FROM transaction_splits s
            INNER JOIN transactions t ON t.transaction_id = s.transaction_id
            WHERE t.transaction_date BETWEEN :startDate AND :endDate
              AND t.transfer_account_id IS NULL
              AND s.amount < 0
              AND s.category_id IS NOT NULL
        ) lines
        INNER JOIN categories c ON c.category_id = lines.eff_category_id
        LEFT JOIN categories pc ON pc.category_id = c.parent_category_id
        GROUP BY c.category_id, c.category_name, pc.category_name
        ORDER BY total_amount ASC
        """
    )
    suspend fun getCategoryExpenseBreakdown(startDate: String, endDate: String): List<CategoryAmountLocal>
}

@Dao
interface TransactionSplitDao {
    @Query("SELECT * FROM transaction_splits WHERE transaction_id = :transactionId ORDER BY split_id")
    suspend fun getByTransactionId(transactionId: Int): List<TransactionSplitEntity>

    @Query("DELETE FROM transaction_splits WHERE transaction_id = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Int)

    @Query("DELETE FROM transaction_splits")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionSplitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionSplitEntity>)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE category_type = :categoryType ORDER BY parent_category_id, display_order, category_name")
    fun observeByType(categoryType: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE category_type = :categoryType ORDER BY parent_category_id, display_order, category_name")
    suspend fun getByType(categoryType: String): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY category_name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE category_id = :id LIMIT 1")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE category_name = :name AND (:excludeId IS NULL OR category_id != :excludeId))")
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean

    @Query(
        """
        SELECT c.*,
               (SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id)
             + (SELECT COUNT(*) FROM transaction_splits s WHERE s.category_id = c.category_id) AS usage_count
        FROM categories c
        ORDER BY c.category_name
        """
    )
    fun observeAllWithUsage(): Flow<List<CategoryWithUsageLocal>>

    @Query("SELECT (SELECT COUNT(*) FROM transactions t WHERE t.category_id = :id) + (SELECT COUNT(*) FROM transaction_splits s WHERE s.category_id = :id)")
    suspend fun countUsage(id: Int): Int

    @Query("SELECT COUNT(*) FROM categories WHERE parent_category_id = :parentId")
    suspend fun countChildren(parentId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("UPDATE categories SET is_hidden = :hidden WHERE category_id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Query("DELETE FROM categories WHERE category_id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface CurrencyDao {
    @Query("SELECT COUNT(*) FROM currencies")
    suspend fun getCount(): Int

    @Query("SELECT * FROM currencies ORDER BY display_order, currency_code")
    suspend fun getAll(): List<CurrencyEntity>

    @Query("SELECT * FROM currencies WHERE is_hidden = 0 ORDER BY display_order, currency_code")
    fun observeActiveCurrencies(): Flow<List<CurrencyEntity>>

    @Query("UPDATE currencies SET is_home_currency = 0")
    suspend fun clearHomeCurrency()

    @Query("UPDATE currencies SET is_home_currency = CASE WHEN currency_id = :currencyId THEN 1 ELSE is_home_currency END, rate_to_home = CASE WHEN currency_id = :currencyId THEN 1.0 ELSE rate_to_home END, rate_from_home = CASE WHEN currency_id = :currencyId THEN 1.0 ELSE rate_from_home END WHERE currency_id = :currencyId")
    suspend fun setHomeCurrency(currencyId: Int)

    @Query("UPDATE currencies SET rate_from_home = :rateFromHome, updated_at = :updatedAt WHERE currency_id = :currencyId")
    suspend fun updateRate(currencyId: Int, rateFromHome: Double, updatedAt: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CurrencyEntity>)

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()
}

@Dao
interface PayeeDao {
    @Query("SELECT * FROM payees WHERE payee_id = :payeeId LIMIT 1")
    suspend fun getById(payeeId: Int): PayeeEntity?

    @Query("SELECT * FROM payees WHERE payee_name = :name LIMIT 1")
    suspend fun findByName(name: String): PayeeEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM payees WHERE payee_name = :name AND (:excludeId IS NULL OR payee_id != :excludeId))")
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean

    @Query(
        """
        SELECT p.*,
               COALESCE(SUM(ABS(t.amount)), 0) AS total_amount,
               COUNT(t.transaction_id) AS usage_count
        FROM payees p
        LEFT JOIN transactions t ON t.payee_id = p.payee_id
        GROUP BY p.payee_id
        ORDER BY p.payee_name
        """
    )
    fun observeAllWithStats(): Flow<List<PayeeWithUsageLocal>>

    @Query("SELECT COUNT(*) FROM transactions WHERE payee_id = :id")
    suspend fun countUsage(id: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PayeeEntity): Long

    @Update
    suspend fun update(entity: PayeeEntity)

    @Query("UPDATE payees SET is_hidden = :hidden WHERE payee_id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Query("DELETE FROM payees WHERE payee_id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM payees")
    suspend fun deleteAll()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE tag_id = :id LIMIT 1")
    suspend fun getById(id: Int): TagEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM tags WHERE tag_name = :name AND (:excludeId IS NULL OR tag_id != :excludeId))")
    suspend fun existsByName(name: String, excludeId: Int? = null): Boolean

    @Query(
        """
        SELECT g.*,
               (SELECT COUNT(*) FROM transaction_tags tt WHERE tt.tag_id = g.tag_id)
             + (SELECT COUNT(*) FROM transaction_split_tags st WHERE st.tag_id = g.tag_id) AS usage_count
        FROM tags g
        ORDER BY g.tag_name
        """
    )
    fun observeAllWithUsage(): Flow<List<TagWithUsageLocal>>

    @Query("SELECT (SELECT COUNT(*) FROM transaction_tags tt WHERE tt.tag_id = :id) + (SELECT COUNT(*) FROM transaction_split_tags st WHERE st.tag_id = :id)")
    suspend fun countUsage(id: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TagEntity): Long

    @Update
    suspend fun update(entity: TagEntity)

    @Query("UPDATE tags SET is_hidden = :hidden WHERE tag_id = :id")
    suspend fun setHidden(id: Int, hidden: Boolean)

    @Query("DELETE FROM tags WHERE tag_id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}

@Dao
interface TransactionTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionTagEntity): Long

    @Query("DELETE FROM transaction_tags")
    suspend fun deleteAll()
}

@Dao
interface TransactionSplitTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionSplitTagEntity): Long

    @Query("DELETE FROM transaction_split_tags")
    suspend fun deleteAll()
}

@Dao
interface AccountMonthlyBalanceDao {
    @Query("SELECT * FROM account_monthly_balances WHERE account_id = :accountId AND year = :year AND month = :month LIMIT 1")
    suspend fun getByAccountYearMonth(accountId: Int, year: Int, month: Int): AccountMonthlyBalanceEntity?

    @Query(
        "SELECT * FROM account_monthly_balances WHERE account_id = :accountId AND (year < :year OR (year = :year AND month < :month)) ORDER BY year DESC, month DESC LIMIT 1"
    )
    suspend fun getLatestBefore(accountId: Int, year: Int, month: Int): AccountMonthlyBalanceEntity?

    @Query(
        "SELECT * FROM account_monthly_balances WHERE account_id = :accountId AND (year > :year OR (year = :year AND month > :month)) ORDER BY year, month"
    )
    suspend fun getLaterSnapshots(accountId: Int, year: Int, month: Int): List<AccountMonthlyBalanceEntity>

    @Query("SELECT * FROM account_monthly_balances WHERE year = :year AND month = :month")
    suspend fun getAllByYearMonth(year: Int, month: Int): List<AccountMonthlyBalanceEntity>

    @Query("SELECT * FROM account_monthly_balances WHERE year = :year ORDER BY month")
    suspend fun getAllByYear(year: Int): List<AccountMonthlyBalanceEntity>

    @Query("DELETE FROM account_monthly_balances")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AccountMonthlyBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AccountMonthlyBalanceEntity>)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): AppSettingEntity?

    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingEntity)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}







