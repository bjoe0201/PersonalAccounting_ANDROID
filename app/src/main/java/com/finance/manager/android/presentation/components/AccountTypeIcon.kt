package com.finance.manager.android.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.finance.manager.android.domain.model.AccountType

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.Bank -> Icons.Filled.Home
    AccountType.Cash -> Icons.Filled.Star
    AccountType.CCard -> Icons.Filled.Info
    AccountType.Invst -> Icons.Filled.Settings
    AccountType.OthA -> Icons.Filled.Person
    AccountType.OthL -> Icons.AutoMirrored.Filled.List
}




