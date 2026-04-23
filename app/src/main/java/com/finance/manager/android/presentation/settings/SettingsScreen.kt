package com.finance.manager.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.BuildConfig
import com.finance.manager.android.presentation.components.FinanceDivider
import com.finance.manager.android.ui.theme.OutlineVariant
import com.finance.manager.android.ui.theme.extendedColors

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
    val ec = MaterialTheme.extendedColors

    var showResetStep1 by remember { mutableStateOf(false) }
    var showResetStep2 by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        // Reset dialogs
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "資料一旦清空將無法復原，請確認已備份重要資料。",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                },
            )
        }
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
                        ),
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
                    Text("您確定要清空所有資料並重新初始化資料庫嗎？\n\n此操作「無法復原」。")
                },
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Title
            item {
                Text(
                    "設定",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp),
                )
            }

            // Loading indicator
            if (uiState.isBusy) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        uiState.progressMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            // ── 安全性 ──
            item {
                SettingsGroup(
                    title = "安全性",
                    items = listOf(
                        SettingsItemData(
                            icon = "🔒",
                            label = "PIN 鎖定",
                            description = "未啟用",
                            toggle = false,
                            onToggle = {
                                viewModel.showComingSoon("PIN 鎖定")
                            },
                        ),
                        SettingsItemData(
                            icon = "👆",
                            label = "生物辨識",
                            description = "未啟用",
                            toggle = false,
                            onToggle = {
                                viewModel.showComingSoon("生物辨識")
                            },
                        ),
                    ),
                )
            }

            // ── 帳目管理 ──
            item {
                SettingsGroup(
                    title = "帳目管理",
                    items = listOf(
                        SettingsItemData("🏷️", "分類管理", arrow = true, onClick = onOpenCategories),
                        SettingsItemData("👤", "付款人管理", arrow = true, onClick = onOpenPayees),
                        SettingsItemData("🔖", "標籤管理", arrow = true, onClick = onOpenTags),
                        SettingsItemData("💱", "幣別管理", arrow = true, onClick = onOpenCurrencies),
                    ),
                )
            }

            // ── 資料管理 ──
            item {
                SettingsGroup(
                    title = "資料管理",
                    items = listOf(
                        SettingsItemData(
                            "💾", "備份資料庫",
                            arrow = true,
                            onClick = { viewModel.backupDatabase() },
                        ),
                        SettingsItemData(
                            "🔄", "重建月結快照",
                            description = "資料修復用途",
                            arrow = true,
                            onClick = { viewModel.rebuildSnapshots() },
                        ),
                        SettingsItemData(
                            "⚠️", "初始化資料庫",
                            description = "清除所有資料",
                            danger = true,
                            arrow = true,
                            onClick = { showResetStep1 = true },
                        ),
                    ),
                )
            }

            // ── 關於 ──
            item {
                SettingsGroup(
                    title = "關於",
                    items = listOf(
                        SettingsItemData(
                            "ℹ️", "版本",
                            description = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        ),
                        SettingsItemData("📄", "開放原始碼授權", arrow = true),
                    ),
                )
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "PersonalAccounting",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "個人財務管理系統 for Android",
                        style = MaterialTheme.typography.labelSmall,
                        color = ec.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class SettingsItemData(
    val icon: String,
    val label: String,
    val description: String? = null,
    val arrow: Boolean = false,
    val danger: Boolean = false,
    val toggle: Boolean? = null,
    val onToggle: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItemData>,
) {
    val ec = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ec.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (item.onClick != null) Modifier.clickable(onClick = item.onClick)
                            else Modifier
                        )
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        item.icon,
                        fontSize = 20.sp,
                        modifier = Modifier.width(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (item.danger) ec.danger else MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.description != null) {
                            Text(
                                item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = ec.onSurfaceVariant,
                            )
                        }
                    }
                    if (item.arrow) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (item.danger) ec.danger else OutlineVariant,
                        )
                    }
                    if (item.toggle != null) {
                        Switch(
                            checked = item.toggle,
                            onCheckedChange = { item.onToggle?.invoke(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = OutlineVariant,
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                }
                if (index < items.lastIndex) {
                    FinanceDivider(horizontalPadding = 16.dp)
                }
            }
        }
    }
}
