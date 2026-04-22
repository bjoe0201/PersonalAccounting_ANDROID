package com.finance.manager.android.presentation.accountregister

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.usecase.account.ObserveAccountUseCase
import com.finance.manager.android.domain.usecase.transaction.DeleteTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.GetAccountTransactionsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRegisterViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeAccountUseCase: ObserveAccountUseCase
    private lateinit var getAccountTransactionsUseCase: GetAccountTransactionsUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    private val stubAccount = Account(
        accountId = 1,
        accountName = "銀行帳戶",
        accountType = AccountType.Bank,
        currentBalance = 5000.0,
        initialBalance = 5000.0,
    )

    private val stubTransactions = listOf(
        TransactionListItem(
            transactionId = 10,
            accountId = 1,
            accountName = "銀行帳戶",
            transactionDate = LocalDate.of(2026, 4, 1),
            amount = -500.0,
            memo = "午餐",
            categoryName = "飲食",
            payeeName = null,
        ),
        TransactionListItem(
            transactionId = 11,
            accountId = 1,
            accountName = "銀行帳戶",
            transactionDate = LocalDate.of(2026, 4, 5),
            amount = 50000.0,
            memo = "薪水",
            categoryName = "薪資",
            payeeName = null,
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observeAccountUseCase = mockk()
        getAccountTransactionsUseCase = mockk()
        deleteTransactionUseCase = mockk()

        every { observeAccountUseCase(1) } returns flowOf(stubAccount)
        every { getAccountTransactionsUseCase(1) } returns flowOf(stubTransactions)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AccountRegisterViewModel(
        observeAccountUseCase,
        getAccountTransactionsUseCase,
        deleteTransactionUseCase,
    )

    @Test
    fun `initial state is loading with no data`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.account)
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
    }

    @Test
    fun `load populates account and transactions`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(stubAccount, state.account)
        assertEquals(stubTransactions, state.transactions)
    }

    @Test
    fun `load shows error message when account not found`() = runTest {
        every { observeAccountUseCase(99) } returns flowOf(null)
        every { getAccountTransactionsUseCase(99) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        viewModel.load(99)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("找不到帳戶", viewModel.uiState.value.message)
    }

    @Test
    fun `load ignores duplicate call for same accountId`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Second call with same ID should be a no-op; state remains
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(stubAccount, viewModel.uiState.value.account)
    }

    @Test
    fun `deleteTransaction on success shows message`() = runTest {
        coEvery { deleteTransactionUseCase(10) } returns AppResult.Success(Unit)

        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTransaction(10)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("交易已刪除", viewModel.uiState.value.message)
    }

    @Test
    fun `deleteTransaction on failure shows error message`() = runTest {
        coEvery { deleteTransactionUseCase(10) } returns AppResult.Error("刪除失敗")

        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTransaction(10)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("刪除失敗", viewModel.uiState.value.message)
    }

    @Test
    fun `clearMessage resets message to null`() = runTest {
        coEvery { deleteTransactionUseCase(10) } returns AppResult.Success(Unit)

        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTransaction(10)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearMessage()

        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `transactions are sorted descending by date`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val transactions = viewModel.uiState.value.transactions
        assertEquals(2, transactions.size)
        // Stub order as returned by use case
        assertEquals(10, transactions.first().transactionId)
    }
}

