package com.finance.manager.android.domain.usecase.category

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> = repository.observeAllWithUsage()
}

class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): AppResult<Long> {
        val name = category.categoryName.trim()
        if (name.isBlank()) return AppResult.Error("分類名稱不可空白")
        if (category.categoryType !in setOf("E", "I", "T")) {
            return AppResult.Error("分類類型不合法（須為 E/I/T）")
        }
        if (repository.existsByName(name)) return AppResult.Error("分類名稱不可重複")
        if (category.parentCategoryId != null) {
            val parent = repository.getById(category.parentCategoryId)
                ?: return AppResult.Error("父分類不存在")
            if (parent.categoryType != category.categoryType) {
                return AppResult.Error("父分類類型不一致")
            }
        }
        return AppResult.Success(repository.insert(category.copy(categoryName = name)))
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): AppResult<Unit> {
        val name = category.categoryName.trim()
        if (name.isBlank()) return AppResult.Error("分類名稱不可空白")
        if (repository.existsByName(name, excludeId = category.categoryId)) {
            return AppResult.Error("分類名稱不可重複")
        }
        if (category.parentCategoryId != null) {
            if (category.parentCategoryId == category.categoryId) {
                return AppResult.Error("不可將自己設為父分類")
            }
            // 檢查父分類鏈是否形成循環
            var current: Int? = category.parentCategoryId
            val visited = mutableSetOf<Int>()
            while (current != null) {
                if (current == category.categoryId) return AppResult.Error("父分類形成循環")
                if (!visited.add(current)) break
                current = repository.getById(current)?.parentCategoryId
            }
        }
        repository.update(category.copy(categoryName = name))
        return AppResult.Success(Unit)
    }
}

class ToggleHiddenCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Int, hidden: Boolean): AppResult<Unit> {
        repository.setHidden(id, hidden)
        return AppResult.Success(Unit)
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: Int): AppResult<Unit> {
        val usage = repository.countUsage(id)
        if (usage > 0) return AppResult.Error("已有 $usage 筆交易引用此分類，無法刪除；請改為隱藏")
        val children = repository.countChildren(id)
        if (children > 0) return AppResult.Error("此分類仍有 $children 個子分類，無法刪除")
        repository.delete(id)
        return AppResult.Success(Unit)
    }
}

