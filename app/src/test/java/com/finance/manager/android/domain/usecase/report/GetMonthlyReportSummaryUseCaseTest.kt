package com.finance.manager.android.domain.usecase.report

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.data.local.dao.CategoryAmountLocal
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetMonthlyReportSummaryUseCaseTest {

    private lateinit var snapshotDao: AccountMonthlyBalanceDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountRepository: AccountRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: GetMonthlyReportSummaryUseCase

    private val homeCurrency = Currency(1, "TWD", "新台幣", "NT$", 1.0, isHome = true)
    private val usdCurrency = Currency(2, "USD", "美元", "$", 0.033, isHome = false) // 1 TWD = 0.033 USD

    private val twdAccount = Account(1, "TWD Account", AccountType.Bank, currencyId = 1, currentBalance = 0.0)
    private val usdAccount = Account(2, "USD Account", AccountType.Bank, currencyId = 2, currentBalance = 0.0)

    private val targetMonth = YearMonth.of(2026, 1)

    @Before
    fun setUp() {
        snapshotDao = mockk()
        transactionDao = mockk()
        accountRepository = mockk()
        currencyRepository = mockk()
        useCase = GetMonthlyReportSummaryUseCase(snapshotDao, transactionDao, accountRepository, currencyRepository)

        coEvery { accountRepository.getAll() } returns listOf(twdAccount, usdAccount)
        coEvery { currencyRepository.getAll() } returns listOf(homeCurrency, usdCurrency)
        coEvery { transactionDao.getCategoryExpenseBreakdown(any(), any()) } returns emptyList()
    }

    @Test
    fun `sums home currency snapshots correctly`() = runTest {
        coEvery { snapshotDao.getAllByYearMonth(2026, 1) } returns listOf(
            makeSnapshot(1, income = 10000.0, expense = -3000.0, endBalance = 7000.0)
        )

        val result = useCase(targetMonth)

        assertEquals(10000.0, result.totalIncome, 0.001)
        assertEquals(-3000.0, result.totalExpense, 0.001)
        assertEquals(7000.0, result.endBalance, 0.001)
    }

    @Test
    fun `converts USD snapshot to TWD using rateFromHome`() = runTest {
        // USD account: income=100 USD, rateFromHome=0.033 → toHome = 100/0.033 ≈ 3030.3 TWD
        coEvery { snapshotDao.getAllByYearMonth(2026, 1) } returns listOf(
            makeSnapshot(2, income = 100.0, expense = 0.0, endBalance = 100.0)
        )

        val result = useCase(targetMonth)

        val expected = 100.0 / 0.033
        assertEquals(expected, result.totalIncome, 1.0)
    }

    @Test
    fun `category breakdown percentage calculated against home total`() = runTest {
        coEvery { snapshotDao.getAllByYearMonth(2026, 1) } returns listOf(
            makeSnapshot(1, income = 0.0, expense = -1000.0, endBalance = -1000.0)
        )
        coEvery { transactionDao.getCategoryExpenseBreakdown(any(), any()) } returns listOf(
            CategoryAmountLocal(1, "Food", null, -600.0),
            CategoryAmountLocal(2, "Transport", null, -400.0),
        )

        val result = useCase(targetMonth)

        val food = result.categoryBreakdown.first { it.categoryId == 1 }
        assertEquals(60.0, food.percentage, 0.1)
    }

    @Test
    fun `returns empty breakdown when no expenses`() = runTest {
        coEvery { snapshotDao.getAllByYearMonth(2026, 1) } returns listOf(
            makeSnapshot(1, income = 5000.0, expense = 0.0, endBalance = 5000.0)
        )

        val result = useCase(targetMonth)

        assertEquals(0, result.categoryBreakdown.size)
    }

    private fun makeSnapshot(
        accountId: Int,
        income: Double = 0.0,
        expense: Double = 0.0,
        endBalance: Double = 0.0,
    ) = AccountMonthlyBalanceEntity(
        accountId = accountId,
        year = 2026,
        month = 1,
        beginBalance = 0.0,
        endBalance = endBalance,
        totalIncome = income,
        totalExpense = expense,
        transactionCount = 0,
        createdAt = LocalDateTime.now().toString(),
    )
}

