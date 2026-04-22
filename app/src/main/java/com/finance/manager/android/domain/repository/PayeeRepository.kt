package com.finance.manager.android.domain.repository

interface PayeeRepository {
    suspend fun getNameById(payeeId: Int): String?
    suspend fun findByName(name: String): Int?
    suspend fun insert(name: String): Int
}

