package com.finance.manager.android.domain.usecase.account

import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Account
import com.finance.manager.android.domain.model.AccountType
import com.finance.manager.android.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateAccountUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var useCase: UpdateAccountUseCase

    private val existingAccount = Account(
        accountId = 1,
        accountName = "Old Name",
        accountType = AccountType.Bank,
        initialBalance = 1000.0,
        currentBalance = 2000.0,
        currencyId = 1,
    )

    @Before
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        useCase = UpdateAccountUseCase(accountRepository)
        coEvery { accountRepository.getById(1) } returns existingAccount
        coEvery { accountRepository.existsByName(any(), any()) } returns false
        coEvery { accountRepository.countTransactions(1) } returns 5
    }

    @Test
    fun `returns error when account not found`() = runTest {
        coEvery { accountRepository.getById(99) } returns null
        val result = useCase(99, "Test", AccountType.Bank, 0.0, false)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `returns error when name is blank`() = runTest {
        val result = useCase(1, "  ", AccountType.Bank, 1000.0, false)
        assertTrue(result is AppResult.Error)
        assertEquals("帳戶名稱不可空白", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error when name duplicated for different account`() = runTest {
        coEvery { accountRepository.existsByName("Duplicate", 1) } returns true
        val result = useCase(1, "Duplicate", AccountType.Bank, 1000.0, false)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `returns error when changing initial balance with existing transactions`() = runTest {
        val result = useCase(1, "Old Name", AccountType.Bank, 999.0, false)
        assertTrue(result is AppResult.Error)
        assertEquals("已有交易的帳戶不可修改初始餘額", (result as AppResult.Error).message)
    }

    @Test
    fun `allows initial balance change when no transactions`() = runTest {
        coEvery { accountRepository.countTransactions(1) } returns 0
        val result = useCase(1, "Old Name", AccountType.Bank, 2000.0, false)
        assertTrue(result is AppResult.Success)
        val slot = slot<Account>()
        coVerify { accountRepository.update(capture(slot)) }
        assertEquals(2000.0, slot.captured.initialBalance, 0.001)
        assertEquals(2000.0, slot.captured.currentBalance, 0.001)
    }

    @Test
    fun `preserves existing currencyId when null passed`() = runTest {
        val slot = slot<Account>()
        coEvery { accountRepository.update(capture(slot)) } returns Unit
        useCase(1, "Old Name", AccountType.Bank, 1000.0, false, currencyId = null)
        assertEquals(1, slot.captured.currencyId)
    }

    @Test
    fun `updates currencyId when new value provided`() = runTest {
        val slot = slot<Account>()
        coEvery { accountRepository.update(capture(slot)) } returns Unit
        useCase(1, "Old Name", AccountType.Bank, 1000.0, false, currencyId = 5)
        assertEquals(5, slot.captured.currencyId)
    }
}

