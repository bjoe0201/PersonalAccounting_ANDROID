package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.CurrencyDao
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao,
) : CurrencyRepository {
    override fun observeActiveCurrencies(): Flow<List<Currency>> =
        currencyDao.observeActiveCurrencies().map { entities -> entities.map(::toCurrency) }

    override suspend fun getAll(): List<Currency> =
        currencyDao.getAll().map(::toCurrency)

    override suspend fun clearHomeCurrency() {
        currencyDao.clearHomeCurrency()
    }

    override suspend fun setHomeCurrency(currencyId: Int) {
        currencyDao.setHomeCurrency(currencyId)
    }

    override suspend fun updateRate(currencyId: Int, rateFromHome: Double) {
        currencyDao.updateRate(
            currencyId = currencyId,
            rateFromHome = rateFromHome,
            updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
    }
}

private fun toCurrency(entity: com.finance.manager.android.data.local.entity.CurrencyEntity) = Currency(
    currencyId = entity.currencyId,
    currencyCode = entity.currencyCode,
    currencyName = entity.currencyName,
    symbol = entity.currencySymbol,
    rateFromHome = entity.rateFromHome,
    isHome = entity.isHomeCurrency,
)

