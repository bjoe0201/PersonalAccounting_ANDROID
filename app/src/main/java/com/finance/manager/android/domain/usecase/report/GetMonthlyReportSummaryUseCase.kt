package com.finance.manager.android.domain.usecase.report

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.domain.model.CategoryAmount
import com.finance.manager.android.domain.model.CurrencyConverter
import com.finance.manager.android.domain.model.MonthlyReportSummary
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlyReportSummaryUseCase @Inject constructor(
    private val snapshotDao: AccountMonthlyBalanceDao,
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(targetMonth: YearMonth): MonthlyReportSummary {
        val snapshots = snapshotDao.getAllByYearMonth(targetMonth.year, targetMonth.monthValue)
        val startDate = LocalDate.of(targetMonth.year, targetMonth.monthValue, 1).toString()
        val endDate = LocalDate.of(targetMonth.year, targetMonth.monthValue, 1)
            .plusMonths(1).minusDays(1).toString()

        val accounts = accountRepository.getAll().associateBy { it.accountId }
        val currencies = currencyRepository.getAll().associateBy { it.currencyId }

        fun rateFor(accountId: Int): Double {
            val currencyId = accounts[accountId]?.currencyId ?: return 1.0
            return currencies[currencyId]?.rateFromHome ?: 1.0
        }

        val totalIncome = snapshots.sumOf { CurrencyConverter.toHomeCurrency(it.totalIncome, rateFor(it.accountId)) }
        val totalExpense = snapshots.sumOf { CurrencyConverter.toHomeCurrency(it.totalExpense, rateFor(it.accountId)) }
        val endBalance = snapshots.sumOf { CurrencyConverter.toHomeCurrency(it.endBalance, rateFor(it.accountId)) }

        val rawBreakdown = transactionDao.getCategoryExpenseBreakdown(startDate, endDate)
        val categoryBreakdown = rawBreakdown.map { row ->
            CategoryAmount(
                categoryId = row.categoryId,
                categoryName = row.categoryName,
                parentCategoryName = row.parentCategoryName,
                amount = row.totalAmount,
                percentage = if (totalExpense != 0.0) (row.totalAmount / totalExpense) * 100 else 0.0,
            )
        }

        return MonthlyReportSummary(
            year = targetMonth.year,
            month = targetMonth.monthValue,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            endBalance = endBalance,
            categoryBreakdown = categoryBreakdown,
        )
    }
}
