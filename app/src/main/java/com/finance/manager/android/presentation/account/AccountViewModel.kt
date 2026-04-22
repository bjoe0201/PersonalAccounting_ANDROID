package com.finance.manager.android.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.usecase.account.CreateAccountUseCase
import com.finance.manager.android.domain.usecase.account.DeleteAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountsUseCase
import com.finance.manager.android.domain.usecase.account.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAccountUseCase: GetAccountUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        observeAccounts()
        observeCurrencies()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                _uiState.update { it.copy(isLoading = false, accounts = accounts) }
            }
        }
    }

    private fun observeCurrencies() {
        viewModelScope.launch {
            currencyRepository.observeActiveCurrencies().collect { currencies ->
                _uiState.update { it.copy(currencies = currencies) }
            }
        }
    }

    fun prepareNewAccount() {
        val homeCurrency = _uiState.value.currencies.firstOrNull { it.isHome }
        _uiState.update {
            it.copy(
                formState = AccountFormState(currencyId = homeCurrency?.currencyId),
                savedAccountId = null,
            )
        }
    }

    fun loadAccount(accountId: Int) {
        if (_uiState.value.formState.accountId == accountId) return
        viewModelScope.launch {
            val account = getAccountUseCase(accountId)
            if (account == null) {
                _uiState.update { it.copy(message = "找不到帳戶資料") }
                return@launch
            }
            _uiState.update { it.copy(formState = account.toFormState(), savedAccountId = null) }
        }
    }

    fun updateAccountName(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(accountName = value)) }
    }

    fun updateAccountType(value: AccountType) {
        _uiState.update { it.copy(formState = it.formState.copy(accountType = value)) }
    }

    fun updateInitialBalance(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(initialBalance = value)) }
    }

    fun updateHidden(value: Boolean) {
        _uiState.update { it.copy(formState = it.formState.copy(isHidden = value)) }
    }

    fun updateCurrency(currencyId: Int?) {
        _uiState.update { it.copy(formState = it.formState.copy(currencyId = currencyId)) }
    }

    fun saveAccount() {
        val formState = _uiState.value.formState
        val initialBalance = formState.initialBalance.toDoubleOrNull()
        if (initialBalance == null) {
            _uiState.update { it.copy(message = "初始餘額格式不正確") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (formState.accountId == null) {
                when (val result = createAccountUseCase(
                    accountName = formState.accountName,
                    accountType = formState.accountType,
                    initialBalance = initialBalance,
                    isHidden = formState.isHidden,
                    currencyId = formState.currencyId,
                )) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(isSaving = false, savedAccountId = result.data.toInt(), message = null)
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(isSaving = false, message = result.message)
                    }
                }
            } else {
                when (val result = updateAccountUseCase(
                    accountId = formState.accountId,
                    accountName = formState.accountName,
                    accountType = formState.accountType,
                    initialBalance = initialBalance,
                    isHidden = formState.isHidden,
                    currencyId = formState.currencyId,
                )) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(isSaving = false, savedAccountId = formState.accountId, message = null)
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(isSaving = false, message = result.message)
                    }
                }
            }
        }
    }

    fun deleteAccount(accountId: Int) {
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(accountId)) {
                is AppResult.Success -> _uiState.update { it.copy(message = "帳戶已刪除") }
                is AppResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(savedAccountId = null) }
    }
}

data class AccountUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val formState: AccountFormState = AccountFormState(),
    val message: String? = null,
    val savedAccountId: Int? = null,
)

data class AccountFormState(
    val accountId: Int? = null,
    val accountName: String = "",
    val accountType: AccountType = AccountType.Bank,
    val initialBalance: String = "0.0",
    val isHidden: Boolean = false,
    val currencyId: Int? = null,
)

private fun Account.toFormState(): AccountFormState = AccountFormState(
    accountId = accountId,
    accountName = accountName,
    accountType = accountType,
    initialBalance = initialBalance.toString(),
    isHidden = isHidden,
    currencyId = currencyId,
)

