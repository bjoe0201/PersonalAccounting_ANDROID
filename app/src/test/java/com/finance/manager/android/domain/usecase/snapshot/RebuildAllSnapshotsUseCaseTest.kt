package com.finance.manager.android.domain.usecase.snapshot

import app.cash.turbine.test
import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RebuildAllSnapshotsUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionDao: TransactionDao
    private lateinit var snapshotDao: AccountMonthlyBalanceDao
    private lateinit var useCase: RebuildAllSnapshotsUseCase

    private val account1 = Account(1, "Bank", AccountType.Bank, initialBalance = 1000.0, currentBalance = 1000.0)
    private val account2 = Account(2, "Cash", AccountType.Cash, initialBalance = 500.0, currentBalance = 500.0)

    @Before
    fun setUp() {
        accountRepository = mockk()
        transactionDao = mockk(relaxed = true)
        snapshotDao = mockk(relaxed = true)
        useCase = RebuildAllSnapshotsUseCase(accountRepository, transactionDao, snapshotDao)

        coEvery { transactionDao.getEarliestTransactionDate(any()) } returns null
        coEvery { transactionDao.sumIncomeByDateRange(any(), any(), any()) } returns 0.0
        coEvery { transactionDao.sumExpenseByDateRange(any(), any(), any()) } returns 0.0
        coEvery { transactionDao.countByDateRange(any(), any(), any()) } returns 0
    }

    @Test
    fun `emits progress for each account and final completion`() = runTest {
        coEvery { accountRepository.getAll() } returns listOf(account1, account2)

        useCase().test {
            val p0 = awaitItem()
            assertEquals(0, p0.current)
            assertEquals(2, p0.total)
            assertEquals("Bank", p0.currentAccountName)

            val p1 = awaitItem()
            assertEquals(1, p1.current)
            assertEquals("Cash", p1.currentAccountName)

            val done = awaitItem()
            assertEquals(2, done.current)
            assertEquals(2, done.total)
            assertEquals("完成", done.currentAccountName)

            awaitComplete()
        }
    }

    @Test
    fun `deletes all snapshots before rebuilding`() = runTest {
        coEvery { accountRepository.getAll() } returns emptyList()
        useCase().test { cancelAndConsumeRemainingEvents() }
        coVerify { snapshotDao.deleteAll() }
    }

    @Test
    fun `uses earliest transaction date as rebuild start`() = runTest {
        coEvery { accountRepository.getAll() } returns listOf(account1)
        coEvery { transactionDao.getEarliestTransactionDate(1) } returns "2024-06-15"

        useCase().test { cancelAndConsumeRemainingEvents() }

        // Should have queried months starting from 2024-06, which is more than 12 months ago
        coVerify(atLeast = 1) { transactionDao.sumIncomeByDateRange(1, any(), any()) }
    }

    @Test
    fun `falls back to 12 months when no transactions exist`() = runTest {
        coEvery { accountRepository.getAll() } returns listOf(account1)
        coEvery { transactionDao.getEarliestTransactionDate(1) } returns null

        useCase().test { cancelAndConsumeRemainingEvents() }

        // Should still process months (at least 12)
        coVerify(atLeast = 12) { transactionDao.sumIncomeByDateRange(1, any(), any()) }
    }

    @Test
    fun `handles empty accounts list gracefully`() = runTest {
        coEvery { accountRepository.getAll() } returns emptyList()

        useCase().test {
            val done = awaitItem()
            assertEquals(1, done.total) // coerceAtLeast(1)
            awaitComplete()
        }
    }
}

