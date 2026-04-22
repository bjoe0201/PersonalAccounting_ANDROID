package com.finance.manager.android.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.model.Currency
import com.finance.manager.android.domain.model.CurrencyConverter
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.repository.PayeeRepository
import com.finance.manager.android.domain.usecase.account.GetAccountUseCase
import com.finance.manager.android.domain.usecase.account.GetAccountsUseCase
import com.finance.manager.android.domain.usecase.category.GetCategoriesByTypeUseCase
import com.finance.manager.android.domain.usecase.transaction.CreateTransactionInput
import com.finance.manager.android.domain.usecase.transaction.SplitInput
import com.finance.manager.android.domain.usecase.transaction.CreateTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.CreateTransferInput
import com.finance.manager.android.domain.usecase.transaction.CreateTransferUseCase
import com.finance.manager.android.domain.usecase.transaction.GetTransactionUseCase
import com.finance.manager.android.domain.usecase.transaction.GetTransactionSplitsUseCase
import com.finance.manager.android.domain.usecase.transaction.UpdateTransactionInput
import com.finance.manager.android.domain.usecase.transaction.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getAccountUseCase: GetAccountUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val currencyRepository: CurrencyRepository,
    private val payeeRepository: PayeeRepository,
    private val getCategoriesByTypeUseCase: GetCategoriesByTypeUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val createTransferUseCase: CreateTransferUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val getTransactionSplitsUseCase: GetTransactionSplitsUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState = _uiState.asStateFlow()

    private var accountJob: Job? = null
    private var categoryJob: Job? = null

    init {
        observeCurrencies()
    }

    fun load(accountId: Int, transactionId: Int?) {
        if (_uiState.value.account?.accountId == accountId && _uiState.value.form.transactionId == transactionId) return

        _uiState.update {
            it.copy(
                form = TransactionFormState(transactionId = transactionId),
                entryType = EntryType.Expense,
                message = null,
            )
        }

        viewModelScope.launch {
            val account = getAccountUseCase(accountId)
            _uiState.update {
                it.copy(
                    account = account,
                    message = if (account == null) "找不到帳戶" else null,
                )
            }
            refreshTransferEstimate()
        }
        observeAccounts(accountId)
        if (transactionId == null) {
            observeCategories(_uiState.value.entryType.categoryType)
        } else {
            loadTransaction(transactionId)
        }
    }

    fun updateDate(value: String) {
        _uiState.update { it.copy(form = it.form.copy(date = value)) }
    }

    fun updatePayee(value: String) {
        _uiState.update { it.copy(form = it.form.copy(payeeName = value)) }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(form = it.form.copy(amount = value)) }
        refreshTransferEstimate()
    }

    fun updateMemo(value: String) {
        _uiState.update { it.copy(form = it.form.copy(memo = value)) }
    }

    fun updateEntryType(entryType: EntryType) {
        _uiState.update {
            it.copy(
                entryType = entryType,
                form = it.form.copy(
                    categoryId = if (entryType == EntryType.Transfer) null else it.form.categoryId,
                    targetAccountId = if (entryType == EntryType.Transfer) it.form.targetAccountId else null,
                ),
            )
        }
        if (entryType == EntryType.Transfer) {
            categoryJob?.cancel()
            _uiState.update { it.copy(categories = emptyList()) }
        } else {
            observeCategories(entryType.categoryType)
        }
        refreshTransferEstimate()
    }

    fun updateCategory(categoryId: Int) {
        _uiState.update { it.copy(form = it.form.copy(categoryId = categoryId)) }
    }

    fun updateSplitEnabled(enabled: Boolean) {
        _uiState.update {
            val items = if (enabled && it.form.splitItems.isEmpty()) {
                listOf(newSplitItem(it.categories.firstOrNull()?.categoryId))
            } else if (!enabled) {
                emptyList()
            } else {
                it.form.splitItems
            }
            it.copy(
                form = it.form.copy(
                    splitEnabled = enabled,
                    splitItems = items,
                    categoryId = if (enabled) null else it.form.categoryId ?: it.categories.firstOrNull()?.categoryId,
                )
            )
        }
    }

    fun addSplitItem() {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    splitItems = it.form.splitItems + newSplitItem(it.categories.firstOrNull()?.categoryId)
                )
            )
        }
    }

    fun removeSplitItem(itemId: Long) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    splitItems = it.form.splitItems.filterNot { split -> split.id == itemId }
                )
            )
        }
    }

    fun updateSplitCategory(itemId: Long, categoryId: Int) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    splitItems = it.form.splitItems.map { split ->
                        if (split.id == itemId) split.copy(categoryId = categoryId) else split
                    }
                )
            )
        }
    }

    fun updateSplitAmount(itemId: Long, amount: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    splitItems = it.form.splitItems.map { split ->
                        if (split.id == itemId) split.copy(amount = amount) else split
                    }
                )
            )
        }
    }

    fun updateSplitMemo(itemId: Long, memo: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(
                    splitItems = it.form.splitItems.map { split ->
                        if (split.id == itemId) split.copy(memo = memo) else split
                    }
                )
            )
        }
    }

    fun updateTargetAccount(targetAccountId: Int) {
        _uiState.update { it.copy(form = it.form.copy(targetAccountId = targetAccountId)) }
        refreshTransferEstimate()
    }

    fun save() {
        val state = _uiState.value
        val account = state.account ?: run {
            _uiState.update { it.copy(message = "找不到帳戶") }
            return
        }
        if (state.form.transactionId != null && state.entryType == EntryType.Transfer) {
            _uiState.update { it.copy(message = "編輯模式不支援改成轉帳") }
            return
        }
        val date = state.form.date.toLocalDateOrNull() ?: run {
            _uiState.update { it.copy(message = "日期格式請使用 yyyy-MM-dd") }
            return
        }
        val amountValue = state.form.amount.toDoubleOrNull()?.takeIf { it > 0 } ?: run {
            _uiState.update { it.copy(message = "請輸入大於 0 的金額") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = when (state.entryType) {
                EntryType.Transfer -> {
                    val targetAccountId = state.form.targetAccountId ?: run {
                        _uiState.update { it.copy(isSaving = false, message = "請選擇轉入帳戶") }
                        return@launch
                    }
                    if (state.availableAccounts.none { it.accountId == targetAccountId }) {
                        _uiState.update { it.copy(isSaving = false, message = "轉入帳戶不可用，請重新選擇") }
                        return@launch
                    }
                    createTransferUseCase(
                        CreateTransferInput(
                            fromAccountId = account.accountId,
                            toAccountId = targetAccountId,
                            date = date,
                            amount = amountValue,
                            memo = state.form.memo,
                        )
                    )
                }
                else -> {
                    val signedAmount = if (state.entryType == EntryType.Expense) -amountValue else amountValue
                    val splitInputs = if (state.form.splitEnabled) {
                        if (state.form.splitItems.isEmpty()) {
                            _uiState.update { it.copy(isSaving = false, message = "請至少新增一筆分割項目") }
                            return@launch
                        }
                        val parsed = mutableListOf<SplitInput>()
                        for (split in state.form.splitItems) {
                            val parsedAmount = split.amount.toDoubleOrNull()?.takeIf { it > 0 }
                            if (parsedAmount == null) {
                                _uiState.update { it.copy(isSaving = false, message = "分割項目金額需大於 0") }
                                return@launch
                            }
                            val categoryId = split.categoryId
                            if (categoryId == null) {
                                _uiState.update { it.copy(isSaving = false, message = "分割項目需選擇分類") }
                                return@launch
                            }
                            val signedSplitAmount = if (state.entryType == EntryType.Expense) -parsedAmount else parsedAmount
                            parsed += SplitInput(
                                categoryId = categoryId,
                                amount = signedSplitAmount,
                                memo = split.memo.ifBlank { null },
                            )
                        }
                        parsed
                    } else {
                        emptyList()
                    }

                    val categoryId = if (!state.form.splitEnabled) {
                        state.form.categoryId ?: run {
                            _uiState.update { it.copy(isSaving = false, message = "請選擇分類") }
                            return@launch
                        }
                    } else {
                        0
                    }

                    if (state.form.transactionId == null) {
                        createTransactionUseCase(
                            CreateTransactionInput(
                                accountId = account.accountId,
                                date = date,
                                amount = signedAmount,
                                payeeName = state.form.payeeName,
                                categoryId = categoryId,
                                memo = state.form.memo,
                                splits = splitInputs,
                            )
                        )
                    } else {
                        updateTransactionUseCase(
                            UpdateTransactionInput(
                                transactionId = state.form.transactionId,
                                date = date,
                                amount = signedAmount,
                                payeeName = state.form.payeeName,
                                categoryId = categoryId,
                                memo = state.form.memo,
                                splits = splitInputs,
                            )
                        )
                    }
                }
            }
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedTransactionId = when (val data = result.data) {
                            is Long -> data.toInt()
                            is Pair<*, *> -> (data.first as? Int) ?: 0
                            is Unit -> state.form.transactionId ?: 0
                            else -> 0
                        },
                        message = null,
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = result.message,
                    )
                }
            }
        }
    }

    private fun loadTransaction(transactionId: Int) {
        viewModelScope.launch {
            val transaction = getTransactionUseCase(transactionId)
            if (transaction == null) {
                _uiState.update { it.copy(message = "找不到交易資料") }
                observeCategories(EntryType.Expense.categoryType)
                return@launch
            }

            if (transaction.transferAccountId != null) {
                _uiState.update {
                    it.copy(
                        message = "轉帳交易目前不支援編輯，請刪除後重建",
                        form = TransactionFormState(transactionId = transactionId),
                    )
                }
                return@launch
            }

            val entryType = if (transaction.amount < 0) EntryType.Expense else EntryType.Income
            val payeeName = transaction.payeeId?.let { payeeRepository.getNameById(it) }.orEmpty()
            val splits = getTransactionSplitsUseCase(transactionId)
            _uiState.update {
                it.copy(
                    entryType = entryType,
                    form = it.form.copy(
                        transactionId = transactionId,
                        date = transaction.transactionDate.toString(),
                        payeeName = payeeName,
                        amount = kotlin.math.abs(transaction.amount).toString(),
                        categoryId = if (splits.isEmpty()) transaction.categoryId else null,
                        splitEnabled = splits.isNotEmpty(),
                        splitItems = splits.map { split ->
                            SplitFormItem(
                                id = split.splitId.toLong().takeIf { it > 0 } ?: java.util.concurrent.atomic.AtomicLong(System.nanoTime()).incrementAndGet(),
                                categoryId = split.categoryId,
                                amount = kotlin.math.abs(split.amount).toString(),
                                memo = split.memo.orEmpty(),
                            )
                        },
                        memo = transaction.memo.orEmpty(),
                        targetAccountId = null,
                    ),
                )
            }
            observeCategories(entryType.categoryType)
        }
    }

    private fun observeAccounts(currentAccountId: Int) {
        accountJob?.cancel()
        accountJob = viewModelScope.launch {
            getAccountsUseCase().collect { accounts ->
                val targets = accounts.filter { it.accountId != currentAccountId }
                _uiState.update {
                    it.copy(
                        availableAccounts = targets,
                        form = it.form.copy(
                            targetAccountId = it.form.targetAccountId?.takeIf { id -> targets.any { account -> account.accountId == id } }
                                ?: targets.firstOrNull()?.accountId
                        ),
                    )
                }
                refreshTransferEstimate()
            }
        }
    }

    private fun observeCurrencies() {
        viewModelScope.launch {
            currencyRepository.observeActiveCurrencies().collect { currencies ->
                _uiState.update { it.copy(currencies = currencies) }
                refreshTransferEstimate()
            }
        }
    }

    private fun refreshTransferEstimate() {
        _uiState.update { state ->
            if (state.entryType != EntryType.Transfer) {
                return@update state.copy(
                    transferEstimateAmount = null,
                    transferFromCurrencyCode = null,
                    transferToCurrencyCode = null,
                    transferRate = null,
                    transferEstimateText = null,
                    transferRateText = null,
                )
            }

            val amount = state.form.amount.toDoubleOrNull()?.takeIf { it > 0 }
            val source = state.account
            val target = state.availableAccounts.firstOrNull { it.accountId == state.form.targetAccountId }
            if (amount == null || source == null || target == null) {
                return@update state.copy(
                    transferEstimateAmount = null,
                    transferFromCurrencyCode = null,
                    transferToCurrencyCode = null,
                    transferRate = null,
                    transferEstimateText = null,
                    transferRateText = null,
                )
            }

            if (source.currencyId == target.currencyId) {
                return@update state.copy(
                    transferEstimateAmount = null,
                    transferFromCurrencyCode = null,
                    transferToCurrencyCode = null,
                    transferRate = null,
                    transferEstimateText = null,
                    transferRateText = null,
                )
            }

            val currenciesById = state.currencies.associateBy(Currency::currencyId)
            val fromCurrency = source.currencyId?.let(currenciesById::get)
            val toCurrency = target.currencyId?.let(currenciesById::get)
            val fromRate = fromCurrency?.rateFromHome ?: 1.0
            val toRate = toCurrency?.rateFromHome ?: 1.0
            val estimateScale = CurrencyConverter.resolveDecimalPlaces(toCurrency)
            val estimate = CurrencyConverter.roundAmount(
                CurrencyConverter.convert(amount, fromRate, toRate),
                estimateScale,
            )
            val unitRate = CurrencyConverter.roundAmount(
                CurrencyConverter.convert(1.0, fromRate, toRate),
                6,
            )
            val fromCode = fromCurrency?.currencyCode
            val toCode = toCurrency?.currencyCode
            val estimateText = "預估入帳：${CurrencyConverter.formatDeterministic(estimate, estimateScale)} ${toCode ?: "目標幣別"}" +
                "（輸出幣別：${fromCode ?: "來源幣別"}）"
            val rateText = "參考匯率：1 ${fromCode ?: "來源幣別"} = ${CurrencyConverter.formatDeterministic(unitRate, 6)} ${toCode ?: "目標幣別"}"

            state.copy(
                transferEstimateAmount = estimate,
                transferFromCurrencyCode = fromCode,
                transferToCurrencyCode = toCode,
                transferRate = unitRate,
                transferEstimateText = estimateText,
                transferRateText = rateText,
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(savedTransactionId = null) }
    }

    private fun observeCategories(categoryType: String) {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            getCategoriesByTypeUseCase(categoryType).collect { categories ->
                val selectable = categories.filter { it.parentCategoryId != null }
                _uiState.update {
                    it.copy(
                        categories = selectable,
                        form = it.form.copy(
                            categoryId = it.form.categoryId?.takeIf { id -> selectable.any { category -> category.categoryId == id } }
                                ?: selectable.firstOrNull()?.categoryId
                        ),
                    )
                }
            }
        }
    }
}

data class TransactionUiState(
    val account: Account? = null,
    val currencies: List<Currency> = emptyList(),
    val entryType: EntryType = EntryType.Expense,
    val categories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val form: TransactionFormState = TransactionFormState(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val savedTransactionId: Int? = null,
    val transferEstimateAmount: Double? = null,
    val transferFromCurrencyCode: String? = null,
    val transferToCurrencyCode: String? = null,
    val transferRate: Double? = null,
    val transferEstimateText: String? = null,
    val transferRateText: String? = null,
)

data class TransactionFormState(
    val transactionId: Int? = null,
    val date: String = LocalDate.now().toString(),
    val payeeName: String = "",
    val amount: String = "",
    val categoryId: Int? = null,
    val splitEnabled: Boolean = false,
    val splitItems: List<SplitFormItem> = emptyList(),
    val targetAccountId: Int? = null,
    val memo: String = "",
)

data class SplitFormItem(
    val id: Long,
    val categoryId: Int? = null,
    val amount: String = "",
    val memo: String = "",
)

enum class EntryType(val categoryType: String, val displayName: String) {
    Expense("E", "支出"),
    Income("I", "收入"),
    Transfer("", "轉帳"),
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun newSplitItem(defaultCategoryId: Int?): SplitFormItem = SplitFormItem(
    id = java.util.concurrent.atomic.AtomicLong(System.nanoTime()).incrementAndGet(),
    categoryId = defaultCategoryId,
)

