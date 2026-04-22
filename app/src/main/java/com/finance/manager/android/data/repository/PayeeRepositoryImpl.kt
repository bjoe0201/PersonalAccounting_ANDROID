package com.finance.manager.android.data.repository

import com.finance.manager.android.data.local.dao.PayeeDao
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.domain.repository.PayeeRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PayeeRepositoryImpl @Inject constructor(
    private val payeeDao: PayeeDao,
) : PayeeRepository {

    override suspend fun getNameById(payeeId: Int): String? =
        payeeDao.getById(payeeId)?.payeeName

    override suspend fun findByName(name: String): Int? = payeeDao.findByName(name.trim())?.payeeId

    override suspend fun insert(name: String): Int = payeeDao.insert(
        PayeeEntity(
            payeeName = name.trim(),
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
    ).toInt()
}

