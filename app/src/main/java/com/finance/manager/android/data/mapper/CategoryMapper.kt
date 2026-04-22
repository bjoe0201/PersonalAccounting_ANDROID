package com.finance.manager.android.data.mapper

import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryMapper @Inject constructor() {
    fun toDomain(entity: CategoryEntity, parentName: String? = null): Category = Category(
        categoryId = entity.categoryId,
        categoryName = entity.categoryName,
        categoryType = entity.categoryType,
        parentCategoryId = entity.parentCategoryId,
        displayName = if (parentName.isNullOrBlank()) entity.categoryName else "$parentName:${entity.categoryName}",
    )
}

