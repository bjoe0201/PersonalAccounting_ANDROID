package com.finance.manager.android.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finance.manager.android.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.categoryName ?: "") }
    var type by remember { mutableStateOf(initial?.categoryType ?: "E") }
    var taxLine by remember { mutableStateOf(initial?.taxLine ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var isHidden by remember { mutableStateOf(initial?.isHidden ?: false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val typeOptions = mapOf("E" to "支出", "I" to "收入", "T" to "投資")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增分類" else "編輯分類") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名稱") },
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = typeOptions[type] ?: type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("類型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                    ) {
                        typeOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { type = code; typeExpanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = taxLine,
                    onValueChange = { taxLine = it },
                    label = { Text("稅務項目（選填）") },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（選填）") },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("隱藏此分類")
                    Switch(checked = isHidden, onCheckedChange = { isHidden = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Category(
                        categoryId = initial?.categoryId ?: 0,
                        categoryName = name,
                        categoryType = type,
                        parentCategoryId = initial?.parentCategoryId,
                        displayName = name,
                        taxLine = taxLine.ifBlank { null },
                        displayOrder = initial?.displayOrder ?: 0,
                        isHidden = isHidden,
                        description = description.ifBlank { null },
                        usageCount = initial?.usageCount ?: 0,
                    )
                )
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}


