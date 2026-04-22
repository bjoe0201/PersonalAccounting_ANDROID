package com.finance.manager.android.domain.usecase.transaction

import androidx.room.withTransaction
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.CurrencyConverter
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import com.finance.manager.android.domain.usecase.snapshot.UpdateSnapshotUseCase
import java.time.LocalDate
import javax.inject.Inject

data class CreateTransferInput(
    val fromAccountId: Int,
    val toAccountId: Int,
    val date: LocalDate,
    val amount: Double,
    val memo: String?,
)

class CreateTransferUseCase @Inject constructor(
    private val db: AppDatabase,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val transactionRepository: TransactionRepository,
    private val updateSnapshotUseCase: UpdateSnapshotUseCase,
) {
    suspend operator fun invoke(input: CreateTransferInput): AppResult<Pair<Int, Int>> {
        if (input.amount <= 0.0) return AppResult.Error("轉帳金額必須大於 0")
        if (input.fromAccountId == input.toAccountId) return AppResult.Error("來源帳戶與目標帳戶不可相同")

        val fromAccount = accountRepository.getById(input.fromAccountId) ?: return AppResult.Error("找不到來源帳戶")
        val toAccount = accountRepository.getById(input.toAccountId) ?: return AppResult.Error("找不到目標帳戶")

        val currencies = currencyRepository.getAll().associateBy { it.currencyId }
        val fromCurrency = fromAccount.currencyId?.let { currencies[it] }
        val toCurrency = toAccount.currencyId?.let { currencies[it] }
        val fromRate = fromCurrency?.rateFromHome ?: 1.0
        val toRate = toCurrency?.rateFromHome ?: 1.0
        val rawTargetAmount = CurrencyConverter.convert(input.amount, fromRate, toRate)
        val targetScale = CurrencyConverter.resolveDecimalPlaces(toCurrency)
        val targetAmount = CurrencyConverter.roundAmount(rawTargetAmount, targetScale)

        var fromTransactionId = 0
        var toTransactionId = 0
        db.withTransaction {
            fromTransactionId = transactionRepository.insert(
                Transaction(
                    accountId = input.fromAccountId,
                    transactionDate = input.date,
                    amount = -input.amount,
                    transferAccountId = input.toAccountId,
                    memo = input.memo,
                )
            ).toInt()
            toTransactionId = transactionRepository.insert(
                Transaction(
                    accountId = input.toAccountId,
                    transactionDate = input.date,
                    amount = targetAmount,
                    transferAccountId = input.fromAccountId,
                    memo = input.memo,
                )
            ).toInt()
            transactionRepository.updateLinkedTransactionId(fromTransactionId, toTransactionId)
            transactionRepository.updateLinkedTransactionId(toTransactionId, fromTransactionId)
            updateSnapshotUseCase(fromAccount, input.date, -input.amount)
            updateSnapshotUseCase(toAccount, input.date, targetAmount)
        }
        return AppResult.Success(fromTransactionId to toTransactionId)
    }
}
