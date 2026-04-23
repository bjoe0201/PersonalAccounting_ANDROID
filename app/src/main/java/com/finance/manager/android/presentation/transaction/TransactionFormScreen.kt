package com.finance.manager.android.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    accountId: Int,
    transactionId: Int?,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountId, transactionId) {
        viewModel.load(accountId, transactionId)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.savedTransactionId) {
        if (uiState.savedTransactionId != null) {
            viewModel.clearSavedState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "新增交易" else "編輯交易") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = { TextButton(onClick = viewModel::save) { Text("儲存") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isSaving) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        } else {
            TransactionFormContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                uiState = uiState,
                onDateChange = viewModel::updateDate,
                onPayeeChange = viewModel::updatePayee,
                onAmountChange = viewModel::updateAmount,
                onMemoChange = viewModel::updateMemo,
                onEntryTypeChange = viewModel::updateEntryType,
                onCategoryChange = viewModel::updateCategory,
                onTargetAccountChange = viewModel::updateTargetAccount,
                onSplitEnabledChange = viewModel::updateSplitEnabled,
                onAddSplitItem = viewModel::addSplitItem,
                onRemoveSplitItem = viewModel::removeSplitItem,
                onSplitCategoryChange = viewModel::updateSplitCategory,
                onSplitAmountChange = viewModel::updateSplitAmount,
                onSplitMemoChange = viewModel::updateSplitMemo,
                onSave = viewModel::save,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormContent(
    modifier: Modifier,
    uiState: TransactionUiState,
    onDateChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onEntryTypeChange: (EntryType) -> Unit,
    onCategoryChange: (Int) -> Unit,
    onTargetAccountChange: (Int) -> Unit,
    onSplitEnabledChange: (Boolean) -> Unit,
    onAddSplitItem: () -> Unit,
    onRemoveSplitItem: (Long) -> Unit,
    onSplitCategoryChange: (Long, Int) -> Unit,
    onSplitAmountChange: (Long, String) -> Unit,
    onSplitMemoChange: (Long, String) -> Unit,
    onSave: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val entryTypes = if (uiState.form.transactionId == null) EntryType.entries else EntryType.entries.filter { it != EntryType.Transfer }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(uiState.account?.accountName ?: "")

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            entryTypes.forEachIndexed { index, entryType ->
                SegmentedButton(
                    selected = uiState.entryType == entryType,
                    onClick = { onEntryTypeChange(entryType) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = entryTypes.size),
                ) {
                    Text(entryType.displayName)
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            value = uiState.form.date,
            onValueChange = {},
            readOnly = true,
            label = { Text("日期") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "選擇日期")
                }
            },
            singleLine = true,
        )

        if (showDatePicker) {
            val initialDate = runCatching { LocalDate.parse(uiState.form.date) }.getOrDefault(LocalDate.now())
            val datePickerState = androidx.compose.material3.rememberDatePickerState(
                initialSelectedDateMillis = initialDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateChange(selectedDate.toString())
                        }
                        showDatePicker = false
                    }) {
                        Text("確定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("取消")
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (uiState.entryType != EntryType.Transfer) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.form.payeeName,
                onValueChange = onPayeeChange,
                label = { Text("付款人") },
                singleLine = true,
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.form.amount,
            onValueChange = onAmountChange,
            label = { Text("金額") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        if (uiState.entryType == EntryType.Transfer) {
            ExposedDropdownMenuBox(expanded = targetExpanded, onExpandedChange = { targetExpanded = !targetExpanded }) {
                val selectedTargetName = uiState.availableAccounts.firstOrNull { it.accountId == uiState.form.targetAccountId }?.accountName.orEmpty()
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = selectedTargetName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("轉入帳戶") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(targetExpanded) },
                )
                ExposedDropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }) {
                    uiState.availableAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.accountName) },
                            onClick = {
                                onTargetAccountChange(account.accountId)
                                targetExpanded = false
                            },
                        )
                    }
                }
            }

            uiState.transferEstimateText?.let { Text(it) }
            uiState.transferRateText?.let { Text(it) }
        } else if (!uiState.form.splitEnabled) {
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                val selectedCategoryName = uiState.categories.firstOrNull { it.categoryId == uiState.form.categoryId }?.displayName.orEmpty()
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分類") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                onCategoryChange(category.categoryId)
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (uiState.entryType != EntryType.Transfer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.form.splitEnabled,
                    onCheckedChange = onSplitEnabledChange,
                )
                Text("分割交易")
            }
        }

        if (uiState.form.splitEnabled) {
            Text("分割項目", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            uiState.form.splitItems.forEach { split ->
                CardBlock(
                    uiState = uiState,
                    split = split,
                    onRemoveSplitItem = onRemoveSplitItem,
                    onSplitCategoryChange = onSplitCategoryChange,
                    onSplitAmountChange = onSplitAmountChange,
                    onSplitMemoChange = onSplitMemoChange,
                )
            }
            Button(onClick = onAddSplitItem, modifier = Modifier.fillMaxWidth()) {
                Text("新增分割項目")
            }
            val splitTotal = uiState.form.splitItems.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            val signedTotal = if (uiState.entryType == EntryType.Expense) -splitTotal else splitTotal
            val expected = uiState.form.amount.toDoubleOrNull()?.let {
                if (uiState.entryType == EntryType.Expense) -it else it
            } ?: 0.0
            Text("分割合計：${String.format("%.2f", signedTotal)}")
            Text("差額：${String.format("%.2f", expected - signedTotal)}")
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.form.memo,
            onValueChange = onMemoChange,
            label = { Text("備註") },
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSave,
        ) {
            Text("儲存交易")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardBlock(
    uiState: TransactionUiState,
    split: SplitFormItem,
    onRemoveSplitItem: (Long) -> Unit,
    onSplitCategoryChange: (Long, Int) -> Unit,
    onSplitAmountChange: (Long, String) -> Unit,
    onSplitMemoChange: (Long, String) -> Unit,
) {
    var expanded by remember(split.id) { mutableStateOf(false) }
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                val selectedName = uiState.categories.firstOrNull { it.categoryId == split.categoryId }?.displayName.orEmpty()
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分類") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                onSplitCategoryChange(split.id, category.categoryId)
                                expanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = split.amount,
                onValueChange = { onSplitAmountChange(split.id, it) },
                label = { Text("分割金額") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = split.memo,
                onValueChange = { onSplitMemoChange(split.id, it) },
                label = { Text("分割備註") },
            )
            Spacer(modifier = Modifier.height(2.dp))
            Button(onClick = { onRemoveSplitItem(split.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("刪除此分割")
            }
        }
    }
}

