package com.finance.manager.android.presentation.account

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.usecase.account.CreateAccountUseCase
import com.finance.manager.android.domain.usecase.account.DeleteAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountsUseCase
import com.finance.manager.android.domain.usecase.account.UpdateAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAccountsUseCase: GetAccountsUseCase
    private lateinit var getAccountUseCase: GetAccountUseCase
    private lateinit var createAccountUseCase: CreateAccountUseCase
    private lateinit var updateAccountUseCase: UpdateAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var viewModel: AccountViewModel

    private val homeCurrency = Currency(1, "TWD", "新台幣", "NT$", 1.0, isHome = true)
    private val sampleAccounts = listOf(
        Account(1, "Bank", AccountType.Bank, currentBalance = 5000.0, initialBalance = 5000.0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAccountsUseCase = mockk()
        getAccountUseCase = mockk()
        createAccountUseCase = mockk()
        updateAccountUseCase = mockk()
        deleteAccountUseCase = mockk()
        currencyRepository = mockk()

        coEvery { getAccountsUseCase() } returns flowOf(sampleAccounts)
        coEvery { currencyRepository.observeActiveCurrencies() } returns flowOf(listOf(homeCurrency))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AccountViewModel(
        getAccountsUseCase, getAccountUseCase, createAccountUseCase,
        updateAccountUseCase, deleteAccountUseCase, currencyRepository,
    )

    @Test
    fun `initial state loads accounts and currencies`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleAccounts, state.accounts)
        assertEquals(listOf(homeCurrency), state.currencies)
    }

    @Test
    fun `prepareNewAccount sets home currency as default`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.prepareNewAccount()

        assertEquals(1, viewModel.uiState.value.formState.currencyId)
    }

    @Test
    fun `saveAccount calls createAccountUseCase and navigates back on success`() = runTest {
        coEvery { createAccountUseCase(any(), any(), any(), any(), any()) } returns AppResult.Success(42L)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAccountName("New Account")
        viewModel.updateInitialBalance("1000")
        viewModel.saveAccount()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(42, viewModel.uiState.value.savedAccountId)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `saveAccount shows error message on failure`() = runTest {
        coEvery { createAccountUseCase(any(), any(), any(), any(), any()) } returns AppResult.Error("名稱重複")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAccountName("Dupe")
        viewModel.updateInitialBalance("0")
        viewModel.saveAccount()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("名稱重複", viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.savedAccountId)
    }

    @Test
    fun `saveAccount shows error when initial balance is not a number`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateInitialBalance("abc")
        viewModel.saveAccount()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("初始餘額格式不正確", viewModel.uiState.value.message)
    }

    @Test
    fun `deleteAccount calls use case and shows success message`() = runTest {
        coEvery { deleteAccountUseCase(1) } returns AppResult.Success(Unit)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteAccount(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("帳戶已刪除", viewModel.uiState.value.message)
    }

    @Test
    fun `clearMessage resets message to null`() = runTest {
        coEvery { deleteAccountUseCase(1) } returns AppResult.Success(Unit)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteAccount(1)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearMessage()

        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `updateCurrency updates formState currencyId`() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCurrency(3)

        assertEquals(3, viewModel.uiState.value.formState.currencyId)
    }
}

