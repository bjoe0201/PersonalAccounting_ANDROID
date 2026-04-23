package com.finance.manager.android.presentation.account

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.presentation.components.AccountTypeIconCircle
import com.finance.manager.android.presentation.components.FinanceChip
import com.finance.manager.android.presentation.components.FinanceDivider
import com.finance.manager.android.presentation.components.formatAmount
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.OutlineVariant
import com.finance.manager.android.ui.theme.extendedColors

@Composable
fun AccountListScreen(
    onCreateAccount: () -> Unit,
    onEditAccount: (Int) -> Unit,
    onOpenRegister: (Int) -> Unit,
    onNavigateBack: () -> Unit = {},
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        pendingDeleteAccount?.let { account ->
            AlertDialog(
                onDismissRequest = { pendingDeleteAccount = null },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeleteAccount = null
                        viewModel.deleteAccount(account.accountId)
                    }) { Text("刪除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteAccount = null }) { Text("取消") }
                },
                title = { Text("刪除帳戶") },
                text = { Text("確定要刪除「${account.accountName}」嗎？") },
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
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
                    ) { Text("建立帳戶") }
                }
            }

            else -> {
                val assetTypes = setOf(AccountType.Bank, AccountType.Cash, AccountType.Invst, AccountType.OthA)
                val liabilityTypes = setOf(AccountType.CCard, AccountType.OthL)
                val assetAccounts = uiState.accounts.filter { it.accountType in assetTypes }
                val liabilityAccounts = uiState.accounts.filter { it.accountType in liabilityTypes }
                val netAssets = uiState.accounts.sumOf { it.currentBalance }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    // TopBar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "帳戶管理",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                    )
                                    .clickable(onClick = onCreateAccount)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("+", fontSize = 16.sp, color = Color.White, lineHeight = 16.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "新增帳戶",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }

                    // Asset accounts
                    if (assetAccounts.isNotEmpty()) {
                        item {
                            AccountGroupSection(
                                label = "資產帳戶",
                                accounts = assetAccounts,
                                onClickAccount = { onOpenRegister(it.accountId) },
                            )
                        }
                    }

                    // Liability accounts
                    if (liabilityAccounts.isNotEmpty()) {
                        item {
                            AccountGroupSection(
                                label = "負債帳戶",
                                accounts = liabilityAccounts,
                                onClickAccount = { onOpenRegister(it.accountId) },
                            )
                        }
                    }

                    // Net asset summary
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            GradientStart.copy(alpha = 0.08f),
                                            GradientEnd.copy(alpha = 0.12f),
                                        ),
                                    ),
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "總淨資產",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "T\$${formatAmount(netAssets)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AccountGroupSection(
    label: String,
    accounts: List<Account>,
    onClickAccount: (Account) -> Unit,
) {
    val ec = MaterialTheme.extendedColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Text(
            label.uppercase(),
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
            accounts.forEachIndexed { index, account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClickAccount(account) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AccountTypeIconCircle(type = account.accountType, size = 42.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            account.accountName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FinanceChip(
                            label = account.accountType.displayName,
                            color = ec.onSurfaceVariant,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${if (account.currentBalance < 0) "-" else ""}T\$${formatAmount(account.currentBalance)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (account.currentBalance < 0) ec.expenseRed
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = OutlineVariant,
                        )
                    }
                }
                if (index < accounts.lastIndex) {
                    FinanceDivider(horizontalPadding = 16.dp)
                }
            }
        }
    }
}
