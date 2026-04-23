package com.finance.manager.android.data.mapper

import com.finance.manager.android.data.local.dao.PayeeWithUsageLocal
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.domain.model.Payee
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayeeMapper @Inject constructor() {
    fun toDomain(entity: PayeeEntity, usageCount: Int = 0, totalAmount: Double = 0.0): Payee = Payee(
        payeeId = entity.payeeId,
        payeeName = entity.payeeName,
        description = entity.description,
        isHidden = entity.isHidden,
        displayOrder = entity.displayOrder,
        usageCount = usageCount,
        totalAmount = totalAmount,
    )

    fun toDomain(local: PayeeWithUsageLocal): Payee =
        toDomain(local.payee, local.usageCount, local.totalAmount)
}

