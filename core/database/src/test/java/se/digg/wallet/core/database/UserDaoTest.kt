// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import se.digg.wallet.core.model.SavedCredential

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun get_returnsNullWhenEmpty() = runTest {
        assertNull(userDao.get())
    }

    @Test
    fun upsert_and_get_storesUserSuccessfully() = runTest {
        val credential = SavedCredential(
            id = "cred-1",
            compactSerialized = "raw-token",
            claimDisplayNames = emptyMap(),
            issuer = null,
            displayData = null,
            keyId = "key-1",
        )
        val user = User(
            id = 0,
            accountId = "acc-123",
            credentials = listOf(credential),
            opaqueSession = OpaqueSession(
                serverPublicKeyJwk = "jwk",
                opaqueServerId = "server-1",
                opaqueContext = "ctx-1",
            ),
            isReset = false,
            backendGeneration = 1,
        )

        userDao.upsert(user)

        val retrieved = userDao.get()
        assertNotNull(retrieved)
        assertEquals("acc-123", retrieved?.accountId)
        assertEquals(1, retrieved?.credentials?.size)
        assertEquals("cred-1", retrieved?.credentials?.first()?.id)
        assertEquals("server-1", retrieved?.opaqueSession?.opaqueServerId)
        assertTrue(retrieved?.isOnboarded == true)
    }

    @Test
    fun observe_emitsUpdatedUser() = runTest {
        val user = User(
            id = 0,
            accountId = "acc-456",
            credentials = emptyList(),
        )

        userDao.upsert(user)

        val observed = userDao.observe().first()
        assertEquals("acc-456", observed?.accountId)
    }

    @Test
    fun clear_deletesAllUsers() = runTest {
        val user = User(
            id = 0,
            accountId = "acc-789",
            credentials = emptyList(),
        )
        userDao.upsert(user)
        assertNotNull(userDao.get())

        userDao.clear()
        assertNull(userDao.get())
    }
}
