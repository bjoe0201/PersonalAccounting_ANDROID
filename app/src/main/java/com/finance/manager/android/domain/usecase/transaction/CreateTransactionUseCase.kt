package com.finance.manager.android.domain.usecase.transaction

import androidx.room.withTransaction
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.model.TransactionSplitItem
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.PayeeRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import com.finance.manager.android.domain.usecase.snapshot.UpdateSnapshotUseCase
import java.time.LocalDate
import javax.inject.Inject

data class CreateTransactionInput(
    val accountId: Int,
    val date: LocalDate,
    val amount: Double,
    val payeeName: String?,
    val categoryId: Int,
    val memo: String?,
    val splits: List<SplitInput> = emptyList(),
)

data class SplitInput(
    val categoryId: Int,
    val amount: Double,
    val memo: String?,
)

class CreateTransactionUseCase @Inject constructor(
    private val db: AppDatabase,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val payeeRepository: PayeeRepository,
    private val updateSnapshotUseCase: UpdateSnapshotUseCase,
) {
    suspend operator fun invoke(input: CreateTransactionInput): AppResult<Long> {
        if (input.amount == 0.0) return AppResult.Error("金額不可為零")
        if (input.splits.isNotEmpty()) {
            val splitTotal = input.splits.sumOf { it.amount }
            if (kotlin.math.abs(splitTotal - input.amount) > 0.005) {
                return AppResult.Error("分割合計需等於交易金額")
            }
            if (input.splits.any { it.amount == 0.0 }) {
                return AppResult.Error("分割項目金額不可為 0")
            }
        }
        val account = accountRepository.getById(input.accountId) ?: return AppResult.Error("找不到帳戶")
        val payeeId = input.payeeName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
            payeeRepository.findByName(name) ?: payeeRepository.insert(name)
        }

        var transactionId = 0L
        db.withTransaction {
            transactionId = transactionRepository.insert(
                Transaction(
                    accountId = input.accountId,
                    transactionDate = input.date,
                    amount = input.amount,
                    payeeId = payeeId,
                    categoryId = if (input.splits.isEmpty()) input.categoryId else null,
                    memo = input.memo,
                )
            )
            if (input.splits.isNotEmpty()) {
                transactionRepository.replaceSplits(
                    transactionId = transactionId.toInt(),
                    splits = input.splits.map {
                        TransactionSplitItem(
                            transactionId = transactionId.toInt(),
                            categoryId = it.categoryId,
                            amount = it.amount,
                            memo = it.memo,
                        )
                    }
                )
            }
            updateSnapshotUseCase(account, input.date, input.amount)
        }
        return AppResult.Success(transactionId)
    }
}

