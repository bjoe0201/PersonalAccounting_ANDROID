package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.CategoryDao
import com.finance.manager.android.data.mapper.CategoryMapper
import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryMapper: CategoryMapper,
) : CategoryRepository {

    override fun observeByType(categoryType: String): Flow<List<Category>> =
        categoryDao.observeByType(categoryType).map(::mapCategories)

    override suspend fun getByType(categoryType: String): List<Category> =
        mapCategories(categoryDao.getByType(categoryType))

    private fun mapCategories(entities: List<com.finance.manager.android.data.local.entity.CategoryEntity>): List<Category> {
        val parents = entities.filter { it.parentCategoryId == null }.associateBy { it.categoryId }
        return entities.map { entity ->
            val parentName = entity.parentCategoryId?.let { parentId -> parents[parentId]?.categoryName }
            categoryMapper.toDomain(entity, parentName)
        }
    }
}

