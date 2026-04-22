package com.finance.manager.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.CurrencyEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrencyDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CurrencyDao

    private val now = "2026-01-01T00:00:00"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.currencyDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertCurrencies() {
        dao.insertAll(
            listOf(
                CurrencyEntity(currencyName = "新台幣", currencySymbol = "NT$", currencyCode = "TWD", isHomeCurrency = true, rateFromHome = 1.0, displayOrder = 1, createdAt = now),
                CurrencyEntity(currencyName = "美元", currencySymbol = "$", currencyCode = "USD", isHomeCurrency = false, rateFromHome = 0.033, displayOrder = 2, createdAt = now),
                CurrencyEntity(currencyName = "日圓", currencySymbol = "¥", currencyCode = "JPY", isHomeCurrency = false, rateFromHome = 4.5, displayOrder = 3, createdAt = now),
            )
        )
    }

    @Test
    fun insertAll_andGetAll() = runTest {
        insertCurrencies()
        val all = dao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun getCount_returnsCorrectCount() = runTest {
        assertEquals(0, dao.getCount())
        insertCurrencies()
        assertEquals(3, dao.getCount())
    }

    @Test
    fun observeActiveCurrencies_excludesHidden() = runTest {
        dao.insertAll(
            listOf(
                CurrencyEntity(currencyName = "TWD", currencySymbol = "NT$", currencyCode = "TWD", isHomeCurrency = true, createdAt = now, isHidden = false),
                CurrencyEntity(currencyName = "USD", currencySymbol = "$", currencyCode = "USD", isHomeCurrency = false, createdAt = now, isHidden = true),
            )
        )
        val active = dao.observeActiveCurrencies().first()
        assertEquals(1, active.size)
        assertEquals("TWD", active.first().currencyCode)
    }

    @Test
    fun clearHomeCurrency_setsAllToFalse() = runTest {
        insertCurrencies()
        dao.clearHomeCurrency()
        val all = dao.getAll()
        assertTrue(all.none { it.isHomeCurrency })
    }

    @Test
    fun setHomeCurrency_updatesTarget() = runTest {
        insertCurrencies()
        val usdId = dao.getAll().first { it.currencyCode == "USD" }.currencyId
        dao.setHomeCurrency(usdId)
        val updated = dao.getAll().first { it.currencyCode == "USD" }
        assertTrue(updated.isHomeCurrency)
        assertEquals(1.0, updated.rateFromHome, 0.0001)
        assertEquals(1.0, updated.rateToHome, 0.0001)
    }

    @Test
    fun updateRate_changesRateFromHome() = runTest {
        insertCurrencies()
        val usdId = dao.getAll().first { it.currencyCode == "USD" }.currencyId
        dao.updateRate(usdId, 0.031, now)
        val updated = dao.getAll().first { it.currencyCode == "USD" }
        assertEquals(0.031, updated.rateFromHome, 0.00001)
    }
}

