package com.finance.manager.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.ui.theme.*

data class AccountTypeStyle(
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color,
)

fun accountTypeStyle(type: AccountType): AccountTypeStyle = when (type) {
    AccountType.Bank -> AccountTypeStyle(Icons.Filled.AccountBalance, BankIconBg, BankIconColor)
    AccountType.Cash -> AccountTypeStyle(Icons.Filled.Payments, CashIconBg, CashIconColor)
    AccountType.CCard -> AccountTypeStyle(Icons.Filled.CreditCard, CCardIconBg, CCardIconColor)
    AccountType.Invst -> AccountTypeStyle(Icons.AutoMirrored.Filled.TrendingUp, InvestIconBg, InvestIconColor)
    AccountType.OthA -> AccountTypeStyle(Icons.Filled.Person, BankIconBg, BankIconColor)
    AccountType.OthL -> AccountTypeStyle(Icons.AutoMirrored.Filled.List, CCardIconBg, CCardIconColor)
}

fun accountTypeIcon(type: AccountType): ImageVector = accountTypeStyle(type).icon

@Composable
fun AccountTypeIconCircle(
    type: AccountType,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
) {
    val style = accountTypeStyle(type)
    Box(
        modifier = modifier
            .size(size)
            .background(style.backgroundColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = type.displayName,
            tint = style.iconColor,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
