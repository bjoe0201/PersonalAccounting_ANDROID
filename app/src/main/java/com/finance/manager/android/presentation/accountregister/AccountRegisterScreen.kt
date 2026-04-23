package com.finance.manager.android.presentation.accountregister

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.presentation.components.CategoryBadge
import com.finance.manager.android.presentation.components.FinanceChip
import com.finance.manager.android.presentation.components.FinanceDivider
import com.finance.manager.android.presentation.components.formatAmount
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.OutlineVariant
import com.finance.manager.android.ui.theme.extendedColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

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
    var showDeleteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) { viewModel.load(accountId) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val ec = MaterialTheme.extendedColors

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTransaction(accountId) },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(26.dp),
                containerColor = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                            shape = RoundedCornerShape(26.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "新增交易",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->

        // Delete confirmation BottomSheet
        if (showDeleteSheet && pendingDeleteTransactionId != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDeleteSheet = false
                    pendingDeleteTransactionId = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    Text(
                        "刪除交易",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "確定要刪除這筆交易嗎？此操作無法復原。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ec.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                                .clickable {
                                    showDeleteSheet = false
                                    pendingDeleteTransactionId = null
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "取消",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ec.danger)
                                .clickable {
                                    pendingDeleteTransactionId?.let { viewModel.deleteTransaction(it) }
                                    showDeleteSheet = false
                                    pendingDeleteTransactionId = null
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "刪除",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val account = uiState.account
        val txs = uiState.transactions
        val balance = account?.currentBalance ?: 0.0
        val currentYm = YearMonth.now()
        // 本月收支（永遠對應「本月」標題）
        val monthIncome = txs.filter { YearMonth.from(it.transactionDate) == currentYm && it.amount > 0 }
            .sumOf { it.amount }
        val monthExpense = txs.filter { YearMonth.from(it.transactionDate) == currentYm && it.amount < 0 }
            .sumOf { it.amount }

        // Available months for month tabs
        val availableMonths = remember(txs) {
            txs.map { YearMonth.from(it.transactionDate) }
                .distinct()
                .sorted()
        }
        // 預設選擇最新的有資料月份；若無交易則用當月
        var selectedMonth by remember(availableMonths) {
            mutableStateOf(
                availableMonths.lastOrNull { it == currentYm }
                    ?: availableMonths.lastOrNull()
                    ?: currentYm
            )
        }

        // 只顯示選定月份的交易
        val filteredTxs = remember(txs, selectedMonth) {
            txs.filter { YearMonth.from(it.transactionDate) == selectedMonth }
        }
        val groupedByDate = remember(filteredTxs) {
            filteredTxs.sortedByDescending { it.transactionDate }
                .groupBy { it.transactionDate }
                .toSortedMap(compareByDescending { it })
        }
        // 選定月份淨額（用於期末結算 footer）
        val selectedMonthNet = filteredTxs.sumOf { it.amount }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // ── TopBar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account?.accountName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        account?.accountType?.displayName.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ec.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { /* filter */ }) {
                    Icon(Icons.Filled.FilterList, contentDescription = "篩選", tint = ec.onSurfaceVariant)
                }
                IconButton(onClick = { /* calendar */ }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "日曆", tint = ec.onSurfaceVariant)
                }
            }

            // ── Balance Header Card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "目前餘額",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "T\$${formatAmount(balance)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "本月",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+T\$${formatAmount(monthIncome)} / -T\$${formatAmount(abs(monthExpense))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Month Tabs ──
            if (availableMonths.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    items(availableMonths) { month ->
                        val isSelected = month == selectedMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { selectedMonth = month }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (isSelected) "${month.monthValue}月 ✓"
                                else "${month.monthValue}月",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else ec.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // ── Transaction List ──
            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (txs.isEmpty()) "目前沒有交易資料"
                        else "${selectedMonth.monthValue}月沒有交易資料",
                        color = ec.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) {
                    groupedByDate.forEach { (date, dateTxs) ->
                        val dayTotal = dateTxs.sumOf { it.amount }

                        // Date group header
                        item(key = "header-$date") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    date.format(DateTimeFormatter.ofPattern("yyyy / MM/dd")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ec.onSurfaceVariant,
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(ec.outlineVariant.copy(alpha = 0.6f)),
                                )
                                Text(
                                    "${if (dayTotal > 0) "+" else "-"}T\$${formatAmount(dayTotal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dayTotal >= 0) ec.incomeGreen else ec.expenseRed,
                                )
                            }
                        }

                        // Transaction cards for this date
                        item(key = "group-$date") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            ) {
                                dateTxs.forEachIndexed { index, tx ->
                                    TransactionCardRow(
                                        tx = tx,
                                        onEdit = if (tx.transferAccountName == null) {
                                            { onEditTransaction(accountId, tx.transactionId) }
                                        } else null,
                                        onDelete = {
                                            pendingDeleteTransactionId = tx.transactionId
                                            showDeleteSheet = true
                                        },
                                    )
                                    if (index < dateTxs.lastIndex) {
                                        FinanceDivider(horizontalPadding = 14.dp)
                                    }
                                }
                            }
                        }

                        item(key = "spacer-$date") {
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // Month balance footer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.extendedColors.surfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${selectedMonth.monthValue}月淨額",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ec.onSurfaceVariant,
                                )
                                Text(
                                    "${if (selectedMonthNet >= 0) "+" else "-"}T\$${formatAmount(selectedMonthNet)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedMonthNet >= 0) ec.incomeGreen else ec.expenseRed,
                                )
                            }
                        }
                        Spacer(Modifier.height(80.dp)) // space for FAB
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCardRow(
    tx: TransactionListItem,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val ec = MaterialTheme.extendedColors
    val isTransfer = tx.transferAccountName != null
    val isCleared = tx.clearedStatus == "C" || tx.clearedStatus == "X"
    val payeeDisplay = tx.payeeName
        ?: tx.transferAccountName?.let { "→ $it" }
        ?: tx.categoryName ?: "未命名"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // Cleared status dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    when (tx.clearedStatus) {
                        "C" -> ec.incomeGreen
                        "X" -> MaterialTheme.colorScheme.primary
                        else -> ec.outlineVariant
                    },
                    CircleShape,
                ),
        )

        // Category badge
        CategoryBadge(categoryName = if (isTransfer) "轉帳" else tx.categoryName, size = 36.dp)

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                payeeDisplay,
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
                    label = if (isTransfer) "轉帳" else tx.categoryName ?: "未分類",
                    color = if (isTransfer) ec.transferBlue else ec.onSurfaceVariant,
                    backgroundColor = if (isTransfer) ec.transferBlueLight
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                )
                if (!tx.memo.isNullOrBlank()) {
                    Text(
                        tx.memo,
                        style = MaterialTheme.typography.labelSmall,
                        color = ec.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Amount + status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (tx.amount > 0) "+" else ""}T\$${formatAmount(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = when {
                    isTransfer -> ec.transferBlue
                    tx.amount > 0 -> ec.incomeGreen
                    else -> ec.expenseRed
                },
            )
            if (!isCleared) {
                FinanceChip(
                    label = "未清算",
                    color = ec.warning,
                    backgroundColor = ec.warning.copy(alpha = 0.1f),
                )
            }
        }

        // Action buttons (only for non-transfer transactions)
        if (!isTransfer) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "編輯",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "刪除",
                        modifier = Modifier.size(17.dp),
                        tint = ec.expenseRed,
                    )
                }
            }
        }
    }
}
