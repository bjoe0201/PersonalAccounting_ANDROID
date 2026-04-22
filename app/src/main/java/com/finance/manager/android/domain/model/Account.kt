package com.finance.manager.android.domain.model

data class Account(
    val accountId: Int = 0,
    val accountName: String,
    val accountType: AccountType,
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isHidden: Boolean = false,
    val displayOrder: Int = 0,
    val currencyId: Int? = null,
    val createdAt: String = "",
)

enum class AccountType(val code: String, val displayName: String) {
    Bank("Bank", "銀行帳戶"),
    Cash("Cash", "現金"),
    CCard("CCard", "信用卡"),
    Invst("Invst", "投資"),
    OthA("Oth A", "其他資產"),
    OthL("Oth L", "其他負債");

    companion object {
        fun fromCode(code: String): AccountType = entries.firstOrNull { it.code == code } ?: Bank
    }
}

