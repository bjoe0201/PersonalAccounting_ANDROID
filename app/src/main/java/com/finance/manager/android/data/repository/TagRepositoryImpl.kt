package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.TagDao
import com.finance.manager.android.data.local.entity.TagEntity
import com.finance.manager.android.data.mapper.TagMapper
import com.finance.manager.android.domain.model.Tag
import com.finance.manager.android.domain.repository.TagRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val tagMapper: TagMapper,
) : TagRepository {

    override fun observeAllWithUsage(): Flow<List<Tag>> =
        tagDao.observeAllWithUsage().map { list -> list.map(tagMapper::toDomain) }

    override suspend fun getById(id: Int): Tag? = tagDao.getById(id)?.let { tagMapper.toDomain(it) }

    override suspend fun existsByName(name: String, excludeId: Int?): Boolean =
        tagDao.existsByName(name.trim(), excludeId)

    override suspend fun insert(tag: Tag): Long = tagDao.insert(toEntity(tag, now()))

    override suspend fun update(tag: Tag) {
        val existing = tagDao.getById(tag.tagId) ?: return
        tagDao.update(toEntity(tag, existing.createdAt))
    }

    override suspend fun setHidden(id: Int, hidden: Boolean) = tagDao.setHidden(id, hidden)

    override suspend fun delete(id: Int) = tagDao.deleteById(id)

    override suspend fun countUsage(id: Int): Int = tagDao.countUsage(id)

    private fun toEntity(tag: Tag, createdAt: String): TagEntity = TagEntity(
        tagId = tag.tagId,
        tagName = tag.tagName.trim(),
        isHidden = tag.isHidden,
        description = tag.description,
        tagType = tag.tagType,
        displayOrder = tag.displayOrder,
        createdAt = createdAt,
    )

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

