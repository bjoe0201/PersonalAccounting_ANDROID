package com.finance.manager.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.model.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormScreen(
    accountId: Int?,
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountId) {
        if (accountId == null) {
            viewModel.prepareNewAccount()
        } else {
            viewModel.loadAccount(accountId)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.savedAccountId) {
        if (uiState.savedAccountId != null) {
            viewModel.clearSavedState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountId == null) "新增帳戶" else "編輯帳戶") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveAccount() }) {
                        Text("儲存")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
            AccountFormContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                formState = uiState.formState,
                currencies = uiState.currencies,
                onNameChange = viewModel::updateAccountName,
                onTypeChange = viewModel::updateAccountType,
                onInitialBalanceChange = viewModel::updateInitialBalance,
                onHiddenChange = viewModel::updateHidden,
                onCurrencyChange = viewModel::updateCurrency,
                onSave = viewModel::saveAccount,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormContent(
    modifier: Modifier = Modifier,
    formState: AccountFormState,
    currencies: List<Currency>,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onHiddenChange: (Boolean) -> Unit,
    onCurrencyChange: (Int?) -> Unit,
    onSave: () -> Unit,
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = formState.accountName,
            onValueChange = onNameChange,
            label = { Text("帳戶名稱") },
            singleLine = true,
        )

        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded },
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                value = formState.accountType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("帳戶類型") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                AccountType.entries.forEach { accountType ->
                    DropdownMenuItem(
                        text = { Text(accountType.displayName) },
                        onClick = { onTypeChange(accountType); typeExpanded = false },
                    )
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = formState.initialBalance,
            onValueChange = onInitialBalanceChange,
            label = { Text("初始餘額") },
            singleLine = true,
        )

        if (currencies.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = !currencyExpanded },
            ) {
                val selectedCurrencyName = currencies.firstOrNull { it.currencyId == formState.currencyId }
                    ?.let { "${it.currencyCode} ${it.currencyName}" } ?: "請選擇幣別"
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    value = selectedCurrencyName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("幣別") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${currency.currencyCode} ${currency.currencyName}" +
                                        if (currency.isHome) "（本位幣）" else "",
                                )
                            },
                            onClick = { onCurrencyChange(currency.currencyId); currencyExpanded = false },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = formState.isHidden, onCheckedChange = onHiddenChange)
            Text("隱藏此帳戶")
        }

        Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
            Text("儲存帳戶")
        }
    }
}


