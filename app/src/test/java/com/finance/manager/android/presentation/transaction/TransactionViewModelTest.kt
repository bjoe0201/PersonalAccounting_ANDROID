package com.finance.manager.android.presentation.transaction

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.repository.PayeeRepository
import com.finance.manager.android.domain.usecase.account.GetAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountsUseCase
import com.finance.manager.android.domain.usecase.category.GetCategoriesByTypeUseCase
import com.finance.manager.android.domain.usecase.transaction.CreateTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.CreateTransferUseCase
import com.finance.manager.android.domain.usecase.transaction.GetTransactionSplitsUseCase
import com.finance.manager.android.domain.usecase.transaction.GetTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.UpdateTransactionUseCase
import io.mockk.coEvery
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
class TransactionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAccountUseCase: GetAccountUseCase
    private lateinit var getAccountsUseCase: GetAccountsUseCase
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var payeeRepository: PayeeRepository
    private lateinit var getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase
    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var createTransferUseCase: CreateTransferUseCase
    private lateinit var getTransactionUseCase: GetTransactionUseCase
    private lateinit var getTransactionSplitsUseCase: GetTransactionSplitsUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAccountUseCase = mockk()
        getAccountsUseCase = mockk()
        currencyRepository = mockk()
        payeeRepository = mockk()
        getCategoriesByTypeUseCase = mockk()
        createTransactionUseCase = mockk()
        createTransferUseCase = mockk()
        getTransactionUseCase = mockk()
        getTransactionSplitsUseCase = mockk()
        updateTransactionUseCase = mockk()

        coEvery { getCategoriesByTypeUseCase(any()) } returns flowOf(emptyList())
        coEvery { currencyRepository.observeActiveCurrencies() } returns flowOf(
            listOf(
                Currency(1, "TWD", "新台幣", "NT$", 1.0, true),
                Currency(2, "USD", "美元", "$", 0.033, false),
            )
        )
        coEvery { createTransferUseCase(any()) } returns AppResult.Success(1 to 2)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows transfer estimate for cross-currency transfer`() = runTest {
        coEvery { getAccountUseCase(1) } returns Account(1, "TWD", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1)
        coEvery { getAccountsUseCase() } returns flowOf(
            listOf(
                Account(1, "TWD", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1),
                Account(2, "USD", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 2),
            )
        )

        val vm = newVm()
        vm.load(1, null)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateEntryType(EntryType.Transfer)
        vm.updateAmount("3300")
        testDispatcher.scheduler.advanceUntilIdle()

        // 3300 TWD -> USD at 0.033 => 108.9
        assertEquals(108.9, vm.uiState.value.transferEstimateAmount ?: 0.0, 0.001)
        assertEquals("TWD", vm.uiState.value.transferFromCurrencyCode)
        assertEquals("USD", vm.uiState.value.transferToCurrencyCode)
        assertEquals(0.033, vm.uiState.value.transferRate ?: 0.0, 0.000001)
        assertEquals("預估入帳：108.90 USD（輸出幣別：TWD）", vm.uiState.value.transferEstimateText)
        assertEquals("參考匯率：1 TWD = 0.033000 USD", vm.uiState.value.transferRateText)
    }

    @Test
    fun `hides transfer estimate for same currency transfer`() = runTest {
        coEvery { getAccountUseCase(1) } returns Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1)
        coEvery { getAccountsUseCase() } returns flowOf(
            listOf(
                Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1),
                Account(2, "B", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1),
            )
        )

        val vm = newVm()
        vm.load(1, null)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateEntryType(EntryType.Transfer)
        vm.updateAmount("100")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.transferEstimateAmount)
        assertNull(vm.uiState.value.transferRate)
        assertNull(vm.uiState.value.transferEstimateText)
        assertNull(vm.uiState.value.transferRateText)
    }

    @Test
    fun `hides transfer estimate when amount is invalid`() = runTest {
        coEvery { getAccountUseCase(1) } returns Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1)
        coEvery { getAccountsUseCase() } returns flowOf(
            listOf(
                Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1),
                Account(2, "B", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 2),
            )
        )

        val vm = newVm()
        vm.load(1, null)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateEntryType(EntryType.Transfer)
        vm.updateAmount("abc")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.transferEstimateAmount)
        assertNull(vm.uiState.value.transferRate)
        assertNull(vm.uiState.value.transferEstimateText)
        assertNull(vm.uiState.value.transferRateText)
    }

    @Test
    fun `save shows error when selected target account is no longer available`() = runTest {
        coEvery { getAccountUseCase(1) } returns Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1)
        // No available target account after filtering current account
        coEvery { getAccountsUseCase() } returns flowOf(
            listOf(
                Account(1, "A", AccountType.Bank, currentBalance = 0.0, initialBalance = 0.0, currencyId = 1),
            )
        )

        val vm = newVm()
        vm.load(1, null)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateEntryType(EntryType.Transfer)
        vm.updateAmount("100")
        vm.updateTargetAccount(999)
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("轉入帳戶不可用，請重新選擇", vm.uiState.value.message)
    }

    private fun newVm() = TransactionViewModel(
        getAccountUseCase = getAccountUseCase,
        getAccountsUseCase = getAccountsUseCase,
        currencyRepository = currencyRepository,
        payeeRepository = payeeRepository,
        getCategoriesByTypeUseCase = getCategoriesByTypeUseCase,
        createTransactionUseCase = createTransactionUseCase,
        createTransferUseCase = createTransferUseCase,
        getTransactionUseCase = getTransactionUseCase,
        getTransactionSplitsUseCase = getTransactionSplitsUseCase,
        updateTransactionUseCase = updateTransactionUseCase,
    )
}

