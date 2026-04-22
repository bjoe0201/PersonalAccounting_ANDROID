package com.finance.manager.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.usecase.backup.BackupDatabaseUseCase
import com.finance.manager.android.domain.usecase.backup.BackupFileItem
import com.finance.manager.android.domain.usecase.backup.ListBackupsUseCase
import com.finance.manager.android.domain.usecase.backup.RestoreDatabaseUseCase
import com.finance.manager.android.domain.usecase.database.ResetDatabaseUseCase
import com.finance.manager.android.domain.usecase.snapshot.RebuildAllSnapshotsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val rebuildAllSnapshotsUseCase: RebuildAllSnapshotsUseCase,
    private val backupDatabaseUseCase: BackupDatabaseUseCase,
    private val listBackupsUseCase: ListBackupsUseCase,
    private val restoreDatabaseUseCase: RestoreDatabaseUseCase,
    private val resetDatabaseUseCase: ResetDatabaseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshBackups()
    }

    fun rebuildSnapshots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            rebuildAllSnapshotsUseCase().collect { progress ->
                _uiState.update {
                    it.copy(
                        isBusy = progress.current != progress.total,
                        progressMessage = "${progress.currentAccountName} (${progress.current}/${progress.total})",
                    )
                }
            }
            refreshBackups()
        }
    }

    fun backupDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = backupDatabaseUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "備份完成：${result.data}")
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isBusy = false, message = result.message)
                }
            }
            refreshBackups()
        }
    }

    fun restoreDatabase(backup: BackupFileItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = restoreDatabaseUseCase(backup.absolutePath)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = "已還原 ${backup.name}，請重新啟動 App 以重新開啟資料庫",
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isBusy = false, message = result.message)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, progressMessage = "正在清空並初始化資料庫…") }
            try {
                resetDatabaseUseCase()
                _uiState.update {
                    it.copy(isBusy = false, progressMessage = null, message = "資料庫已成功初始化，請重新啟動 App。")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isBusy = false, progressMessage = null, message = "初始化失敗：${e.message}")
                }
            }
        }
    }

    private fun refreshBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(backups = listBackupsUseCase()) }
        }
    }
}

data class SettingsUiState(
    val isBusy: Boolean = false,
    val progressMessage: String? = null,
    val message: String? = null,
    val backups: List<BackupFileItem> = emptyList(),
)

