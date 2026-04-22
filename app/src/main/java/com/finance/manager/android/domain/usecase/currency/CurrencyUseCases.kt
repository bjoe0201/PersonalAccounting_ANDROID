package com.finance.manager.android.domain.usecase.currency

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCurrenciesUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository,
) {
    operator fun invoke(): Flow<List<Currency>> = currencyRepository.observeActiveCurrencies()
}

class SetHomeCurrencyUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(currencyId: Int): AppResult<Unit> {
        currencyRepository.clearHomeCurrency()
        currencyRepository.setHomeCurrency(currencyId)
        return AppResult.Success(Unit)
    }
}

class UpdateExchangeRateUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository,
) {
    suspend operator fun invoke(currencyId: Int, rateFromHome: Double): AppResult<Unit> {
        if (rateFromHome <= 0) return AppResult.Error("匯率必須大於 0")
        currencyRepository.updateRate(currencyId, rateFromHome)
        return AppResult.Success(Unit)
    }
}

