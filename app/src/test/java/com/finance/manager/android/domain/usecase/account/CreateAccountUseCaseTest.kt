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

class CreateAccountUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var useCase: CreateAccountUseCase

    @Before
    fun setUp() {
        accountRepository = mockk(relaxed = true)
        useCase = CreateAccountUseCase(accountRepository)
        coEvery { accountRepository.existsByName(any(), any()) } returns false
        coEvery { accountRepository.insert(any()) } returns 1L
    }

    @Test
    fun `returns error when name is blank`() = runTest {
        val result = useCase("  ", AccountType.Bank, 0.0, false)
        assertTrue(result is AppResult.Error)
        assertEquals("帳戶名稱不可空白", (result as AppResult.Error).message)
    }

    @Test
    fun `returns error when name already exists`() = runTest {
        coEvery { accountRepository.existsByName("Cash", null) } returns true
        val result = useCase("Cash", AccountType.Cash, 0.0, false)
        assertTrue(result is AppResult.Error)
        assertEquals("帳戶名稱不可重複", (result as AppResult.Error).message)
    }

    @Test
    fun `creates account with correct initial and current balance`() = runTest {
        val slot = slot<Account>()
        coEvery { accountRepository.insert(capture(slot)) } returns 42L

        val result = useCase("Savings", AccountType.Bank, 5000.0, false)

        assertTrue(result is AppResult.Success)
        assertEquals(42L, (result as AppResult.Success).data)
        assertEquals(5000.0, slot.captured.initialBalance, 0.001)
        assertEquals(5000.0, slot.captured.currentBalance, 0.001)
        assertEquals("Savings", slot.captured.accountName)
    }

    @Test
    fun `creates account with currencyId`() = runTest {
        val slot = slot<Account>()
        coEvery { accountRepository.insert(capture(slot)) } returns 1L

        useCase("USD Account", AccountType.Bank, 0.0, false, currencyId = 3)

        assertEquals(3, slot.captured.currencyId)
    }

    @Test
    fun `trims whitespace from account name`() = runTest {
        val slot = slot<Account>()
        coEvery { accountRepository.insert(capture(slot)) } returns 1L

        useCase("  My Bank  ", AccountType.Bank, 0.0, false)

        assertEquals("My Bank", slot.captured.accountName)
    }
}

