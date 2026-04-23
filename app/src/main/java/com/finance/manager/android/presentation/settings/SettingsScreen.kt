package com.finance.manager.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.finance.manager.android.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenCurrencies: () -> Unit,
    onOpenCategories: () -> Unit = {},
    onOpenPayees: () -> Unit = {},
    onOpenTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<BackupListItemUi?>(null) }

    // 初始化資料庫：第 1 步確認（提示說明）
    var showResetStep1 by remember { mutableStateOf(false) }
    // 初始化資料庫：第 2 步確認（最後警告）
    var showResetStep2 by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        // ── 還原備份確認 Dialog ──
        pendingRestore?.let { backup ->
            AlertDialog(
                onDismissRequest = { pendingRestore = null },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRestore = null
                        viewModel.restoreDatabase(backup.toDomain())
                    }) {
                        Text("還原")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestore = null }) {
                        Text("取消")
                    }
                },
                title = { Text("還原備份") },
                text = { Text("確定要還原 ${backup.name} 嗎？還原後請重新啟動 App。") },
            )
        }

        // ── 初始化資料庫：第 1 步確認 Dialog ──
        if (showResetStep1) {
            AlertDialog(
                onDismissRequest = { showResetStep1 = false },
                confirmButton = {
                    TextButton(onClick = {
                        showResetStep1 = false
                        showResetStep2 = true
                    }) { Text("我了解，繼續") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetStep1 = false }) { Text("取消") }
                },
                title = { Text("初始化資料庫") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "⚠️ 警告",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text("此操作將會：")
                        Text("• 刪除所有帳戶與帳戶餘額")
                        Text("• 刪除所有交易記錄")
                        Text("• 刪除所有自訂分類、付款人、標籤")
                        Text("• 重設幣別設定")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "資料一旦清空將無法復原，請確認已備份重要資料。",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                },
            )
        }

        // ── 初始化資料庫：第 2 步最終確認 Dialog ──
        if (showResetStep2) {
            AlertDialog(
                onDismissRequest = { showResetStep2 = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetStep2 = false
                            viewModel.resetDatabase()
                        },
                        colors = androidx.compose.material3.ButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.38f),
                        )
                    ) { Text("確認清空並初始化") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetStep2 = false }) { Text("取消") }
                },
                title = {
                    Text(
                        "最後確認",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                text = {
                    Text(
                        "您確定要清空所有資料並重新初始化資料庫嗎？\n\n此操作「無法復原」。",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator()
                    }
                    uiState.progressMessage?.let { Text(it) }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenCategories) {
                        Text("類別管理")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenPayees) {
                        Text("付款人管理")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenTags) {
                        Text("標籤管理")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenCurrencies) {
                        Text("幣別管理")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = viewModel::rebuildSnapshots) {
                        Text("重建月結快照")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = viewModel::backupDatabase) {
                        Text("備份資料庫")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showResetStep1 = true },
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("初始化資料庫")
                    }
                    Text(
                        "⚠ 將清空全部資料並還原初始狀態，無法復原",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text("可還原備份", modifier = Modifier.padding(top = 8.dp))
                }
            }

            if (uiState.backups.isEmpty()) {
                item {
                    Text("目前沒有可用備份")
                }
            } else {
                items(uiState.backups.map(::BackupListItemUi), key = { it.absolutePath }) { backup ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(backup.name)
                            Text(backup.lastModifiedText)
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { pendingRestore = backup },
                                enabled = !uiState.isBusy,
                            ) {
                                Text("還原這份備份")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "PersonalAccounting",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}  (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class BackupListItemUi(
    val name: String,
    val absolutePath: String,
    val lastModified: Long,
) {
    constructor(item: com.finance.manager.android.domain.usecase.backup.BackupFileItem) : this(
        name = item.name,
        absolutePath = item.absolutePath,
        lastModified = item.lastModified,
    )

    val lastModifiedText: String
        get() = "最後修改：${DateFormat.getDateTimeInstance().format(Date(lastModified))}"

    fun toDomain() = com.finance.manager.android.domain.usecase.backup.BackupFileItem(
        name = name,
        absolutePath = absolutePath,
        lastModified = lastModified,
    )
}

