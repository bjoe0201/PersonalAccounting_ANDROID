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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.presentation.components.accountTypeIcon
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.extendedColors
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
                    Column {
                        Text(
                            "財務管理系統",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "淨值 T$ ${formatAmount(uiState.summary?.netAssets ?: 0.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val summary = uiState.summary
        val grouped = remember(summary?.accounts) {
            (summary?.accounts ?: emptyList())
                .filter { !it.isHidden }
                .groupBy { groupOfType(it.accountType) }
                .toSortedMap(compareBy { key -> GROUP_ORDER.indexOf(key).let { idx -> if (idx < 0) 99 else idx } })
        }
        val expanded = remember { mutableStateMapOf<String, Boolean>() }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Net Worth 漸層卡片
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                        .padding(20.dp),
                ) {
                    Column {
                        Text("Net Worth", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "T$ ${formatAmount(summary?.netAssets ?: 0.0)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White,
                        )
                    }
                }
            }

            // 帳戶清冊標題
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Accounts（帳戶清冊）",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    TextButton(onClick = onAddAccount) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新增帳戶")
                    }
                }
            }

            // 依群組顯示帳戶
            grouped.forEach { (groupName, list) ->
                val groupSum = list.sumOf { it.currentBalance }
                val isExpanded = expanded[groupName] ?: true
                item(key = "header-$groupName") {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expanded[groupName] = !isExpanded },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(groupName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Text(
                                "T$ ${formatAmount(groupSum)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (groupSum < 0) MaterialTheme.extendedColors.expenseRed
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (isExpanded) {
                    list.forEach { account ->
                        item(key = "acc-${account.accountId}") {
                            AccountRow(account = account, onClick = { onOpenRegister(account.accountId) })
                        }
                    }
                }
            }

            // 本月收支摘要
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "本月收支",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            // 最近交易
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "最近交易",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            if (uiState.recentTransactions.isEmpty()) {
                item { Text("尚無交易記錄", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column {
                            uiState.recentTransactions.forEachIndexed { index, tx ->
                                val ec = MaterialTheme.extendedColors
                                val isTransfer = tx.transferAccountName != null
                                val amountColor = when {
                                    isTransfer -> ec.transferBlue
                                    tx.amount >= 0 -> ec.incomeGreen
                                    else -> ec.expenseRed
                                }
                                val title = tx.payeeName
                                    ?: tx.transferAccountName?.let { "轉帳至 $it" }
                                    ?: tx.categoryName ?: "未命名交易"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text(
                                            "${tx.transactionDate} ${tx.categoryName.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        "${if (tx.amount >= 0) "+" else ""}T$ ${formatAmount(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = amountColor,
                                    )
                                }
                                if (index < uiState.recentTransactions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val ec = MaterialTheme.extendedColors
    val balanceColor = if (account.currentBalance < 0) ec.expenseRed else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = accountTypeIcon(account.accountType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(account.accountName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "T$ ${formatAmount(account.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = balanceColor,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

private val GROUP_ORDER = listOf("Banking（銀行/現金）", "信用卡", "投資", "其他資產", "其他負債")

private fun groupOfType(type: AccountType): String = when (type) {
    AccountType.Bank, AccountType.Cash -> "Banking（銀行/現金）"
    AccountType.CCard -> "信用卡"
    AccountType.Invst -> "投資"
    AccountType.OthA -> "其他資產"
    AccountType.OthL -> "其他負債"
}

private fun formatAmount(amount: Double): String {
    return if (amount == kotlin.math.floor(amount) && !amount.isInfinite()) {
        String.format(Locale.getDefault(), "%,.0f", amount)
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }
}

