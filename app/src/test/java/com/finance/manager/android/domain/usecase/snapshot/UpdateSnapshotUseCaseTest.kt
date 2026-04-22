package com.finance.manager.android.domain.usecase.snapshot

import com.finance.manager.android.data.local.dao.AccountDao
import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UpdateSnapshotUseCaseTest {

    private lateinit var accountDao: AccountDao
    private lateinit var snapshotDao: AccountMonthlyBalanceDao
    private lateinit var useCase: UpdateSnapshotUseCase

    private val testAccount = Account(
        accountId = 1,
        accountName = "Test Bank",
        accountType = AccountType.Bank,
        initialBalance = 1000.0,
        currentBalance = 1000.0,
    )

    @Before
    fun setUp() {
        accountDao = mockk(relaxed = true)
        snapshotDao = mockk(relaxed = true)
        useCase = UpdateSnapshotUseCase(accountDao, snapshotDao)
    }

    @Test
    fun `creates new snapshot when none exists for month`() = runTest {
        val date = LocalDate.of(2026, 1, 15)
        coEvery { snapshotDao.getByAccountYearMonth(1, 2026, 1) } returns null
        coEvery { snapshotDao.getLatestBefore(1, 2026, 1) } returns null
        coEvery { snapshotDao.getLaterSnapshots(1, 2026, 1) } returns emptyList()

        useCase(testAccount, date, 500.0)

        val slot = slot<AccountMonthlyBalanceEntity>()
        coVerify { snapshotDao.upsert(capture(slot)) }
        val snapshot = slot.captured
        assertEquals(1, snapshot.accountId)
        assertEquals(2026, snapshot.year)
        assertEquals(1, snapshot.month)
        assertEquals(1000.0, snapshot.beginBalance, 0.001)
        assertEquals(1500.0, snapshot.endBalance, 0.001)
        assertEquals(500.0, snapshot.totalIncome, 0.001)
        assertEquals(0.0, snapshot.totalExpense, 0.001)
    }

    @Test
    fun `creates new snapshot using previous month end balance as begin`() = runTest {
        val date = LocalDate.of(2026, 2, 10)
        val previousSnapshot = makeSnapshot(accountId = 1, year = 2026, month = 1, endBalance = 1500.0)
        coEvery { snapshotDao.getByAccountYearMonth(1, 2026, 2) } returns null
        coEvery { snapshotDao.getLatestBefore(1, 2026, 2) } returns previousSnapshot
        coEvery { snapshotDao.getLaterSnapshots(1, 2026, 2) } returns emptyList()

        useCase(testAccount, date, -200.0)

        val slot = slot<AccountMonthlyBalanceEntity>()
        coVerify { snapshotDao.upsert(capture(slot)) }
        val snapshot = slot.captured
        assertEquals(1500.0, snapshot.beginBalance, 0.001)
        assertEquals(1300.0, snapshot.endBalance, 0.001)
        assertEquals(-200.0, snapshot.totalExpense, 0.001)
    }

    @Test
    fun `updates existing snapshot and propagates delta to later months`() = runTest {
        val date = LocalDate.of(2026, 1, 15)
        val existing = makeSnapshot(accountId = 1, year = 2026, month = 1, endBalance = 1500.0, income = 500.0)
        val later = makeSnapshot(accountId = 1, year = 2026, month = 2, beginBalance = 1500.0, endBalance = 1800.0)
        coEvery { snapshotDao.getByAccountYearMonth(1, 2026, 1) } returns existing
        coEvery { snapshotDao.getLaterSnapshots(1, 2026, 1) } returns listOf(later)

        useCase(testAccount, date, 100.0)

        val slot = slot<List<AccountMonthlyBalanceEntity>>()
        coVerify { snapshotDao.upsertAll(capture(slot)) }
        val propagated = slot.captured.first()
        assertEquals(1600.0, propagated.beginBalance, 0.001)
        assertEquals(1900.0, propagated.endBalance, 0.001)
    }

    @Test
    fun `increments account balance after snapshot update`() = runTest {
        val date = LocalDate.of(2026, 1, 15)
        coEvery { snapshotDao.getByAccountYearMonth(1, 2026, 1) } returns null
        coEvery { snapshotDao.getLatestBefore(1, 2026, 1) } returns null
        coEvery { snapshotDao.getLaterSnapshots(1, 2026, 1) } returns emptyList()

        useCase(testAccount, date, -300.0)

        coVerify { accountDao.incrementCurrentBalance(1, -300.0) }
    }

    private fun makeSnapshot(
        accountId: Int,
        year: Int,
        month: Int,
        beginBalance: Double = 0.0,
        endBalance: Double = 0.0,
        income: Double = 0.0,
        expense: Double = 0.0,
    ) = AccountMonthlyBalanceEntity(
        accountId = accountId,
        year = year,
        month = month,
        beginBalance = beginBalance,
        endBalance = endBalance,
        totalIncome = income,
        totalExpense = expense,
        transactionCount = 0,
        createdAt = LocalDateTime.now().toString(),
    )
}

