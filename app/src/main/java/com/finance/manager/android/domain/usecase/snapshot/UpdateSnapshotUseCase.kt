package com.finance.manager.android.domain.usecase.snapshot

import com.finance.manager.android.data.local.dao.AccountDao
import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.domain.model.Account
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class UpdateSnapshotUseCase @Inject constructor(
    private val accountDao: AccountDao,
    private val snapshotDao: AccountMonthlyBalanceDao,
) {

    suspend operator fun invoke(account: Account, date: LocalDate, delta: Double) {
        val year = date.year
        val month = date.monthValue
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val existing = snapshotDao.getByAccountYearMonth(account.accountId, year, month)

        if (existing == null) {
            val previous = snapshotDao.getLatestBefore(account.accountId, year, month)
            val beginBalance = previous?.endBalance ?: account.initialBalance
            snapshotDao.upsert(
                com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity(
                    accountId = account.accountId,
                    year = year,
                    month = month,
                    beginBalance = beginBalance,
                    endBalance = beginBalance + delta,
                    totalIncome = if (delta > 0) delta else 0.0,
                    totalExpense = if (delta < 0) delta else 0.0,
                    transactionCount = 1,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            snapshotDao.upsert(
                existing.copy(
                    endBalance = existing.endBalance + delta,
                    totalIncome = if (delta > 0) existing.totalIncome + delta else existing.totalIncome,
                    totalExpense = if (delta < 0) existing.totalExpense + delta else existing.totalExpense,
                    transactionCount = existing.transactionCount + 1,
                    updatedAt = now,
                )
            )
        }

        val laterSnapshots = snapshotDao.getLaterSnapshots(account.accountId, year, month)
        if (laterSnapshots.isNotEmpty()) {
            snapshotDao.upsertAll(
                laterSnapshots.map {
                    it.copy(
                        beginBalance = it.beginBalance + delta,
                        endBalance = it.endBalance + delta,
                        updatedAt = now,
                    )
                }
            )
        }

        accountDao.incrementCurrentBalance(account.accountId, delta)
    }
}

