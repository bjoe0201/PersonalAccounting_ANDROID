package com.finance.manager.android.di

import android.content.Context
import androidx.room.Room
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.dao.AccountDao
import com.finance.manager.android.data.local.dao.AccountMonthlyBalanceDao
import com.finance.manager.android.data.local.dao.AppSettingsDao
import com.finance.manager.android.data.local.dao.CategoryDao
import com.finance.manager.android.data.local.dao.CurrencyDao
import com.finance.manager.android.data.local.dao.PayeeDao
import com.finance.manager.android.data.local.dao.TagDao
import com.finance.manager.android.data.local.dao.TransactionDao
import com.finance.manager.android.data.local.dao.TransactionSplitDao
import com.finance.manager.android.data.local.dao.TransactionSplitTagDao
import com.finance.manager.android.data.local.dao.TransactionTagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finance_manager.db",
        ).build()
    }

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTransactionSplitDao(db: AppDatabase): TransactionSplitDao = db.transactionSplitDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideCurrencyDao(db: AppDatabase): CurrencyDao = db.currencyDao()
    @Provides fun providePayeeDao(db: AppDatabase): PayeeDao = db.payeeDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideTransactionTagDao(db: AppDatabase): TransactionTagDao = db.transactionTagDao()
    @Provides fun provideTransactionSplitTagDao(db: AppDatabase): TransactionSplitTagDao = db.transactionSplitTagDao()
    @Provides fun provideSnapshotDao(db: AppDatabase): AccountMonthlyBalanceDao = db.accountMonthlyBalanceDao()
    @Provides fun provideAppSettingsDao(db: AppDatabase): AppSettingsDao = db.settingsDao()
}

