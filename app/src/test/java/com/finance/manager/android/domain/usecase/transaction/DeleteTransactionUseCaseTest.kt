package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Transaction
import com.finance.manager.android.domain.repository.AccountRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteTransactionUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var updateSnapshotUseCase: UpdateSnapshotUseCase
    private lateinit var useCase: DeleteTransactionUseCase

    private val testDate = LocalDate.of(2026, 1, 15)
    private val account = Account(1, "Bank", AccountType.Bank, currentBalance = 4500.0, initialBalance = 5000.0)
    private val transaction = Transaction(
        transactionId = 10, accountId = 1, transactionDate = testDate,
        amount = -500.0, linkedTransactionId = null,
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        db = mockk()
        @Suppress("UNCHECKED_CAST")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers { (it.invocation.args[1] as suspend () -> Any).invoke() }
        accountRepository = mockk()
        transactionRepository = mockk(relaxed = true)
        updateSnapshotUseCase = mockk(relaxed = true)
        useCase = DeleteTransactionUseCase(db, accountRepository, transactionRepository, updateSnapshotUseCase)

        coEvery { accountRepository.getById(1) } returns account
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `returns error when transaction not found`() = runTest {
        coEvery { transactionRepository.getById(99) } returns null
        val result = useCase(99)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `deletes transaction and reverses snapshot`() = runTest {
        coEvery { transactionRepository.getById(10) } returns transaction
        val result = useCase(10)

        assertTrue(result is AppResult.Success)
        coVerify { transactionRepository.delete(10) }
        // reverse delta: -(-500) = +500
        coVerify { updateSnapshotUseCase(account, testDate, 500.0) }
    }

    @Test
    fun `deletes linked transfer transaction and reverses both snapshots`() = runTest {
        val linkedAccount = Account(2, "Savings", AccountType.Bank, currentBalance = 5500.0, initialBalance = 5000.0)
        val transferTx = transaction.copy(linkedTransactionId = 11)
        val linkedTx = Transaction(
            transactionId = 11, accountId = 2, transactionDate = testDate, amount = 500.0,
        )
        coEvery { transactionRepository.getById(10) } returns transferTx
        coEvery { transactionRepository.getById(11) } returns linkedTx
        coEvery { accountRepository.getById(2) } returns linkedAccount

        useCase(10)

        coVerify { transactionRepository.delete(10) }
        coVerify { transactionRepository.delete(11) }
        coVerify { updateSnapshotUseCase(account, testDate, 500.0) }
        coVerify { updateSnapshotUseCase(linkedAccount, testDate, -500.0) }
    }
}

