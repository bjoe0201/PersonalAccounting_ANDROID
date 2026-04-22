package com.finance.manager.android.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finance.manager.android.data.local.AppDatabase
import com.finance.manager.android.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AccountDao

    private val now = "2026-01-01T00:00:00"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.accountDao()
    }

    @After
    fun tearDown() = db.close()

    private fun account(name: String, balance: Double = 0.0, hidden: Boolean = false) =
        AccountEntity(accountName = name, accountType = "Bank", initialBalance = balance, currentBalance = balance, isHidden = hidden, createdAt = now)

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(account("Bank")).toInt()
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Bank", result!!.accountName)
    }

    @Test
    fun getAll_returnsAllAccounts() = runTest {
        dao.insert(account("A"))
        dao.insert(account("B"))
        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun observeVisibleAccounts_excludesHidden() = runTest {
        dao.insert(account("Visible"))
        dao.insert(account("Hidden", hidden = true))
        val visible = dao.observeVisibleAccounts().first()
        assertEquals(1, visible.size)
        assertEquals("Visible", visible.first().accountName)
    }

    @Test
    fun existsByName_trueWhenExists() = runTest {
        dao.insert(account("DuplicateName"))
        assertTrue(dao.existsByName("DuplicateName"))
    }

    @Test
    fun existsByName_falseWhenNotExists() = runTest {
        assertFalse(dao.existsByName("NoSuchName"))
    }

    @Test
    fun existsByName_excludesCurrentId() = runTest {
        val id = dao.insert(account("SameName")).toInt()
        assertFalse(dao.existsByName("SameName", excludeId = id))
    }

    @Test
    fun incrementCurrentBalance_addsCorrectly() = runTest {
        val id = dao.insert(account("Wallet", balance = 1000.0)).toInt()
        dao.incrementCurrentBalance(id, 500.0)
        val updated = dao.getById(id)!!
        assertEquals(1500.0, updated.currentBalance, 0.001)
    }

    @Test
    fun incrementCurrentBalance_subtractsWithNegativeDelta() = runTest {
        val id = dao.insert(account("Wallet", balance = 1000.0)).toInt()
        dao.incrementCurrentBalance(id, -300.0)
        val updated = dao.getById(id)!!
        assertEquals(700.0, updated.currentBalance, 0.001)
    }

    @Test
    fun deleteById_removesAccount() = runTest {
        val id = dao.insert(account("ToDelete")).toInt()
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun update_changesName() = runTest {
        val id = dao.insert(account("OldName")).toInt()
        val existing = dao.getById(id)!!
        dao.update(existing.copy(accountName = "NewName"))
        assertEquals("NewName", dao.getById(id)!!.accountName)
    }
}

