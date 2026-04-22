package com.finance.manager.android.domain.repository

import com.finance.manager.android.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun observeActiveCurrencies(): Flow<List<Currency>>
    suspend fun getAll(): List<Currency>
    suspend fun clearHomeCurrency()
    suspend fun setHomeCurrency(currencyId: Int)
    suspend fun updateRate(currencyId: Int, rateFromHome: Double)
}

