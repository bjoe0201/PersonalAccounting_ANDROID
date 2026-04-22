package com.finance.manager.android.presentation.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.usecase.backup.BackupDatabaseUseCase
import com.finance.manager.android.domain.usecase.backup.BackupFileItem
import com.finance.manager.android.domain.usecase.backup.ListBackupsUseCase
import com.finance.manager.android.domain.usecase.backup.RestoreDatabaseUseCase
import com.finance.manager.android.domain.usecase.snapshot.RebuildAllSnapshotsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import com.finance.manager.android.domain.usecase.snapshot.RebuildProgress
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var rebuildAllSnapshotsUseCase: RebuildAllSnapshotsUseCase
    private lateinit var backupDatabaseUseCase: BackupDatabaseUseCase
    private lateinit var listBackupsUseCase: ListBackupsUseCase
    private lateinit var restoreDatabaseUseCase: RestoreDatabaseUseCase

    private val sampleBackups = listOf(
        BackupFileItem("finance_backup_20260101_120000.db", "/storage/backup/finance_backup_20260101_120000.db", 1000L),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        rebuildAllSnapshotsUseCase = mockk()
        backupDatabaseUseCase = mockk()
        listBackupsUseCase = mockk()
        restoreDatabaseUseCase = mockk()

        coEvery { listBackupsUseCase() } returns sampleBackups
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        rebuildAllSnapshotsUseCase,
        backupDatabaseUseCase,
        listBackupsUseCase,
        restoreDatabaseUseCase,
    )

    @Test
    fun `initial state loads backup list`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sampleBackups, viewModel.uiState.value.backups)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `backupDatabase on success shows success message and refreshes backups`() = runTest {
        coEvery { backupDatabaseUseCase() } returns AppResult.Success("/storage/backup/new.db")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.backupDatabase()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBusy)
        assertTrue(state.message?.contains("備份完成") == true)
    }

    @Test
    fun `backupDatabase on failure shows error message`() = runTest {
        coEvery { backupDatabaseUseCase() } returns AppResult.Error("儲存空間不足")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.backupDatabase()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("儲存空間不足", viewModel.uiState.value.message)
    }

    @Test
    fun `restoreDatabase on success shows success message`() = runTest {
        coEvery { restoreDatabaseUseCase(any()) } returns AppResult.Success(Unit)
        val backup = sampleBackups.first()
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreDatabase(backup)
        testDispatcher.scheduler.advanceUntilIdle()

        val message = viewModel.uiState.value.message
        assertTrue(message?.contains("已還原") == true)
        assertTrue(message?.contains(backup.name) == true)
    }

    @Test
    fun `restoreDatabase on failure shows error message`() = runTest {
        coEvery { restoreDatabaseUseCase(any()) } returns AppResult.Error("還原失敗")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreDatabase(sampleBackups.first())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("還原失敗", viewModel.uiState.value.message)
    }

    @Test
    fun `clearMessage resets message to null`() = runTest {
        coEvery { backupDatabaseUseCase() } returns AppResult.Error("error")
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.backupDatabase()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.clearMessage()

        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `rebuildSnapshots emits progress and finishes`() = runTest {
        coEvery { rebuildAllSnapshotsUseCase() } returns flowOf(
            RebuildProgress(current = 1, total = 2, currentAccountName = "帳戶A"),
            RebuildProgress(current = 2, total = 2, currentAccountName = "帳戶B"),
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.rebuildSnapshots()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBusy)
    }
}


