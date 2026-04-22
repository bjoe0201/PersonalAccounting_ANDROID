package com.finance.manager.android.presentation.accountregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.usecase.account.ObserveAccountUseCase
import com.finance.manager.android.domain.usecase.transaction.DeleteTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.GetAccountTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AccountRegisterViewModel @Inject constructor(
    private val observeAccountUseCase: ObserveAccountUseCase,
    private val getAccountTransactionsUseCase: GetAccountTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountRegisterUiState())
    val uiState = _uiState.asStateFlow()

    private var accountJob: Job? = null
    private var observeJob: Job? = null

    fun load(accountId: Int) {
        if (_uiState.value.account?.accountId == accountId) return

        accountJob?.cancel()
        accountJob = viewModelScope.launch {
            observeAccountUseCase(accountId).collect { account ->
                _uiState.update {
                    it.copy(
                        account = account,
                        isLoading = false,
                        message = if (account == null) "找不到帳戶" else it.message,
                    )
                }
            }
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getAccountTransactionsUseCase(accountId).collect { transactions ->
                _uiState.update { it.copy(transactions = transactions) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase(transactionId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(message = "交易已刪除") }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(message = result.message) }
                }
            }
        }
    }
}

data class AccountRegisterUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val transactions: List<TransactionListItem> = emptyList(),
    val message: String? = null,
)


