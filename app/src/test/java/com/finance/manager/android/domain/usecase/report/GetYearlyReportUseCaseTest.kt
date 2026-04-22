package com.finance.manager.android.domain.usecase.report

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetYearlyReportUseCaseTest {

    private lateinit var snapshotDao: AccountMonthlyBalanceDao
    private lateinit var accountRepository: AccountRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: GetYearlyReportUseCase

    private val homeCurrency = Currency(1, "TWD", "新台幣", "NT$", 1.0, isHome = true)
    private val account = Account(1, "Main", AccountType.Bank, currencyId = 1, currentBalance = 0.0)

    @Before
    fun setUp() {
        snapshotDao = mockk()
        accountRepository = mockk()
        currencyRepository = mockk()
        useCase = GetYearlyReportUseCase(snapshotDao, accountRepository, currencyRepository)

        coEvery { accountRepository.getAll() } returns listOf(account)
        coEvery { currencyRepository.getAll() } returns listOf(homeCurrency)
    }

    @Test
    fun `aggregates monthly snapshots for the year`() = runTest {
        coEvery { snapshotDao.getAllByYear(2026) } returns listOf(
            makeSnapshot(month = 1, income = 10000.0, expense = -3000.0, endBalance = 7000.0),
            makeSnapshot(month = 2, income = 8000.0, expense = -2500.0, endBalance = 12500.0),
        )

        val result = useCase(2026)

        assertEquals(2026, result.year)
        assertEquals(18000.0, result.totalIncome, 0.001)
        assertEquals(-5500.0, result.totalExpense, 0.001)
        assertEquals(2, result.monthlyData.size)
    }

    @Test
    fun `returns empty data for year with no snapshots`() = runTest {
        coEvery { snapshotDao.getAllByYear(2020) } returns emptyList()

        val result = useCase(2020)

        assertEquals(0.0, result.totalIncome, 0.001)
        assertEquals(0, result.monthlyData.size)
    }

    @Test
    fun `only includes months that have snapshots`() = runTest {
        coEvery { snapshotDao.getAllByYear(2026) } returns listOf(
            makeSnapshot(month = 3, income = 5000.0, expense = 0.0, endBalance = 5000.0),
        )

        val result = useCase(2026)

        assertEquals(1, result.monthlyData.size)
        assertEquals(3, result.monthlyData.first().month)
    }

    @Test
    fun `converts foreign currency snapshots to home currency`() = runTest {
        val usdCurrency = Currency(2, "USD", "美元", "$", 0.033, isHome = false)
        val usdAccount = Account(2, "USD Account", AccountType.Bank, currencyId = 2, currentBalance = 0.0)
        coEvery { accountRepository.getAll() } returns listOf(account, usdAccount)
        coEvery { currencyRepository.getAll() } returns listOf(homeCurrency, usdCurrency)
        coEvery { snapshotDao.getAllByYear(2026) } returns listOf(
            makeSnapshot(accountId = 2, month = 1, income = 330.0, expense = 0.0, endBalance = 330.0),
        )

        val result = useCase(2026)

        // 330 USD / 0.033 = 10000 TWD
        assertEquals(10000.0, result.totalIncome, 1.0)
    }

    private fun makeSnapshot(
        accountId: Int = 1,
        month: Int,
        income: Double = 0.0,
        expense: Double = 0.0,
        endBalance: Double = 0.0,
    ) = AccountMonthlyBalanceEntity(
        accountId = accountId,
        year = 2026,
        month = month,
        beginBalance = 0.0,
        endBalance = endBalance,
        totalIncome = income,
        totalExpense = expense,
        transactionCount = 0,
        createdAt = LocalDateTime.now().toString(),
    )
}

