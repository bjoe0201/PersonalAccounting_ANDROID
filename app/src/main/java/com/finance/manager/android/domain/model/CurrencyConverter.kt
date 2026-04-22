package com.finance.manager.android.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object CurrencyConverter {

    /**
     * Convert a foreign currency amount to home currency.
     * @param amount Foreign currency amount
     * @param rateFromHome Exchange rate (home = 1.0, foreign = X)
     */
    fun toHomeCurrency(amount: Double, rateFromHome: Double): Double {
        if (rateFromHome == 0.0) return amount
        return amount / rateFromHome
    }

    /**
     * Convert a home currency amount to foreign currency.
     */
    fun fromHomeCurrency(homeAmount: Double, rateFromHome: Double): Double = homeAmount * rateFromHome

    /**
     * Cross-currency conversion (A → B via home currency).
     */
    fun convert(amount: Double, rateA: Double, rateB: Double): Double =
        fromHomeCurrency(toHomeCurrency(amount, rateA), rateB)

    /**
     * Resolve decimal places for a currency.
     * If `decimalPlaces` is explicitly set (> 0), use it;
     * otherwise use common defaults by code.
     */
    fun resolveDecimalPlaces(currency: Currency?): Int {
        if (currency == null) return 2
        if (currency.decimalPlaces > 0) return currency.decimalPlaces
        return when (currency.currencyCode.uppercase(Locale.US)) {
            "TWD", "JPY" -> 0
            else -> 2
        }
    }

    /**
     * Round with a deterministic mode.
     */
    fun roundAmount(
        amount: Double,
        scale: Int,
        roundingMode: RoundingMode = RoundingMode.HALF_UP,
    ): Double = BigDecimal.valueOf(amount).setScale(scale.coerceAtLeast(0), roundingMode).toDouble()

    /**
     * Locale-safe deterministic numeric formatting.
     */
    fun formatDeterministic(amount: Double, scale: Int): String =
        String.format(Locale.US, "%.${scale.coerceAtLeast(0)}f", roundAmount(amount, scale))

    /**
     * Format amount with currency symbol, respecting decimal places.
     */
    fun format(amount: Double, currency: Currency): String {
        val dp = resolveDecimalPlaces(currency)
        return "${currency.symbol}${formatDeterministic(amount, dp)}"
    }
}

