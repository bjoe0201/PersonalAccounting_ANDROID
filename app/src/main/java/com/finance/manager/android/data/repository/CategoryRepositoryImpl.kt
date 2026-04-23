package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.CategoryDao
import com.finance.manager.android.data.local.dao.CategoryWithUsageLocal
import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.data.mapper.CategoryMapper
import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.repository.CategoryRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    override fun observeAllWithUsage(): Flow<List<Category>> =
        categoryDao.observeAllWithUsage().map { list: List<CategoryWithUsageLocal> ->
            val parents = list.map { it.category }.filter { it.parentCategoryId == null }
                .associateBy { it.categoryId }
            list.map { item ->
                val parentName = item.category.parentCategoryId?.let { pid -> parents[pid]?.categoryName }
                categoryMapper.toDomain(item.category, parentName, item.usageCount)
            }
        }

    override suspend fun getById(id: Int): Category? =
        categoryDao.getById(id)?.let { categoryMapper.toDomain(it) }

    override suspend fun existsByName(name: String, excludeId: Int?): Boolean =
        categoryDao.existsByName(name.trim(), excludeId)

    override suspend fun insert(category: Category): Long =
        categoryDao.insert(toEntity(category, createdAt = now()))

    override suspend fun update(category: Category) {
        val existing = categoryDao.getById(category.categoryId) ?: return
        categoryDao.update(toEntity(category, createdAt = existing.createdAt))
    }

    override suspend fun setHidden(id: Int, hidden: Boolean) = categoryDao.setHidden(id, hidden)

    override suspend fun delete(id: Int) = categoryDao.deleteById(id)

    override suspend fun countUsage(id: Int): Int = categoryDao.countUsage(id)

    override suspend fun countChildren(id: Int): Int = categoryDao.countChildren(id)

    private fun mapCategories(entities: List<CategoryEntity>): List<Category> {
        val parents = entities.filter { it.parentCategoryId == null }.associateBy { it.categoryId }
        return entities.map { entity ->
            val parentName = entity.parentCategoryId?.let { parentId -> parents[parentId]?.categoryName }
            categoryMapper.toDomain(entity, parentName)
        }
    }

    private fun toEntity(category: Category, createdAt: String): CategoryEntity = CategoryEntity(
        categoryId = category.categoryId,
        categoryName = category.categoryName.trim(),
        categoryType = category.categoryType,
        parentCategoryId = category.parentCategoryId,
        taxLine = category.taxLine,
        displayOrder = category.displayOrder,
        isHidden = category.isHidden,
        description = category.description,
        createdAt = createdAt,
    )

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}



