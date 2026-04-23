package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.PayeeDao
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.data.mapper.PayeeMapper
import com.finance.manager.android.domain.model.Payee
import com.finance.manager.android.domain.repository.PayeeRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PayeeRepositoryImpl @Inject constructor(
    private val payeeDao: PayeeDao,
    private val payeeMapper: PayeeMapper,
) : PayeeRepository {

    override suspend fun getNameById(payeeId: Int): String? =
        payeeDao.getById(payeeId)?.payeeName

    override suspend fun findByName(name: String): Int? = payeeDao.findByName(name.trim())?.payeeId

    override suspend fun insert(name: String): Int = payeeDao.insert(
        PayeeEntity(
            payeeName = name.trim(),
            createdAt = now(),
        )
    ).toInt()

    override fun observeAllWithStats(): Flow<List<Payee>> =
        payeeDao.observeAllWithStats().map { list -> list.map(payeeMapper::toDomain) }

    override suspend fun getDomainById(id: Int): Payee? =
        payeeDao.getById(id)?.let { payeeMapper.toDomain(it) }

    override suspend fun existsByName(name: String, excludeId: Int?): Boolean =
        payeeDao.existsByName(name.trim(), excludeId)

    override suspend fun insertDomain(payee: Payee): Long = payeeDao.insert(toEntity(payee, now()))

    override suspend fun update(payee: Payee) {
        val existing = payeeDao.getById(payee.payeeId) ?: return
        payeeDao.update(toEntity(payee, existing.createdAt))
    }

    override suspend fun setHidden(id: Int, hidden: Boolean) = payeeDao.setHidden(id, hidden)

    override suspend fun delete(id: Int) = payeeDao.deleteById(id)

    override suspend fun countUsage(id: Int): Int = payeeDao.countUsage(id)

    private fun toEntity(payee: Payee, createdAt: String): PayeeEntity = PayeeEntity(
        payeeId = payee.payeeId,
        payeeName = payee.payeeName.trim(),
        isHidden = payee.isHidden,
        description = payee.description,
        displayOrder = payee.displayOrder,
        createdAt = createdAt,
    )

    private fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}


