package com.finance.manager.android.domain.model

data class Category(
    val categoryId: Int,
    val categoryName: String,
    val categoryType: String,
    val parentCategoryId: Int? = null,
    val displayName: String = categoryName,
    val taxLine: String? = null,
    val displayOrder: Int = 0,
    val isHidden: Boolean = false,
    val description: String? = null,
    val usageCount: Int = 0,
)

