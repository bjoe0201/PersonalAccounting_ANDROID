package com.finance.manager.android.presentation.accountregister

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRegisterScreen(
    accountId: Int,
    onNavigateBack: () -> Unit,
    onCreateTransaction: (Int) -> Unit,
    onEditTransaction: (Int, Int) -> Unit,
    viewModel: AccountRegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteTransactionId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(accountId) {
        viewModel.load(accountId)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.account?.accountName ?: "帳戶登記簿") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateTransaction(accountId) },
            ) {
                Text("新增交易")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        pendingDeleteTransactionId?.let { transactionId ->
            AlertDialog(
                onDismissRequest = { pendingDeleteTransactionId = null },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeleteTransactionId = null
                        viewModel.deleteTransaction(transactionId)
                    }) { Text("刪除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteTransactionId = null }) { Text("取消") }
                },
                title = { Text("刪除交易") },
                text = { Text("確定要刪除這筆交易嗎？") },
            )
        }

        if (uiState.isLoading) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("目前餘額", style = MaterialTheme.typography.titleMedium)
                        Text(String.format("%.2f", uiState.account?.currentBalance ?: 0.0), style = MaterialTheme.typography.headlineMedium)
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Text("目前沒有交易資料")
                    }
                } else {
                    items(uiState.transactions, key = { it.transactionId }) { item ->
                        val isTransfer = item.transferAccountName != null
                        val transferLabel = when {
                            !isTransfer -> null
                            item.amount < 0 -> "轉帳至 ${item.transferAccountName}"
                            else -> "轉帳自 ${item.transferAccountName}"
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(item.transactionDate.toString(), style = MaterialTheme.typography.bodySmall)
                            Text(
                                item.payeeName
                                    ?: transferLabel
                                    ?: item.categoryName
                                    ?: "分割交易",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(transferLabel ?: item.categoryName ?: "分割明細")
                            Text(item.memo.orEmpty())
                            Text(String.format("%.2f", item.amount), style = MaterialTheme.typography.titleMedium)
                            if (!isTransfer) {
                                TextButton(onClick = { onEditTransaction(accountId, item.transactionId) }) {
                                    Text("編輯")
                                }
                            }
                            TextButton(onClick = { pendingDeleteTransactionId = item.transactionId }) {
                                Text("刪除")
                            }
                        }
                    }
                }
            }
        }
    }
}


