package com.finance.manager.android.domain.usecase.report

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.domain.model.CurrencyConverter
import com.finance.manager.android.domain.model.MonthlySnapshot
import com.finance.manager.android.domain.model.YearlyReport
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import javax.inject.Inject

class GetYearlyReportUseCase @Inject constructor(
    private val snapshotDao: AccountMonthlyBalanceDao,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(year: Int): YearlyReport {
        val snapshots = snapshotDao.getAllByYear(year)
        val accounts = accountRepository.getAll().associateBy { it.accountId }
        val currencies = currencyRepository.getAll().associateBy { it.currencyId }

        fun rateFor(accountId: Int): Double {
            val currencyId = accounts[accountId]?.currencyId ?: return 1.0
            return currencies[currencyId]?.rateFromHome ?: 1.0
        }

        // Group by month and aggregate, converting to home currency
        val byMonth = snapshots.groupBy { it.month }
        val monthlyData = (1..12).mapNotNull { month ->
            val rows = byMonth[month] ?: return@mapNotNull null
            MonthlySnapshot(
                month = month,
                income = rows.sumOf { CurrencyConverter.toHomeCurrency(it.totalIncome, rateFor(it.accountId)) },
                expense = rows.sumOf { CurrencyConverter.toHomeCurrency(it.totalExpense, rateFor(it.accountId)) },
                balance = rows.sumOf { CurrencyConverter.toHomeCurrency(it.endBalance, rateFor(it.accountId)) },
            )
        }

        return YearlyReport(
            year = year,
            totalIncome = monthlyData.sumOf { it.income },
            totalExpense = monthlyData.sumOf { it.expense },
            monthlyData = monthlyData,
        )
    }
}
