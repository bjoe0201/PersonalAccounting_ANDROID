package com.finance.manager.android.di

import com.finance.manager.android.data.repository.AccountRepositoryImpl
import com.finance.manager.android.data.repository.CategoryRepositoryImpl
import com.finance.manager.android.data.repository.CurrencyRepositoryImpl
import com.finance.manager.android.data.repository.PayeeRepositoryImpl
import com.finance.manager.android.data.repository.TransactionRepositoryImpl
import com.finance.manager.android.domain.repository.AccountRepository
import com.finance.manager.android.domain.repository.CategoryRepository
import com.finance.manager.android.domain.repository.CurrencyRepository
import com.finance.manager.android.domain.repository.PayeeRepository
import com.finance.manager.android.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindPayeeRepository(impl: PayeeRepositoryImpl): PayeeRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}



