package com.finance.manager.android.domain.model

data class Currency(
    val currencyId: Int,
    val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val rateFromHome: Double,
    val isHome: Boolean,
    val decimalPlaces: Int = 0,
)

