package com.finance.manager.android.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.model.MonthlyReportSummary
import com.finance.manager.android.domain.model.YearlyReport
import com.finance.manager.android.domain.usecase.report.GetMonthlyReportSummaryUseCase
import com.finance.manager.android.domain.usecase.report.GetYearlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReportTab { MONTHLY, YEARLY, TREND }

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getMonthlyReportSummaryUseCase: GetMonthlyReportSummaryUseCase,
    private val getYearlyReportUseCase: GetYearlyReportUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMonthly(YearMonth.now())
    }

    fun selectTab(tab: ReportTab) {
        _uiState.update { it.copy(activeTab = tab) }
        when (tab) {
            ReportTab.MONTHLY -> loadMonthly(_uiState.value.month)
            ReportTab.YEARLY, ReportTab.TREND -> loadYearly(_uiState.value.year)
        }
    }

    fun loadMonthly(target: YearMonth) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, month = target) }
            val summary = getMonthlyReportSummaryUseCase(target)
            _uiState.update { it.copy(isLoading = false, monthlySummary = summary) }
        }
    }

    fun loadYearly(year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, year = year) }
            val report = getYearlyReportUseCase(year)
            _uiState.update { it.copy(isLoading = false, yearlyReport = report) }
        }
    }

    fun previousMonth() = loadMonthly(_uiState.value.month.minusMonths(1))
    fun nextMonth() = loadMonthly(_uiState.value.month.plusMonths(1))
    fun previousYear() = loadYearly(_uiState.value.year - 1)
    fun nextYear() = loadYearly(_uiState.value.year + 1)
}

data class ReportUiState(
    val isLoading: Boolean = true,
    val activeTab: ReportTab = ReportTab.MONTHLY,
    val month: YearMonth = YearMonth.now(),
    val year: Int = YearMonth.now().year,
    val monthlySummary: MonthlyReportSummary? = null,
    val yearlyReport: YearlyReport? = null,
)
