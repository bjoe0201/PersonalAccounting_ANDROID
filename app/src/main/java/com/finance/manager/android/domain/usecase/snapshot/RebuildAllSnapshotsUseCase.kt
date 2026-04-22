package com.finance.manager.android.domain.usecase.snapshot

import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.repository.AccountRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class RebuildProgress(
    val current: Int,
    val total: Int,
    val currentAccountName: String,
)

class RebuildAllSnapshotsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionDao: TransactionDao,
    private val snapshotDao: AccountMonthlyBalanceDao,
) {
    operator fun invoke(): Flow<RebuildProgress> = flow {
        val accounts = accountRepository.getAll()
        snapshotDao.deleteAll()
        val total = accounts.size.coerceAtLeast(1)
        accounts.forEachIndexed { index, account ->
            emit(RebuildProgress(index, total, account.accountName))
            rebuildAccount(account)
        }
        emit(RebuildProgress(total, total, "完成"))
    }

    private suspend fun rebuildAccount(account: Account) {
        // Find the earliest transaction date; fall back to 12 months ago if none exist
        val earliestDateStr = transactionDao.getEarliestTransactionDate(account.accountId)
        val start = if (earliestDateStr != null) {
            runCatching { YearMonth.from(LocalDate.parse(earliestDateStr)) }
                .getOrDefault(YearMonth.now().minusMonths(12))
        } else {
            YearMonth.now().minusMonths(12)
        }

        var cursor = start
        val end = YearMonth.now()
        var runningBalance = account.initialBalance
        while (!cursor.isAfter(end)) {
            val startDate = cursor.atDay(1).toString()
            val endDate = cursor.atEndOfMonth().toString()
            val income = transactionDao.sumIncomeByDateRange(account.accountId, startDate, endDate)
            val expense = transactionDao.sumExpenseByDateRange(account.accountId, startDate, endDate)
            val count = transactionDao.countByDateRange(account.accountId, startDate, endDate)
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            snapshotDao.upsert(
                com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity(
                    accountId = account.accountId,
                    year = cursor.year,
                    month = cursor.monthValue,
                    beginBalance = runningBalance,
                    endBalance = runningBalance + income + expense,
                    totalIncome = income,
                    totalExpense = expense,
                    transactionCount = count,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            runningBalance += income + expense
            cursor = cursor.plusMonths(1)
        }
    }
}
