package com.finance.manager.android.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.presentation.components.AccountTypeIconCircle
import com.finance.manager.android.presentation.components.AmountText
import com.finance.manager.android.presentation.components.CategoryBadge
import com.finance.manager.android.presentation.components.FinanceChip
import com.finance.manager.android.presentation.components.FinanceDivider
import com.finance.manager.android.presentation.components.SectionHeader
import com.finance.manager.android.presentation.components.formatAmount
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.OutlineVariant
import com.finance.manager.android.ui.theme.extendedColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onOpenRegister: (Int) -> Unit,
    onOpenAccounts: () -> Unit,
    onQuickAdd: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }

    val summary = uiState.summary
    val accounts = summary?.accounts?.filter { !it.isHidden } ?: emptyList()
    val netWorth = summary?.netAssets ?: 0.0
    val monthIncome = summary?.monthIncome ?: 0.0
    val monthExpense = summary?.monthExpense ?: 0.0
    val monthBalance = monthIncome - monthExpense
    val currentYearMonth = YearMonth.now()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Top Bar ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "財務總覽",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extendedColors.onSurfaceVariant,
                    )
                    Text(
                        "${currentYearMonth.year}年${currentYearMonth.monthValue}月",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔔", fontSize = 18.sp)
                }
            }
        }

        // ── Net Worth Hero Card ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd),
                        ),
                    )
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Column {
                    Text(
                        "淨資產",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "T\$${formatAmount(netWorth)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.25f)),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        HeroStat("本月收入", "+T\$${formatAmount(monthIncome)}")
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                        HeroStat("本月支出", "-T\$${formatAmount(monthExpense)}")
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                        HeroStat("本月結餘", "+T\$${formatAmount(monthBalance)}")
                    }
                }
            }
        }

        // ── Accounts Section ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "我的帳戶",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    "管理",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenAccounts() },
                )
            }
        }

        // Horizontal account cards
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(accounts, key = { it.accountId }) { account ->
                    AccountMiniCard(
                        account = account,
                        onClick = { onOpenRegister(account.accountId) },
                    )
                }
            }
        }

        // ── Recent Transactions ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "最近交易",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    "全部",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "尚無交易記錄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.extendedColors.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                ) {
                    uiState.recentTransactions.forEachIndexed { index, tx ->
                        RecentTransactionRow(tx = tx)
                        if (index < uiState.recentTransactions.lastIndex) {
                            FinanceDivider(horizontalPadding = 14.dp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun AccountMiniCard(
    account: Account,
    onClick: () -> Unit,
) {
    val ec = MaterialTheme.extendedColors
    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .shadow(1.dp, RoundedCornerShape(14.dp), clip = false)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            AccountTypeIconCircle(type = account.accountType, size = 32.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                account.accountName,
                style = MaterialTheme.typography.labelMedium,
                color = ec.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${if (account.currentBalance < 0) "-" else ""}T\$${formatAmount(account.currentBalance)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (account.currentBalance < 0) ec.expenseRed
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RecentTransactionRow(tx: TransactionListItem) {
    val ec = MaterialTheme.extendedColors
    val isTransfer = tx.transferAccountName != null
    val title = tx.payeeName
        ?: tx.transferAccountName?.let { "→ $it" }
        ?: tx.categoryName ?: "未命名交易"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(categoryName = tx.categoryName, size = 38.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                FinanceChip(
                    label = tx.categoryName ?: "未分類",
                    color = ec.onSurfaceVariant,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                )
                Text(
                    tx.accountName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ec.onSurfaceVariant,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            AmountText(
                amount = tx.amount,
                isTransfer = isTransfer,
                fontSize = 14.sp,
            )
            Text(
                tx.transactionDate.format(DateTimeFormatter.ofPattern("MM/dd")),
                style = MaterialTheme.typography.labelSmall,
                color = ec.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
