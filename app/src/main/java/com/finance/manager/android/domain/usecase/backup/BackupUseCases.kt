package com.finance.manager.android.domain.usecase.backup

import android.content.Context
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.AppSettingEntity
import com.finance.manager.android.domain.common.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupFileItem(
    val name: String,
    val absolutePath: String,
    val lastModified: Long,
)

class BackupDatabaseUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    suspend operator fun invoke(): AppResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val now = LocalDateTime.now()
            db.query("PRAGMA wal_checkpoint(FULL)", null)
            val source = context.getDatabasePath("finance_manager.db")
            val backupDir = File(context.getExternalFilesDir(null), "backup").apply { mkdirs() }
            val fileName = "finance_backup_${now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.db"
            val target = File(backupDir, fileName)
            source.copyTo(target, overwrite = true)
            db.settingsDao().upsert(
                AppSettingEntity(
                    key = "last_backup_at",
                    value = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                )
            )
            AppResult.Success(target.absolutePath)
        }.getOrElse { AppResult.Error("備份失敗：${it.message}") }
    }
}

class ListBackupsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(): List<BackupFileItem> = withContext(Dispatchers.IO) {
        val backupDir = File(context.getExternalFilesDir(null), "backup")
        backupDir.listFiles { file -> file.isFile && file.extension.equals("db", ignoreCase = true) }
            ?.sortedByDescending(File::lastModified)
            ?.map { file ->
                BackupFileItem(
                    name = file.name,
                    absolutePath = file.absolutePath,
                    lastModified = file.lastModified(),
                )
            }
            .orEmpty()
    }
}

class RestoreDatabaseUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    suspend operator fun invoke(backupPath: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return@withContext AppResult.Error("找不到備份檔案")
            }

            db.close()
            val target = context.getDatabasePath("finance_manager.db")
            backupFile.copyTo(target, overwrite = true)
            AppResult.Success(Unit)
        }.getOrElse { AppResult.Error("還原失敗：${it.message}") }
    }
}


