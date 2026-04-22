package com.finance.manager.android.presentation.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.usecase.currency.GetCurrenciesUseCase
import com.finance.manager.android.domain.usecase.currency.SetHomeCurrencyUseCase
import com.finance.manager.android.domain.usecase.currency.UpdateExchangeRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val getCurrenciesUseCase: GetCurrenciesUseCase,
    private val setHomeCurrencyUseCase: SetHomeCurrencyUseCase,
    private val updateExchangeRateUseCase: UpdateExchangeRateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrenciesUseCase().collect { currencies ->
                _uiState.update { it.copy(currencies = currencies) }
            }
        }
    }

    fun setHomeCurrency(currencyId: Int) {
        viewModelScope.launch {
            when (val result = setHomeCurrencyUseCase(currencyId)) {
                is AppResult.Success -> _uiState.update { it.copy(message = "已設定本位幣") }
                is AppResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun updateRate(currencyId: Int, text: String) {
        val value = text.toDoubleOrNull() ?: return
        viewModelScope.launch {
            when (val result = updateExchangeRateUseCase(currencyId, value)) {
                is AppResult.Success -> _uiState.update { it.copy(message = "匯率已更新") }
                is AppResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class CurrencyUiState(
    val currencies: List<Currency> = emptyList(),
    val message: String? = null,
)

