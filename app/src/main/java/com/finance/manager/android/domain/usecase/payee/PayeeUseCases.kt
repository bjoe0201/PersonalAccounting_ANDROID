package com.finance.manager.android.domain.usecase.payee

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Payee
import com.finance.manager.android.domain.repository.PayeeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePayeesUseCase @Inject constructor(
    private val repository: PayeeRepository,
) {
    operator fun invoke(): Flow<List<Payee>> = repository.observeAllWithStats()
}

class CreatePayeeUseCase @Inject constructor(
    private val repository: PayeeRepository,
) {
    suspend operator fun invoke(payee: Payee): AppResult<Long> {
        val name = payee.payeeName.trim()
        if (name.isBlank()) return AppResult.Error("付款人名稱不可空白")
        if (repository.existsByName(name)) return AppResult.Error("付款人名稱不可重複")
        return AppResult.Success(repository.insertDomain(payee.copy(payeeName = name)))
    }
}

class UpdatePayeeUseCase @Inject constructor(
    private val repository: PayeeRepository,
) {
    suspend operator fun invoke(payee: Payee): AppResult<Unit> {
        val name = payee.payeeName.trim()
        if (name.isBlank()) return AppResult.Error("付款人名稱不可空白")
        if (repository.existsByName(name, excludeId = payee.payeeId)) {
            return AppResult.Error("付款人名稱不可重複")
        }
        repository.update(payee.copy(payeeName = name))
        return AppResult.Success(Unit)
    }
}

class ToggleHiddenPayeeUseCase @Inject constructor(
    private val repository: PayeeRepository,
) {
    suspend operator fun invoke(id: Int, hidden: Boolean): AppResult<Unit> {
        repository.setHidden(id, hidden)
        return AppResult.Success(Unit)
    }
}

class DeletePayeeUseCase @Inject constructor(
    private val repository: PayeeRepository,
) {
    suspend operator fun invoke(id: Int): AppResult<Unit> {
        val usage = repository.countUsage(id)
        if (usage > 0) return AppResult.Error("已有 $usage 筆交易引用此付款人，無法刪除；請改為隱藏")
        repository.delete(id)
        return AppResult.Success(Unit)
    }
}

