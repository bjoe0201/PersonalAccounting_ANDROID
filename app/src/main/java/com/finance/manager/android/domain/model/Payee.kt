package com.finance.manager.android.domain.model

data class Payee(
    val payeeId: Int,
    val payeeName: String,
    val description: String? = null,
    val isHidden: Boolean = false,
    val displayOrder: Int = 0,
    val usageCount: Int = 0,
    val totalAmount: Double = 0.0,
)

