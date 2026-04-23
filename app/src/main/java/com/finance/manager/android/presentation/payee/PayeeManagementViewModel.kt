package com.finance.manager.android.presentation.payee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Payee
import com.finance.manager.android.domain.usecase.payee.CreatePayeeUseCase
import com.finance.manager.android.domain.usecase.payee.DeletePayeeUseCase
import com.finance.manager.android.domain.usecase.payee.ObservePayeesUseCase
import com.finance.manager.android.domain.usecase.payee.ToggleHiddenPayeeUseCase
import com.finance.manager.android.domain.usecase.payee.UpdatePayeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PayeeManagementUiState(
    val isLoading: Boolean = true,
    val payees: List<Payee> = emptyList(),
    val searchQuery: String = "",
    val includeHidden: Boolean = false,
    val editing: Payee? = null,
    val creatingNew: Boolean = false,
    val message: String? = null,
) {
    val filtered: List<Payee>
        get() = payees
            .asSequence()
            .filter { includeHidden || !it.isHidden }
            .filter { searchQuery.isBlank() || it.payeeName.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.payeeName }
            .toList()
}

@HiltViewModel
class PayeeManagementViewModel @Inject constructor(
    private val observePayeesUseCase: ObservePayeesUseCase,
    private val createPayeeUseCase: CreatePayeeUseCase,
    private val updatePayeeUseCase: UpdatePayeeUseCase,
    private val toggleHiddenPayeeUseCase: ToggleHiddenPayeeUseCase,
    private val deletePayeeUseCase: DeletePayeeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayeeManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observePayeesUseCase().collect { list ->
                _uiState.update { it.copy(isLoading = false, payees = list) }
            }
        }
    }

    fun updateSearch(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun toggleIncludeHidden() = _uiState.update { it.copy(includeHidden = !it.includeHidden) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun startCreate() = _uiState.update { it.copy(creatingNew = true, editing = null) }
    fun startEdit(payee: Payee) = _uiState.update { it.copy(editing = payee, creatingNew = false) }
    fun dismissEditor() = _uiState.update { it.copy(editing = null, creatingNew = false) }

    fun save(payee: Payee) {
        viewModelScope.launch {
            val result = if (payee.payeeId == 0) createPayeeUseCase(payee) else updatePayeeUseCase(payee)
            val msg = when (result) {
                is AppResult.Success<*> -> { dismissEditor(); "已儲存" }
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }

    fun toggleHidden(payee: Payee) {
        viewModelScope.launch {
            toggleHiddenPayeeUseCase(payee.payeeId, !payee.isHidden)
        }
    }

    fun delete(payee: Payee) {
        viewModelScope.launch {
            val result = deletePayeeUseCase(payee.payeeId)
            val msg = when (result) {
                is AppResult.Success -> "已刪除"
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }
}

