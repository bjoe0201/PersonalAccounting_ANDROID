package com.finance.manager.android.domain.usecase.transaction

import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
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

class CreateTransferUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var updateSnapshotUseCase: UpdateSnapshotUseCase
    private lateinit var useCase: CreateTransferUseCase

    private val fromAccount = Account(
        1,
        "Checking",
        AccountType.Bank,
        currentBalance = 5000.0,
        initialBalance = 0.0,
        currencyId = 1,
    )
    private val toAccount = Account(
        2,
        "Savings",
        AccountType.Bank,
        currentBalance = 2000.0,
        initialBalance = 0.0,
        currencyId = 1,
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        db = mockk()
        @Suppress("UNCHECKED_CAST")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers { (it.invocation.args[1] as suspend () -> Any).invoke() }
        accountRepository = mockk()
        currencyRepository = mockk()
        transactionRepository = mockk(relaxed = true)
        updateSnapshotUseCase = mockk(relaxed = true)
        useCase = CreateTransferUseCase(db, accountRepository, currencyRepository, transactionRepository, updateSnapshotUseCase)

        coEvery { accountRepository.getById(1) } returns fromAccount
        coEvery { accountRepository.getById(2) } returns toAccount
        coEvery { currencyRepository.getAll() } returns listOf(
            Currency(1, "TWD", "新台幣", "NT$", 1.0, true),
        )
        coEvery { transactionRepository.insert(any()) } returnsMany listOf(100L, 101L)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `returns error when amount is zero or negative`() = runTest {
        val input = CreateTransferInput(1, 2, LocalDate.now(), 0.0, null)
        assertTrue(useCase(input) is AppResult.Error)

        val negInput = CreateTransferInput(1, 2, LocalDate.now(), -100.0, null)
        assertTrue(useCase(negInput) is AppResult.Error)
    }

    @Test
    fun `returns error when from and to accounts are the same`() = runTest {
        val input = CreateTransferInput(1, 1, LocalDate.now(), 500.0, null)
        val result = useCase(input)
        assertTrue(result is AppResult.Error)
        assertEquals("來源帳戶與目標帳戶不可相同", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error when from account not found`() = runTest {
        coEvery { accountRepository.getById(99) } returns null
        val input = CreateTransferInput(99, 2, LocalDate.now(), 500.0, null)
        assertTrue(useCase(input) is AppResult.Error)
    }

    @Test
    fun `creates two linked transactions`() = runTest {
        val date = LocalDate.of(2026, 1, 20)
        val input = CreateTransferInput(1, 2, date, 1000.0, "Transfer")
        val result = useCase(input)

        assertTrue(result is AppResult.Success)
        val (fromId, toId) = (result as AppResult.Success).data
        assertEquals(100, fromId)
        assertEquals(101, toId)
        coVerify { transactionRepository.updateLinkedTransactionId(100, 101) }
        coVerify { transactionRepository.updateLinkedTransactionId(101, 100) }
    }

    @Test
    fun `updates snapshots for both accounts with correct amounts`() = runTest {
        val date = LocalDate.of(2026, 1, 20)
        val input = CreateTransferInput(1, 2, date, 500.0, null)
        useCase(input)

        coVerify { updateSnapshotUseCase(fromAccount, date, -500.0) }
        coVerify { updateSnapshotUseCase(toAccount, date, 500.0) }
    }

    @Test
    fun `converts transfer amount when account currencies differ`() = runTest {
        val usdToTwd = Account(
            accountId = 2,
            accountName = "USD Savings",
            accountType = AccountType.Bank,
            initialBalance = 0.0,
            currentBalance = 0.0,
            currencyId = 2,
        )
        coEvery { accountRepository.getById(2) } returns usdToTwd
        coEvery { currencyRepository.getAll() } returns listOf(
            Currency(1, "TWD", "新台幣", "NT$", 1.0, true),
            Currency(2, "USD", "美元", "$", 0.033, false),
        )

        val date = LocalDate.of(2026, 1, 20)
        val input = CreateTransferInput(1, 2, date, 3300.0, null)
        useCase(input)

        // 3300 TWD -> USD (rateFromHome USD=0.033) => 108.9 USD
        coVerify { updateSnapshotUseCase(usdToTwd, date, match<Double> { kotlin.math.abs(it - 108.9) < 0.0001 }) }
    }

    @Test
    fun `rounds converted target amount deterministically`() = runTest {
        val eurAccount = Account(
            accountId = 2,
            accountName = "EUR Savings",
            accountType = AccountType.Bank,
            initialBalance = 0.0,
            currentBalance = 0.0,
            currencyId = 3,
        )
        coEvery { accountRepository.getById(2) } returns eurAccount
        coEvery { currencyRepository.getAll() } returns listOf(
            Currency(1, "TWD", "新台幣", "NT$", 1.0, true),
            Currency(3, "EUR", "歐元", "€", 0.333333, false),
        )

        val date = LocalDate.of(2026, 1, 20)
        val input = CreateTransferInput(1, 2, date, 1.0, null)
        useCase(input)

        // 1.0 * 0.333333 => 0.333333, with 2 decimal places (HALF_UP) => 0.33
        coVerify { updateSnapshotUseCase(eurAccount, date, match<Double> { kotlin.math.abs(it - 0.33) < 0.0001 }) }
    }
}
