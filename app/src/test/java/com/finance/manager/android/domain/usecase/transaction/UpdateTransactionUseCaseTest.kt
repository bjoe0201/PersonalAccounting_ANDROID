package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.PayeeRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import com.finance.manager.android.domain.usecase.snapshot.UpdateSnapshotUseCase
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateTransactionUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var payeeRepository: PayeeRepository
    private lateinit var updateSnapshotUseCase: UpdateSnapshotUseCase
    private lateinit var useCase: UpdateTransactionUseCase

    private val account = Account(
        accountId = 1,
        accountName = "Bank",
        accountType = AccountType.Bank,
        initialBalance = 0.0,
        currentBalance = 1000.0,
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        db = mockk()
        @Suppress("UNCHECKED_CAST")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers { (it.invocation.args[1] as suspend () -> Any).invoke() }
        accountRepository = mockk()
        transactionRepository = mockk(relaxed = true)
        payeeRepository = mockk(relaxed = true)
        updateSnapshotUseCase = mockk(relaxed = true)
        useCase = UpdateTransactionUseCase(db, accountRepository, transactionRepository, payeeRepository, updateSnapshotUseCase)

        coEvery { accountRepository.getById(1) } returns account
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `returns error when transaction not found`() = runTest {
        coEvery { transactionRepository.getById(999) } returns null

        val result = useCase(
            UpdateTransactionInput(
                transactionId = 999,
                date = LocalDate.of(2026, 1, 1),
                amount = -100.0,
                payeeName = null,
                categoryId = 1,
                memo = null,
            )
        )

        assertTrue(result is AppResult.Error)
        assertEquals("找不到交易", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error for transfer transaction edit attempt`() = runTest {
        coEvery { transactionRepository.getById(10) } returns Transaction(
            transactionId = 10,
            accountId = 1,
            transactionDate = LocalDate.of(2026, 1, 1),
            amount = -200.0,
            transferAccountId = 2,
        )

        val result = useCase(
            UpdateTransactionInput(
                transactionId = 10,
                date = LocalDate.of(2026, 1, 2),
                amount = -200.0,
                payeeName = null,
                categoryId = 1,
                memo = null,
            )
        )

        assertTrue(result is AppResult.Error)
        assertEquals("轉帳交易目前不支援編輯，請刪除後重建", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error when split total does not match amount`() = runTest {
        val result = useCase(
            UpdateTransactionInput(
                transactionId = 1,
                date = LocalDate.of(2026, 1, 1),
                amount = -500.0,
                payeeName = null,
                categoryId = 0,
                memo = null,
                splits = listOf(
                    SplitInput(categoryId = 1, amount = -200.0, memo = null),
                    SplitInput(categoryId = 2, amount = -200.0, memo = null),
                )
            )
        )

        assertTrue(result is AppResult.Error)
        assertEquals("分割合計需等於交易金額", (result as AppResult.Error).message)
    }

    @Test
    fun `updates transaction and applies snapshot reverse then new delta`() = runTest {
        val original = Transaction(
            transactionId = 10,
            accountId = 1,
            transactionDate = LocalDate.of(2026, 1, 1),
            amount = -200.0,
            categoryId = 1,
        )
        coEvery { transactionRepository.getById(10) } returns original
        coEvery { payeeRepository.findByName("Shop") } returns 5

        val result = useCase(
            UpdateTransactionInput(
                transactionId = 10,
                date = LocalDate.of(2026, 1, 5),
                amount = -300.0,
                payeeName = "Shop",
                categoryId = 2,
                memo = "new",
            )
        )

        assertTrue(result is AppResult.Success)
        coVerify { transactionRepository.updateGeneral(any()) }
        coVerify { transactionRepository.replaceSplits(10, any()) }
        coVerify { updateSnapshotUseCase(account, LocalDate.of(2026, 1, 1), 200.0) }
        coVerify { updateSnapshotUseCase(account, LocalDate.of(2026, 1, 5), -300.0) }
    }
}

