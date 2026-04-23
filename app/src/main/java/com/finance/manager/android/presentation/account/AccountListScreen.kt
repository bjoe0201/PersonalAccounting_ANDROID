package com.finance.manager.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.finance.manager.android.domain.model.Account
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.presentation.components.accountTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onCreateAccount: () -> Unit,
    onEditAccount: (Int) -> Unit,
    onOpenRegister: (Int) -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteAccount by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("帳戶管理") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateAccount,
            ) {
                Text("新增帳戶")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        pendingDeleteAccount?.let { account ->
            AlertDialog(
                onDismissRequest = { pendingDeleteAccount = null },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeleteAccount = null
                        viewModel.deleteAccount(account.accountId)
                    }) {
                        Text("刪除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteAccount = null }) {
                        Text("取消")
                    }
                },
                title = { Text("刪除帳戶") },
                text = { Text("確定要刪除「${account.accountName}」嗎？") },
            )
        }

        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.accounts.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("目前還沒有帳戶")
                    Text("請先建立第一個帳戶開始記帳", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = onCreateAccount,
                    ) {
                        Text("建立帳戶")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.accounts, key = { it.accountId }) { account ->
                        AccountCard(
                            account = account,
                            onEdit = { onEditAccount(account.accountId) },
                            onOpenRegister = { onOpenRegister(account.accountId) },
                            onDelete = { pendingDeleteAccount = account },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    onEdit: () -> Unit,
    onOpenRegister: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = accountTypeIcon(account.accountType),
                            contentDescription = account.accountType.displayName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(account.accountName, style = MaterialTheme.typography.titleMedium)
                        Text(account.accountType.displayName, style = MaterialTheme.typography.bodyMedium)
                        if (account.isHidden) {
                            Text("已隱藏", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(
                    text = String.format("%.2f", account.currentBalance),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onEdit) {
                    Text("編輯")
                }
                TextButton(onClick = onOpenRegister) {
                    Text("登記簿")
                }
                TextButton(onClick = onDelete) {
                    Text("刪除")
                }
            }
        }
    }
}




