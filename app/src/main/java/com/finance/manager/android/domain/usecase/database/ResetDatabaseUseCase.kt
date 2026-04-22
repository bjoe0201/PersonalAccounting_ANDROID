package com.finance.manager.android.domain.usecase.database

import com.finance.manager.android.data.local.DatabaseInitializer
import javax.inject.Inject

class ResetDatabaseUseCase @Inject constructor(
    private val databaseInitializer: DatabaseInitializer,
) {
    suspend operator fun invoke() {
        databaseInitializer.forceInitialize()
    }
}

