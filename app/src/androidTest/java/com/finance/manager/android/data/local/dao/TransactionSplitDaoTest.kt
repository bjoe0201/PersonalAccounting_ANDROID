package com.finance.manager.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.TransactionEntity
import com.finance.manager.android.data.local.entity.TransactionSplitEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionSplitDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var splitDao: TransactionSplitDao
    private lateinit var txDao: TransactionDao
    private lateinit var accountDao: AccountDao

    private val now = "2026-01-01T00:00:00"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        splitDao = db.transactionSplitDao()
        txDao = db.transactionDao()
        accountDao = db.accountDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun prepareTransaction(): Int {
        val accId = accountDao.insert(AccountEntity(accountName = "Bank", accountType = "Bank", createdAt = now)).toInt()
        return txDao.insert(TransactionEntity(accountId = accId, transactionDate = "2026-04-01", amount = -500.0, createdAt = now)).toInt()
    }

    @Test
    fun insertAndGetByTransactionId() = runTest {
        val txId = prepareTransaction()
        splitDao.insert(TransactionSplitEntity(transactionId = txId, categoryId = 1, amount = -300.0, createdAt = now))
        splitDao.insert(TransactionSplitEntity(transactionId = txId, categoryId = 2, amount = -200.0, createdAt = now))

        val splits = splitDao.getByTransactionId(txId)
        assertEquals(2, splits.size)
        assertEquals(-500.0, splits.sumOf { it.amount }, 0.001)
    }

    @Test
    fun getByTransactionId_returnsEmptyForNoSplits() = runTest {
        val txId = prepareTransaction()
        val splits = splitDao.getByTransactionId(txId)
        assertTrue(splits.isEmpty())
    }

    @Test
    fun deleteByTransactionId_removesAllSplits() = runTest {
        val txId = prepareTransaction()
        splitDao.insert(TransactionSplitEntity(transactionId = txId, categoryId = 1, amount = -300.0, createdAt = now))
        splitDao.insert(TransactionSplitEntity(transactionId = txId, categoryId = 2, amount = -200.0, createdAt = now))

        splitDao.deleteByTransactionId(txId)
        assertTrue(splitDao.getByTransactionId(txId).isEmpty())
    }

    @Test
    fun insertAll_insertsMultipleSplits() = runTest {
        val txId = prepareTransaction()
        splitDao.insertAll(
            listOf(
                TransactionSplitEntity(transactionId = txId, categoryId = 1, amount = -100.0, createdAt = now),
                TransactionSplitEntity(transactionId = txId, categoryId = 2, amount = -150.0, createdAt = now),
                TransactionSplitEntity(transactionId = txId, categoryId = 3, amount = -250.0, createdAt = now),
            )
        )
        assertEquals(3, splitDao.getByTransactionId(txId).size)
    }
}

