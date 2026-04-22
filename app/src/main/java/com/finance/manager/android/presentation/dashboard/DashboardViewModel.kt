package com.finance.manager.android.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.model.DashboardSummary
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.usecase.dashboard.GetDashboardSummaryUseCase
import com.finance.manager.android.domain.usecase.dashboard.GetRecentTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDashboardSummaryUseCase().collect { summary ->
                _uiState.update { it.copy(isLoading = false, summary = summary) }
            }
        }
        viewModelScope.launch {
            getRecentTransactionsUseCase().collect { transactions ->
                _uiState.update { it.copy(recentTransactions = transactions) }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary? = null,
    val recentTransactions: List<TransactionListItem> = emptyList(),
)

