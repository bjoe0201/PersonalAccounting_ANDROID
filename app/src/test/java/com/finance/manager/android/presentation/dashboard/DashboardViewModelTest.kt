package com.finance.manager.android.presentation.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.DashboardSummary
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.usecase.dashboard.GetDashboardSummaryUseCase
import com.finance.manager.android.domain.usecase.dashboard.GetRecentTransactionsUseCase
import io.mockk.every
import java.time.LocalDate
import io.mockk.mockk
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getDashboardSummaryUseCase: GetDashboardSummaryUseCase
    private lateinit var getRecentTransactionsUseCase: GetRecentTransactionsUseCase

    private val stubSummary = DashboardSummary(
        netAssets = 80000.0,
        monthIncome = 50000.0,
        monthExpense = -20000.0,
        accounts = listOf(
            Account(1, "銀行帳戶", AccountType.Bank, currentBalance = 80000.0, initialBalance = 50000.0),
        ),
    )

    private val stubTransactions = listOf(
        TransactionListItem(
            transactionId = 1,
            accountId = 1,
            accountName = "銀行帳戶",
            transactionDate = LocalDate.of(2026, 4, 1),
            amount = -1000.0,
            memo = "午餐",
            categoryName = "飲食",
            payeeName = "便當店",
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getDashboardSummaryUseCase = mockk()
        getRecentTransactionsUseCase = mockk()

        every { getDashboardSummaryUseCase() } returns flowOf(stubSummary)
        every { getRecentTransactionsUseCase(any()) } returns flowOf(stubTransactions)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        val viewModel = DashboardViewModel(getDashboardSummaryUseCase, getRecentTransactionsUseCase)
        // Before coroutines run, isLoading should still be true
        // (summary not yet delivered)
        assertEquals(true, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun `state populated after collect`() = runTest {
        val viewModel = DashboardViewModel(getDashboardSummaryUseCase, getRecentTransactionsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(stubSummary, state.summary)
        assertEquals(stubTransactions, state.recentTransactions)
    }

    @Test
    fun `netAssets reflects summary value`() = runTest {
        val viewModel = DashboardViewModel(getDashboardSummaryUseCase, getRecentTransactionsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(80000.0, viewModel.uiState.value.summary?.netAssets ?: 0.0, 0.001)
    }

    @Test
    fun `recentTransactions list is populated`() = runTest {
        val viewModel = DashboardViewModel(getDashboardSummaryUseCase, getRecentTransactionsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recentTransactions.size)
        assertEquals("午餐", viewModel.uiState.value.recentTransactions.first().memo)
    }
}

