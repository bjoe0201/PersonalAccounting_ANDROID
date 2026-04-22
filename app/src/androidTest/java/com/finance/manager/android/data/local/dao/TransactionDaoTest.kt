package com.finance.manager.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.data.local.entity.TransactionEntity
import com.finance.manager.android.data.local.entity.TransactionSplitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var txDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var splitDao: TransactionSplitDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var payeeDao: PayeeDao

    private val now = "2026-01-01T00:00:00"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        txDao = db.transactionDao()
        accountDao = db.accountDao()
        splitDao = db.transactionSplitDao()
        categoryDao = db.categoryDao()
        payeeDao = db.payeeDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertAccount(name: String = "Bank", balance: Double = 0.0): Int =
        accountDao.insert(AccountEntity(accountName = name, accountType = "Bank", currentBalance = balance, initialBalance = balance, createdAt = now)).toInt()

    private suspend fun insertTx(accountId: Int, date: String, amount: Double, categoryId: Int? = null, payeeId: Int? = null): Int =
        txDao.insert(TransactionEntity(accountId = accountId, transactionDate = date, amount = amount, categoryId = categoryId, payeeId = payeeId, createdAt = now)).toInt()

    // ─── insert / getById ─────────────────────────────────────────────────────

    @Test
    fun insertAndGetById() = runTest {
        val accId = insertAccount()
        val txId = insertTx(accId, "2026-04-01", -500.0)
        val result = txDao.getById(txId)
        assertNotNull(result)
        assertEquals(-500.0, result!!.amount, 0.001)
    }

    @Test
    fun getById_returnsNullForMissing() = runTest {
        assertNull(txDao.getById(999))
    }

    // ─── observeByAccount ─────────────────────────────────────────────────────

    @Test
    fun observeByAccount_returnsOnlyGivenAccount() = runTest {
        val acc1 = insertAccount("A")
        val acc2 = insertAccount("B")
        insertTx(acc1, "2026-04-01", -100.0)
        insertTx(acc2, "2026-04-02", -200.0)

        val items = txDao.observeByAccount(acc1).first()
        assertEquals(1, items.size)
        assertEquals(-100.0, items.first().amount, 0.001)
    }

    // ─── deleteById ───────────────────────────────────────────────────────────

    @Test
    fun deleteById_removesTransaction() = runTest {
        val accId = insertAccount()
        val txId = insertTx(accId, "2026-04-01", -100.0)
        txDao.deleteById(txId)
        assertNull(txDao.getById(txId))
    }

    // ─── updateLinkedTransactionId ────────────────────────────────────────────

    @Test
    fun updateLinkedTransactionId_setsValue() = runTest {
        val accId = insertAccount()
        val tx1 = insertTx(accId, "2026-04-01", -1000.0)
        val tx2 = insertTx(accId, "2026-04-01", 1000.0)
        txDao.updateLinkedTransactionId(tx1, tx2)
        assertEquals(tx2, txDao.getById(tx1)!!.linkedTransactionId)
    }

    // ─── getEarliestTransactionDate ───────────────────────────────────────────

    @Test
    fun getEarliestTransactionDate_returnsMinDate() = runTest {
        val accId = insertAccount()
        insertTx(accId, "2026-03-15", -50.0)
        insertTx(accId, "2026-01-01", -100.0)
        insertTx(accId, "2026-04-20", -200.0)
        assertEquals("2026-01-01", txDao.getEarliestTransactionDate(accId))
    }

    @Test
    fun getEarliestTransactionDate_returnsNullWhenNoTransactions() = runTest {
        val accId = insertAccount()
        assertNull(txDao.getEarliestTransactionDate(accId))
    }

    // ─── sumIncomeByDateRange / sumExpenseByDateRange ─────────────────────────

    @Test
    fun sumIncomeByDateRange_sumsPositiveAmounts() = runTest {
        val accId = insertAccount()
        insertTx(accId, "2026-04-01", 5000.0)
        insertTx(accId, "2026-04-15", 3000.0)
        insertTx(accId, "2026-04-20", -200.0)  // expense, excluded
        val income = txDao.sumIncomeByDateRange(accId, "2026-04-01", "2026-04-30")
        assertEquals(8000.0, income, 0.001)
    }

    @Test
    fun sumExpenseByDateRange_sumsNegativeAmounts() = runTest {
        val accId = insertAccount()
        insertTx(accId, "2026-04-01", -1000.0)
        insertTx(accId, "2026-04-10", -500.0)
        insertTx(accId, "2026-04-20", 2000.0)  // income, excluded
        val expense = txDao.sumExpenseByDateRange(accId, "2026-04-01", "2026-04-30")
        assertEquals(-1500.0, expense, 0.001)
    }

    @Test
    fun sumIncomeByDateRange_returnsZeroWhenNoMatch() = runTest {
        val accId = insertAccount()
        val income = txDao.sumIncomeByDateRange(accId, "2026-04-01", "2026-04-30")
        assertEquals(0.0, income, 0.001)
    }

    // ─── countByDateRange ─────────────────────────────────────────────────────

    @Test
    fun countByDateRange_countsCorrectly() = runTest {
        val accId = insertAccount()
        insertTx(accId, "2026-04-01", -100.0)
        insertTx(accId, "2026-04-15", 200.0)
        insertTx(accId, "2026-05-01", -50.0)  // outside range
        assertEquals(2, txDao.countByDateRange(accId, "2026-04-01", "2026-04-30"))
    }

    // ─── observeRecent ────────────────────────────────────────────────────────

    @Test
    fun observeRecent_respectsLimit() = runTest {
        val accId = insertAccount()
        repeat(5) { i -> insertTx(accId, "2026-04-${(i + 1).toString().padStart(2, '0')}", -100.0) }
        val items = txDao.observeRecent(3).first()
        assertEquals(3, items.size)
    }

    // ─── getCategoryExpenseBreakdown ──────────────────────────────────────────

    @Test
    fun getCategoryExpenseBreakdown_aggregatesByCategory() = runTest {
        val accId = insertAccount()
        val parentId = categoryDao.insert(CategoryEntity(categoryName = "Food", categoryType = "E", createdAt = now)).toInt()
        val catId = categoryDao.insert(CategoryEntity(categoryName = "Lunch", categoryType = "E", parentCategoryId = parentId, createdAt = now)).toInt()

        insertTx(accId, "2026-04-01", -300.0, categoryId = catId)
        insertTx(accId, "2026-04-10", -200.0, categoryId = catId)

        val result = txDao.getCategoryExpenseBreakdown("2026-04-01", "2026-04-30")
        assertEquals(1, result.size)
        assertEquals(-500.0, result.first().totalAmount, 0.001)
        assertEquals("Lunch", result.first().categoryName)
        assertEquals("Food", result.first().parentCategoryName)
    }

    @Test
    fun getCategoryExpenseBreakdown_includesSplitTransactions() = runTest {
        val accId = insertAccount()
        val parentId = categoryDao.insert(CategoryEntity(categoryName = "P", categoryType = "E", createdAt = now)).toInt()
        val cat1 = categoryDao.insert(CategoryEntity(categoryName = "Cat1", categoryType = "E", parentCategoryId = parentId, createdAt = now)).toInt()
        val cat2 = categoryDao.insert(CategoryEntity(categoryName = "Cat2", categoryType = "E", parentCategoryId = parentId, createdAt = now)).toInt()

        // Split transaction (category_id NULL on parent)
        val txId = txDao.insert(TransactionEntity(accountId = accId, transactionDate = "2026-04-05", amount = -500.0, createdAt = now)).toInt()
        db.transactionSplitDao().insert(TransactionSplitEntity(transactionId = txId, categoryId = cat1, amount = -300.0, createdAt = now))
        db.transactionSplitDao().insert(TransactionSplitEntity(transactionId = txId, categoryId = cat2, amount = -200.0, createdAt = now))

        val result = txDao.getCategoryExpenseBreakdown("2026-04-01", "2026-04-30")
        assertEquals(2, result.size)
        val total = result.sumOf { it.totalAmount }
        assertEquals(-500.0, total, 0.001)
    }

    @Test
    fun getCategoryExpenseBreakdown_excludesTransfers() = runTest {
        val acc1 = insertAccount("A")
        val acc2 = insertAccount("B")
        val parentId = categoryDao.insert(CategoryEntity(categoryName = "P", categoryType = "E", createdAt = now)).toInt()
        val catId = categoryDao.insert(CategoryEntity(categoryName = "C", categoryType = "E", parentCategoryId = parentId, createdAt = now)).toInt()

        // Transfer transaction (transfer_account_id set)
        txDao.insert(TransactionEntity(accountId = acc1, transactionDate = "2026-04-01", amount = -1000.0, transferAccountId = acc2, categoryId = catId, createdAt = now))

        val result = txDao.getCategoryExpenseBreakdown("2026-04-01", "2026-04-30")
        assertEquals(0, result.size)
    }
}

