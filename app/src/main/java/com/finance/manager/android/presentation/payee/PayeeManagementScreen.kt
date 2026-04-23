package com.finance.manager.android.presentation.payee

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.Payee
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayeeManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: PayeeManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Payee?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it); viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("付款人管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startCreate) {
                Icon(Icons.Filled.Add, contentDescription = "新增付款人")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearch,
                label = { Text("搜尋付款人") },
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("顯示已隱藏", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Switch(checked = uiState.includeHidden, onCheckedChange = { viewModel.toggleIncludeHidden() })
                Spacer(Modifier.width(16.dp))
                Text("共 ${uiState.filtered.size} 筆", style = MaterialTheme.typography.bodySmall)
            }
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(uiState.filtered, key = { it.payeeId }) { p ->
                        PayeeRow(
                            p,
                            onEdit = { viewModel.startEdit(p) },
                            onToggleHidden = { viewModel.toggleHidden(p) },
                            onDelete = { pendingDelete = p },
                        )
                    }
                }
            }
        }
    }

    if (uiState.creatingNew || uiState.editing != null) {
        PayeeEditDialog(uiState.editing, onDismiss = viewModel::dismissEditor, onSave = viewModel::save)
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除付款人") },
            text = { Text("確定要刪除「${p.payeeName}」嗎？") },
            confirmButton = { TextButton(onClick = { viewModel.delete(p); pendingDelete = null }) { Text("刪除") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun PayeeRow(payee: Payee, onEdit: () -> Unit, onToggleHidden: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    payee.payeeName + if (payee.isHidden) "（已隱藏）" else "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (payee.isHidden) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("交易 ${payee.usageCount} 次", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "累計 $${String.format(Locale.getDefault(), "%,.0f", payee.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onToggleHidden) {
                Text(if (payee.isHidden) "顯示" else "隱藏")
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "編輯") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PayeeEditDialog(initial: Payee?, onDismiss: () -> Unit, onSave: (Payee) -> Unit) {
    var name by remember { mutableStateOf(initial?.payeeName ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var isHidden by remember { mutableStateOf(initial?.isHidden ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增付款人" else "編輯付款人") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名稱") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（選填）") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("隱藏此付款人"); Switch(checked = isHidden, onCheckedChange = { isHidden = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Payee(
                        payeeId = initial?.payeeId ?: 0,
                        payeeName = name,
                        description = description.ifBlank { null },
                        isHidden = isHidden,
                        displayOrder = initial?.displayOrder ?: 0,
                        usageCount = initial?.usageCount ?: 0,
                        totalAmount = initial?.totalAmount ?: 0.0,
                    )
                )
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}


