package com.finance.manager.android.domain.usecase.category

import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCategoriesByTypeUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(categoryType: String): Flow<List<Category>> = categoryRepository.observeByType(categoryType)
}

