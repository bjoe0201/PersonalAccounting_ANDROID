package com.finance.manager.android.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.presentation.components.accountTypeIcon
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.extendedColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenRegister: (Int) -> Unit,
    onAddAccount: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "財務總覽",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            val summary = uiState.summary
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ── HeaderCard（淨資產漸層卡片）
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                            .padding(24.dp),
                    ) {
                        Column {
                            Text(
                                "淨資產",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "T$ ${formatAmount(summary?.netAssets ?: 0.0)}",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.White,
                            )
                        }
                    }
                }

                // ── 本月收支摘要卡片
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "本月收支摘要",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            SummaryRow("收入", summary?.monthIncome ?: 0.0, MaterialTheme.extendedColors.incomeGreen)
                            SummaryRow("支出", -(summary?.monthExpense ?: 0.0), MaterialTheme.extendedColors.expenseRed)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            SummaryRow(
                                "結餘",
                                (summary?.monthIncome ?: 0.0) - (summary?.monthExpense ?: 0.0),
                                MaterialTheme.colorScheme.onSurface,
                                bold = true,
                            )
                        }
                    }
                }

                // ── 帳戶列表標題
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "帳戶列表",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        TextButton(onClick = onAddAccount) {
                            Icon(Icons.Filled.Add, contentDescription = "新增帳戶", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("新增帳戶")
                        }
                    }
                }

                // ── 帳戶列表
                items(summary?.accounts.orEmpty(), key = { it.accountId }) { account ->
                    AccountCard(account = account, onClick = { onOpenRegister(account.accountId) })
                }

                // ── 最近交易標題
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "最近交易",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("尚無交易記錄", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column {
                                uiState.recentTransactions.forEachIndexed { index, transaction ->
                                    RecentTransactionRow(transaction = transaction)
                                    if (index < uiState.recentTransactions.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyLarge,
        )
        Text(
            "T$ ${formatAmount(kotlin.math.abs(amount))}",
            style = if (bold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyLarge,
            color = color,
        )
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    val extendedColors = MaterialTheme.extendedColors
    val balanceColor = when {
        account.currentBalance < 0 -> extendedColors.expenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accountTypeIcon(account.accountType),
                        contentDescription = account.accountType.displayName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(account.accountName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                    Text(account.accountType.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "T$ ${formatAmount(account.currentBalance)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = balanceColor,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(transaction: TransactionListItem) {
    val extendedColors = MaterialTheme.extendedColors
    val isTransfer = transaction.transferAccountName != null
    val amountColor = when {
        isTransfer -> extendedColors.transferBlue
        transaction.amount >= 0 -> extendedColors.incomeGreen
        else -> extendedColors.expenseRed
    }
    val title = transaction.payeeName
        ?: transaction.transferAccountName?.let { "轉帳至 $it" }
        ?: transaction.categoryName
        ?: "未命名交易"
    val dateStr = transaction.transactionDate.format(DateTimeFormatter.ofPattern("MM/dd"))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                if (!transaction.categoryName.isNullOrEmpty()) {
                    Text(transaction.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "${if (transaction.amount >= 0) "+" else ""}T$ ${formatAmount(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor,
        )
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == kotlin.math.floor(amount) && !amount.isInfinite()) {
        String.format(Locale.getDefault(), "%,.0f", amount)
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }
}


