package com.finance.manager.android.presentation.quickadd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.presentation.account.AccountViewModel
import com.finance.manager.android.presentation.components.accountTypeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectAccount: (Int) -> Unit,
    onOpenAccountPage: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("快速記帳", style = MaterialTheme.typography.titleLarge)
            Text("先選擇帳戶，下一步直接開啟交易表單。", style = MaterialTheme.typography.bodyMedium)

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.accounts.isEmpty()) {
                Text(
                    "目前沒有可用帳戶，請先建立帳戶。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onOpenAccountPage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("前往帳戶管理")
                }
            } else {
                LazyColumn {
                    items(uiState.accounts, key = { it.accountId }) { account ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAccount(account.accountId) },
                            headlineContent = { Text(account.accountName) },
                            supportingContent = { Text(account.accountType.displayName) },
                            leadingContent = {
                                Icon(
                                    imageVector = accountTypeIcon(account.accountType),
                                    contentDescription = account.accountType.displayName,
                                )
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

