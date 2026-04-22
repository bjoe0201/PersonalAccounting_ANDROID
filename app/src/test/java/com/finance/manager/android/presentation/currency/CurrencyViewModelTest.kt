package com.finance.manager.android.presentation.currency

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.usecase.currency.GetCurrenciesUseCase
import com.finance.manager.android.domain.usecase.currency.SetHomeCurrencyUseCase
import com.finance.manager.android.domain.usecase.currency.UpdateExchangeRateUseCase
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
class CurrencyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrenciesUseCase: GetCurrenciesUseCase
    private lateinit var setHomeCurrencyUseCase: SetHomeCurrencyUseCase
    private lateinit var updateExchangeRateUseCase: UpdateExchangeRateUseCase

    private val currencies = listOf(
        Currency(1, "TWD", "新台幣", "NT$", 1.0, isHome = true),
        Currency(2, "USD", "美元", "$", 0.033, isHome = false),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrenciesUseCase = mockk()
        setHomeCurrencyUseCase = mockk()
        updateExchangeRateUseCase = mockk()

        coEvery { getCurrenciesUseCase() } returns flowOf(currencies)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CurrencyViewModel(
        getCurrenciesUseCase,
        setHomeCurrencyUseCase,
        updateExchangeRateUseCase,
    )

    @Test
    fun `initial state loads currencies`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(currencies, viewModel.uiState.value.currencies)
    }

    @Test
    fun `setHomeCurrency on success updates message`() = runTest {
        coEvery { setHomeCurrencyUseCase(1) } returns AppResult.Success(Unit)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setHomeCurrency(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("已設定本位幣", viewModel.uiState.value.message)
    }

    @Test
    fun `setHomeCurrency on error shows error message`() = runTest {
        coEvery { setHomeCurrencyUseCase(99) } returns AppResult.Error("找不到幣別")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setHomeCurrency(99)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("找不到幣別", viewModel.uiState.value.message)
    }

    @Test
    fun `updateRate with valid number calls use case`() = runTest {
        coEvery { updateExchangeRateUseCase(2, 0.032) } returns AppResult.Success(Unit)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateRate(2, "0.032")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { updateExchangeRateUseCase(2, 0.032) }
        assertEquals("匯率已更新", viewModel.uiState.value.message)
    }

    @Test
    fun `updateRate with invalid text does nothing`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateRate(2, "abc")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateExchangeRateUseCase(any(), any()) }
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `clearMessage resets message to null`() = runTest {
        coEvery { setHomeCurrencyUseCase(1) } returns AppResult.Success(Unit)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setHomeCurrency(1)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearMessage()

        assertNull(viewModel.uiState.value.message)
    }
}

