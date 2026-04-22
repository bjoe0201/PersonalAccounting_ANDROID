package com.finance.manager.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.finance.manager.android.data.local.entity.AccountEntity
import com.finance.manager.android.data.local.entity.AccountMonthlyBalanceEntity
import com.finance.manager.android.data.local.entity.AppSettingEntity
import com.finance.manager.android.data.local.entity.CategoryEntity
import com.finance.manager.android.data.local.entity.CurrencyEntity
import com.finance.manager.android.data.local.entity.PayeeEntity
import com.finance.manager.android.data.local.entity.TagEntity
import com.finance.manager.android.data.local.entity.TransactionEntity
import com.finance.manager.android.data.local.entity.TransactionSplitEntity
import com.finance.manager.android.data.local.entity.TransactionSplitTagEntity
import com.finance.manager.android.data.local.entity.TransactionTagEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        TransactionSplitEntity::class,
        CategoryEntity::class,
        CurrencyEntity::class,
        PayeeEntity::class,
        TagEntity::class,
        TransactionTagEntity::class,
        TransactionSplitTagEntity::class,
        AccountMonthlyBalanceEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionSplitDao(): TransactionSplitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun payeeDao(): PayeeDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun transactionSplitTagDao(): TransactionSplitTagDao
    abstract fun accountMonthlyBalanceDao(): AccountMonthlyBalanceDao
    abstract fun settingsDao(): AppSettingsDao
}

