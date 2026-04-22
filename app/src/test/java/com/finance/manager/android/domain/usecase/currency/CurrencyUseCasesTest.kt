package com.finance.manager.android.domain.usecase.currency

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyUseCasesTest {

    private lateinit var currencyRepository: CurrencyRepository

    private val sampleCurrencies = listOf(
        Currency(1, "TWD", "新台幣", "NT$", 1.0, isHome = true),
        Currency(2, "USD", "美元", "$", 0.033, isHome = false),
    )

    @Before
    fun setUp() {
        currencyRepository = mockk()
        coEvery { currencyRepository.observeActiveCurrencies() } returns flowOf(sampleCurrencies)
        coEvery { currencyRepository.clearHomeCurrency() } returns Unit
        coEvery { currencyRepository.setHomeCurrency(any()) } returns Unit
        coEvery { currencyRepository.updateRate(any(), any()) } returns Unit
    }

    // ─── GetCurrenciesUseCase ─────────────────────────────────────────────────

    @Test
    fun `GetCurrenciesUseCase emits active currencies from repository`() = runTest {
        val useCase = GetCurrenciesUseCase(currencyRepository)
        val result = mutableListOf<List<Currency>>()
        useCase().collect { result.add(it) }

        assertEquals(1, result.size)
        assertEquals(sampleCurrencies, result.first())
    }

    // ─── SetHomeCurrencyUseCase ───────────────────────────────────────────────

    @Test
    fun `SetHomeCurrencyUseCase clears old home then sets new home`() = runTest {
        val useCase = SetHomeCurrencyUseCase(currencyRepository)
        val result = useCase(2)

        assertTrue(result is AppResult.Success)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            currencyRepository.clearHomeCurrency()
            currencyRepository.setHomeCurrency(2)
        }
    }

    // ─── UpdateExchangeRateUseCase ────────────────────────────────────────────

    @Test
    fun `UpdateExchangeRateUseCase returns success for valid positive rate`() = runTest {
        val useCase = UpdateExchangeRateUseCase(currencyRepository)
        val result = useCase(2, 0.032)

        assertTrue(result is AppResult.Success)
        coVerify { currencyRepository.updateRate(2, 0.032) }
    }

    @Test
    fun `UpdateExchangeRateUseCase returns error for zero rate`() = runTest {
        val useCase = UpdateExchangeRateUseCase(currencyRepository)
        val result = useCase(2, 0.0)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { currencyRepository.updateRate(any(), any()) }
    }

    @Test
    fun `UpdateExchangeRateUseCase returns error for negative rate`() = runTest {
        val useCase = UpdateExchangeRateUseCase(currencyRepository)
        val result = useCase(2, -1.0)

        assertTrue(result is AppResult.Error)
    }
}

