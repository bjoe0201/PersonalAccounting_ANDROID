package com.finance.manager.android.domain.usecase.dashboard

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.CurrencyConverter
import com.finance.manager.android.domain.model.DashboardSummary
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetDashboardSummaryUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val snapshotDao: AccountMonthlyBalanceDao,
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val currentMonth = YearMonth.now()
        return accountRepository.observeAccounts().map { accounts ->
            val currencies = currencyRepository.getAll().associateBy { it.currencyId }
            val visibleAccounts = accounts.filterNot(Account::isHidden)
            val monthSnapshots = snapshotDao.getAllByYearMonth(currentMonth.year, currentMonth.monthValue)
                .filter { snapshot -> visibleAccounts.any { it.accountId == snapshot.accountId } }

            // Convert each account balance and snapshot values to home currency
            val netAssets = visibleAccounts.sumOf { account ->
                val rate = currencies[account.currencyId]?.rateFromHome ?: 1.0
                CurrencyConverter.toHomeCurrency(account.currentBalance, rate)
            }
            val monthIncome = monthSnapshots.sumOf { snap ->
                val account = visibleAccounts.firstOrNull { it.accountId == snap.accountId }
                val rate = currencies[account?.currencyId]?.rateFromHome ?: 1.0
                CurrencyConverter.toHomeCurrency(snap.totalIncome, rate)
            }
            val monthExpense = monthSnapshots.sumOf { snap ->
                val account = visibleAccounts.firstOrNull { it.accountId == snap.accountId }
                val rate = currencies[account?.currencyId]?.rateFromHome ?: 1.0
                CurrencyConverter.toHomeCurrency(snap.totalExpense, rate)
            }

            DashboardSummary(
                netAssets = netAssets,
                monthIncome = monthIncome,
                monthExpense = monthExpense,
                accounts = visibleAccounts,
            )
        }
    }
}

class GetRecentTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(limit: Int = 10): Flow<List<TransactionListItem>> = transactionRepository.observeRecent(limit)
}
