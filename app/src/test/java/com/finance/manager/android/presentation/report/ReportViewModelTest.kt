package com.finance.manager.android.presentation.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.finance.manager.android.domain.model.MonthlyReportSummary
import com.finance.manager.android.domain.model.MonthlySnapshot
import com.finance.manager.android.domain.model.YearlyReport
import com.finance.manager.android.domain.usecase.report.GetMonthlyReportSummaryUseCase
import com.finance.manager.android.domain.usecase.report.GetYearlyReportUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getMonthlyUseCase: GetMonthlyReportSummaryUseCase
    private lateinit var getYearlyUseCase: GetYearlyReportUseCase
    private lateinit var viewModel: ReportViewModel

    private val stubMonthlySummary = MonthlyReportSummary(2026, 1, 10000.0, -5000.0, 5000.0)
    private val stubYearlyReport = YearlyReport(
        year = 2026,
        totalIncome = 120000.0,
        totalExpense = -60000.0,
        monthlyData = listOf(MonthlySnapshot(1, 10000.0, -5000.0, 5000.0)),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getMonthlyUseCase = mockk()
        getYearlyUseCase = mockk()
        coEvery { getMonthlyUseCase(any()) } returns stubMonthlySummary
        coEvery { getYearlyUseCase(any()) } returns stubYearlyReport
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads current month summary`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(stubMonthlySummary, state.monthlySummary)
        assertEquals(YearMonth.now(), state.month)
    }

    @Test
    fun `previousMonth decrements month and reloads`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.month
        viewModel.previousMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialMonth.minusMonths(1), viewModel.uiState.value.month)
    }

    @Test
    fun `nextMonth increments month and reloads`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialMonth = viewModel.uiState.value.month
        viewModel.nextMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialMonth.plusMonths(1), viewModel.uiState.value.month)
    }

    @Test
    fun `selectTab YEARLY loads yearly report`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(ReportTab.YEARLY)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ReportTab.YEARLY, state.activeTab)
        assertEquals(stubYearlyReport, state.yearlyReport)
    }

    @Test
    fun `selectTab TREND uses yearly data`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(ReportTab.TREND)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ReportTab.TREND, viewModel.uiState.value.activeTab)
        assertEquals(stubYearlyReport, viewModel.uiState.value.yearlyReport)
    }

    @Test
    fun `previousYear decrements year`() = runTest {
        viewModel = ReportViewModel(getMonthlyUseCase, getYearlyUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialYear = viewModel.uiState.value.year
        viewModel.previousYear()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialYear - 1, viewModel.uiState.value.year)
    }
}

