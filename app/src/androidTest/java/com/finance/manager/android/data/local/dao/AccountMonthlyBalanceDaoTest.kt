package com.finance.manager.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountMonthlyBalanceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var snapshotDao: AccountMonthlyBalanceDao
    private lateinit var accountDao: AccountDao

    private val now = "2026-01-01T00:00:00"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        snapshotDao = db.accountMonthlyBalanceDao()
        accountDao = db.accountDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertAccount(): Int =
        accountDao.insert(AccountEntity(accountName = "Bank", accountType = "Bank", createdAt = now)).toInt()

    private fun snapshot(accountId: Int, year: Int, month: Int, begin: Double = 0.0, end: Double = 0.0) =
        AccountMonthlyBalanceEntity(
            accountId = accountId,
            year = year,
            month = month,
            beginBalance = begin,
            endBalance = end,
            totalIncome = 0.0,
            totalExpense = 0.0,
            transactionCount = 0,
            createdAt = now,
        )

    @Test
    fun upsertAndGetByAccountYearMonth() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 4, begin = 1000.0, end = 1500.0))
        val result = snapshotDao.getByAccountYearMonth(accId, 2026, 4)
        assertNotNull(result)
        assertEquals(1000.0, result!!.beginBalance, 0.001)
        assertEquals(1500.0, result.endBalance, 0.001)
    }

    @Test
    fun upsert_replacesExistingSnapshot() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 4, begin = 1000.0, end = 1500.0))
        snapshotDao.upsert(snapshot(accId, 2026, 4, begin = 1000.0, end = 2000.0))
        val result = snapshotDao.getByAccountYearMonth(accId, 2026, 4)
        assertEquals(2000.0, result!!.endBalance, 0.001)
    }

    @Test
    fun getByAccountYearMonth_returnsNullWhenMissing() = runTest {
        val accId = insertAccount()
        assertNull(snapshotDao.getByAccountYearMonth(accId, 2026, 4))
    }

    @Test
    fun getLatestBefore_returnsImmediatelyPrecedingSnapshot() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 1, end = 100.0))
        snapshotDao.upsert(snapshot(accId, 2026, 2, end = 200.0))
        snapshotDao.upsert(snapshot(accId, 2026, 3, end = 300.0))

        val result = snapshotDao.getLatestBefore(accId, 2026, 3)
        assertNotNull(result)
        assertEquals(2, result!!.month)
        assertEquals(200.0, result.endBalance, 0.001)
    }

    @Test
    fun getLatestBefore_returnsNullWhenNoEarlierSnapshot() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 4))
        assertNull(snapshotDao.getLatestBefore(accId, 2026, 4))
    }

    @Test
    fun getLaterSnapshots_returnsSubsequentMonths() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 1))
        snapshotDao.upsert(snapshot(accId, 2026, 2))
        snapshotDao.upsert(snapshot(accId, 2026, 3))
        snapshotDao.upsert(snapshot(accId, 2026, 4))

        val result = snapshotDao.getLaterSnapshots(accId, 2026, 2)
        assertEquals(2, result.size)
        assertEquals(3, result[0].month)
        assertEquals(4, result[1].month)
    }

    @Test
    fun getAllByYearMonth_returnsAllAccountsForThatMonth() = runTest {
        val acc1 = insertAccount()
        val acc2 = accountDao.insert(AccountEntity(accountName = "Cash", accountType = "Cash", createdAt = now)).toInt()
        snapshotDao.upsert(snapshot(acc1, 2026, 4, end = 1000.0))
        snapshotDao.upsert(snapshot(acc2, 2026, 4, end = 500.0))
        snapshotDao.upsert(snapshot(acc1, 2026, 3))  // different month

        val result = snapshotDao.getAllByYearMonth(2026, 4)
        assertEquals(2, result.size)
    }

    @Test
    fun getAllByYear_returnsSortedMonths() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 12))
        snapshotDao.upsert(snapshot(accId, 2026, 1))
        snapshotDao.upsert(snapshot(accId, 2026, 6))

        val result = snapshotDao.getAllByYear(2026)
        assertEquals(listOf(1, 6, 12), result.map { it.month })
    }

    @Test
    fun deleteAll_removesEverything() = runTest {
        val accId = insertAccount()
        snapshotDao.upsert(snapshot(accId, 2026, 1))
        snapshotDao.upsert(snapshot(accId, 2026, 2))
        snapshotDao.deleteAll()
        assertNull(snapshotDao.getByAccountYearMonth(accId, 2026, 1))
        assertNull(snapshotDao.getByAccountYearMonth(accId, 2026, 2))
    }
}

