package com.finance.manager.android.presentation.accountregister

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.TransactionListItem
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.extendedColors
import java.util.Locale

private val COL_DATE = 96.dp
private val COL_PAYEE = 130.dp
private val COL_CATEGORY = 130.dp
private val COL_TAG = 80.dp
private val COL_MEMO = 140.dp
private val COL_EXPENSE = 96.dp
private val COL_INCOME = 96.dp
private val COL_BALANCE = 110.dp
private val COL_ACTION = 96.dp

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

    LaunchedEffect(accountId) { viewModel.load(accountId) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.account?.accountName.orEmpty()} - 交易清冊") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { onCreateTransaction(accountId) }) {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val account = uiState.account
        val txs = uiState.transactions

        val runningBalance = remember(txs, account?.initialBalance) {
            val asc = txs.sortedBy { it.transactionDate }
            val map = HashMap<Int, Double>(asc.size)
            var running = account?.initialBalance ?: 0.0
            for (t in asc) {
                running += t.amount
                map[t.transactionId] = running
            }
            map
        }

        val hScroll = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AccountHeaderCard(
                accountName = account?.accountName.orEmpty(),
                accountTypeLabel = account?.accountType?.displayName.orEmpty(),
                balance = account?.currentBalance ?: 0.0,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell("日期", COL_DATE)
                HeaderCell("收/付款人", COL_PAYEE)
                HeaderCell("類別", COL_CATEGORY)
                HeaderCell("標籤", COL_TAG)
                HeaderCell("備註", COL_MEMO)
                HeaderCell("支出", COL_EXPENSE, TextAlign.End)
                HeaderCell("收入", COL_INCOME, TextAlign.End)
                HeaderCell("餘額", COL_BALANCE, TextAlign.End)
                HeaderCell("操作", COL_ACTION, TextAlign.Center)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            if (txs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("目前沒有交易資料", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(txs, key = { it.transactionId }) { item ->
                        TransactionRegisterRow(
                            item = item,
                            balance = runningBalance[item.transactionId] ?: 0.0,
                            hScrollState = hScroll,
                            onEdit = if (item.transferAccountName == null) {
                                { onEditTransaction(accountId, item.transactionId) }
                            } else null,
                            onDelete = { pendingDeleteTransactionId = item.transactionId },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountHeaderCard(
    accountName: String,
    accountTypeLabel: String,
    balance: Double,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    accountName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                if (accountTypeLabel.isNotEmpty()) {
                    Text(
                        accountTypeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "當前餘額",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    formatAmount(balance),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TransactionRegisterRow(
    item: TransactionListItem,
    balance: Double,
    hScrollState: ScrollState,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val ec = MaterialTheme.extendedColors
    val isTransfer = item.transferAccountName != null
    val isIncome = item.amount >= 0
    val categoryLabel = when {
        isTransfer -> "[${item.transferAccountName}]"
        else -> item.categoryName.orEmpty()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScrollState)
            .clickable(enabled = onEdit != null, onClick = { onEdit?.invoke() })
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyCell(item.transactionDate.toString(), COL_DATE)
        BodyCell(item.payeeName ?: "-", COL_PAYEE)
        BodyCell(
            categoryLabel.ifEmpty { "-" },
            COL_CATEGORY,
            color = if (isTransfer) ec.transferBlue else MaterialTheme.colorScheme.onSurface,
        )
        BodyCell("-", COL_TAG)
        BodyCell(item.memo?.ifBlank { "-" } ?: "-", COL_MEMO)

        BodyCell(
            if (!isIncome) formatAmount(-item.amount) else "-",
            COL_EXPENSE,
            align = TextAlign.End,
            color = if (!isIncome) ec.expenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
            bold = !isIncome,
        )
        BodyCell(
            if (isIncome) formatAmount(item.amount) else "-",
            COL_INCOME,
            align = TextAlign.End,
            color = if (isIncome) ec.incomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            bold = isIncome,
        )
        BodyCell(
            formatAmount(balance),
            COL_BALANCE,
            align = TextAlign.End,
            color = if (balance < 0) ec.expenseRed else MaterialTheme.colorScheme.primary,
            bold = true,
        )
        Box(modifier = Modifier.width(COL_ACTION), contentAlignment = Alignment.Center) {
            Row {
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.height(36.dp)) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "編輯",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.height(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = ec.expenseRed)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        textAlign = align,
        maxLines = 1,
    )
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun BodyCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        else MaterialTheme.typography.bodyMedium,
        textAlign = align,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.width(4.dp))
}

private fun formatAmount(amount: Double): String {
    return if (amount == kotlin.math.floor(amount) && !amount.isInfinite()) {
        String.format(Locale.getDefault(), "%,.0f", amount)
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }
}

