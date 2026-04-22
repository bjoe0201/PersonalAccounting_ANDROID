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
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTransactionUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var payeeRepository: PayeeRepository
    private lateinit var updateSnapshotUseCase: UpdateSnapshotUseCase
    private lateinit var useCase: CreateTransactionUseCase

    private val testAccount = Account(
        accountId = 1, accountName = "Bank", accountType = AccountType.Bank,
        initialBalance = 0.0, currentBalance = 5000.0,
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
        useCase = CreateTransactionUseCase(db, accountRepository, transactionRepository, payeeRepository, updateSnapshotUseCase)

        accountRepository = mockk()
        transactionRepository = mockk(relaxed = true)
        payeeRepository = mockk(relaxed = true)
        updateSnapshotUseCase = mockk(relaxed = true)
        useCase = CreateTransactionUseCase(db, accountRepository, transactionRepository, payeeRepository, updateSnapshotUseCase)

        coEvery { accountRepository.getById(1) } returns testAccount
        coEvery { transactionRepository.insert(any()) } returns 10L
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `returns error when amount is zero`() = runTest {
        val input = CreateTransactionInput(1, LocalDate.now(), 0.0, null, 1, null)
        val result = useCase(input)
        assertTrue(result is AppResult.Error)
        assertEquals("金額不可為零", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error when account not found`() = runTest {
        coEvery { accountRepository.getById(99) } returns null
        val input = CreateTransactionInput(99, LocalDate.now(), -500.0, null, 1, null)
        val result = useCase(input)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `returns error when split total does not match amount`() = runTest {
        val splits = listOf(SplitInput(1, -200.0, null), SplitInput(2, -200.0, null))
        val input = CreateTransactionInput(1, LocalDate.now(), -500.0, null, 0, null, splits)
        val result = useCase(input)
        assertTrue(result is AppResult.Error)
        assertEquals("分割合計需等於交易金額", (result as AppResult.Error).message)
    }

    @Test
    fun `creates transaction successfully and returns id`() = runTest {
        val input = CreateTransactionInput(1, LocalDate.of(2026, 1, 15), -500.0, "Shop", 1, "memo")
        val result = useCase(input)
        assertTrue(result is AppResult.Success)
        assertEquals(10L, (result as AppResult.Success).data)
    }

    @Test
    fun `snapshot is updated with transaction amount`() = runTest {
        val date = LocalDate.of(2026, 1, 15)
        val input = CreateTransactionInput(1, date, -300.0, null, 1, null)
        useCase(input)
        coVerify { updateSnapshotUseCase(testAccount, date, -300.0) }
    }

    @Test
    fun `auto-creates payee when payeeName provided`() = runTest {
        coEvery { payeeRepository.findByName("Starbucks") } returns null
        coEvery { payeeRepository.insert("Starbucks") } returns 5
        val input = CreateTransactionInput(1, LocalDate.now(), -150.0, "Starbucks", 1, null)
        useCase(input)
        coVerify { payeeRepository.insert("Starbucks") }
    }

    @Test
    fun `reuses existing payee when payeeName matches`() = runTest {
        coEvery { payeeRepository.findByName("7-Eleven") } returns 3
        val slot = slot<Transaction>()
        coEvery { transactionRepository.insert(capture(slot)) } returns 11L
        val input = CreateTransactionInput(1, LocalDate.now(), -50.0, "7-Eleven", 1, null)
        useCase(input)
        assertEquals(3, slot.captured.payeeId)
    }
}

