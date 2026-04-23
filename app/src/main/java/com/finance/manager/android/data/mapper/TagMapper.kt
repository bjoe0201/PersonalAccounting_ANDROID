package com.finance.manager.android.data.mapper

import com.finance.manager.android.data.local.dao.TagWithUsageLocal
import com.finance.manager.android.data.local.entity.TagEntity
import com.finance.manager.android.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagMapper @Inject constructor() {
    fun toDomain(entity: TagEntity, usageCount: Int = 0): Tag = Tag(
        tagId = entity.tagId,
        tagName = entity.tagName,
        description = entity.description,
        tagType = entity.tagType,
        isHidden = entity.isHidden,
        displayOrder = entity.displayOrder,
        usageCount = usageCount,
    )

    fun toDomain(local: TagWithUsageLocal): Tag = toDomain(local.tag, local.usageCount)
}

