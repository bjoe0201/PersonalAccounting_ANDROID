package com.finance.manager.android.domain.model

data class Category(
    val categoryId: Int,
    val categoryName: String,
    val categoryType: String,
    val parentCategoryId: Int? = null,
    val displayName: String = categoryName,
)

