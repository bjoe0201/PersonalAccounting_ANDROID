package com.finance.manager.android.domain.usecase.tag

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Tag
import com.finance.manager.android.domain.repository.TagRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTagsUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    operator fun invoke(): Flow<List<Tag>> = repository.observeAllWithUsage()
}

class CreateTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tag: Tag): AppResult<Long> {
        val name = tag.tagName.trim()
        if (name.isBlank()) return AppResult.Error("標籤名稱不可空白")
        if (repository.existsByName(name)) return AppResult.Error("標籤名稱不可重複")
        return AppResult.Success(repository.insert(tag.copy(tagName = name)))
    }
}

class UpdateTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(tag: Tag): AppResult<Unit> {
        val name = tag.tagName.trim()
        if (name.isBlank()) return AppResult.Error("標籤名稱不可空白")
        if (repository.existsByName(name, excludeId = tag.tagId)) {
            return AppResult.Error("標籤名稱不可重複")
        }
        repository.update(tag.copy(tagName = name))
        return AppResult.Success(Unit)
    }
}

class ToggleHiddenTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(id: Int, hidden: Boolean): AppResult<Unit> {
        repository.setHidden(id, hidden)
        return AppResult.Success(Unit)
    }
}

class DeleteTagUseCase @Inject constructor(
    private val repository: TagRepository,
) {
    suspend operator fun invoke(id: Int): AppResult<Unit> {
        val usage = repository.countUsage(id)
        if (usage > 0) return AppResult.Error("已有 $usage 筆交易引用此標籤，無法刪除；請改為隱藏")
        repository.delete(id)
        return AppResult.Success(Unit)
    }
}

