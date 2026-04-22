package com.finance.manager.android.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun `toHomeCurrency - home currency rate 1 returns same amount`() {
        assertEquals(1000.0, CurrencyConverter.toHomeCurrency(1000.0, 1.0), 0.001)
    }

    @Test
    fun `toHomeCurrency - non-1 rate converts correctly`() {
        assertEquals(200.0, CurrencyConverter.toHomeCurrency(100.0, 0.5), 0.001)
    }

    @Test
    fun `toHomeCurrency - zero rate returns original amount`() {
        assertEquals(500.0, CurrencyConverter.toHomeCurrency(500.0, 0.0), 0.001)
    }

    @Test
    fun `fromHomeCurrency - converts home to foreign correctly`() {
        assertEquals(100.0, CurrencyConverter.fromHomeCurrency(200.0, 0.5), 0.001)
    }

    @Test
    fun `convert - cross currency A to B`() {
        assertEquals(400.0, CurrencyConverter.convert(100.0, 0.5, 2.0), 0.001)
    }

    @Test
    fun `convert - same rate returns same amount`() {
        assertEquals(100.0, CurrencyConverter.convert(100.0, 1.0, 1.0), 0.001)
    }

    @Test
    fun `roundAmount - uses HALF_UP`() {
        assertEquals(2.35, CurrencyConverter.roundAmount(2.345, 2), 0.000001)
        assertEquals(2.34, CurrencyConverter.roundAmount(2.344, 2), 0.000001)
    }

    @Test
    fun `resolveDecimalPlaces - uses code defaults when decimalPlaces is not set`() {
        val twd = Currency(1, "TWD", "新台幣", "NT$", 1.0, true)
        val usd = Currency(2, "USD", "美元", "$", 0.033, false)
        assertEquals(0, CurrencyConverter.resolveDecimalPlaces(twd))
        assertEquals(2, CurrencyConverter.resolveDecimalPlaces(usd))
    }

    @Test
    fun `formatDeterministic - output is locale safe`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1234.50", CurrencyConverter.formatDeterministic(1234.5, 2))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `format - home currency with 0 decimal places`() {
        val currency = Currency(1, "TWD", "新台幣", "NT$", 1.0, true, 0)
        assertEquals("NT$1000", CurrencyConverter.format(1000.0, currency))
    }

    @Test
    fun `format - foreign currency with 2 decimal places`() {
        val currency = Currency(2, "USD", "美元", "$", 0.033, false, 2)
        assertEquals("$100.00", CurrencyConverter.format(100.0, currency))
    }
}
