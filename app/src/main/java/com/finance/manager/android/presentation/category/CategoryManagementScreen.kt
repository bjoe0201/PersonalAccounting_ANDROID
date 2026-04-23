package com.finance.manager.android.presentation.category

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.finance.manager.android.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("類別管理") },
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
                Icon(Icons.Filled.Add, contentDescription = "新增分類")
            }
        },
    ) { padding ->
        Content(padding, uiState, viewModel, onDeleteRequest = { pendingDelete = it })
    }

    if (uiState.creatingNew || uiState.editing != null) {
        CategoryEditDialog(
            initial = uiState.editing,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::save,
        )
    }

    pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("刪除分類") },
            text = { Text("確定要刪除「${cat.categoryName}」嗎？已被交易引用的分類不可刪除。") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(cat); pendingDelete = null }) { Text("刪除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    padding: PaddingValues,
    uiState: CategoryManagementUiState,
    viewModel: CategoryManagementViewModel,
    onDeleteRequest: (Category) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.searchQuery,
            onValueChange = viewModel::updateSearch,
            label = { Text("搜尋分類") },
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryFilter.entries.forEach { f ->
                FilterChip(
                    selected = uiState.filter == f,
                    onClick = { viewModel.updateFilter(f) },
                    label = { Text(f.displayName) },
                )
            }
        }
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
                items(uiState.filtered, key = { it.categoryId }) { cat ->
                    CategoryRow(
                        cat,
                        onEdit = { viewModel.startEdit(cat) },
                        onToggleHidden = { viewModel.toggleHidden(cat) },
                        onDelete = { onDeleteRequest(cat) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName + if (category.isHidden) "（已隱藏）" else "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (category.isHidden) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeLabel = when (category.categoryType) {
                        "E" -> "支出"; "I" -> "收入"; "T" -> "投資"; else -> category.categoryType
                    }
                    AssistChip(onClick = {}, label = { Text(typeLabel) })
                    Text("使用 ${category.usageCount} 次", style = MaterialTheme.typography.bodySmall)
                    category.taxLine?.takeIf { it.isNotBlank() }?.let {
                        Text("稅務：$it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            TextButton(onClick = onToggleHidden) {
                Text(if (category.isHidden) "顯示" else "隱藏")
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "編輯") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}



