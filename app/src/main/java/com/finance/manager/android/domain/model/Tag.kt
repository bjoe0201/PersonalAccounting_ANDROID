package com.finance.manager.android.domain.model

data class Tag(
    val tagId: Int,
    val tagName: String,
    val description: String? = null,
    val tagType: String? = null,
    val isHidden: Boolean = false,
    val displayOrder: Int = 0,
    val usageCount: Int = 0,
)

