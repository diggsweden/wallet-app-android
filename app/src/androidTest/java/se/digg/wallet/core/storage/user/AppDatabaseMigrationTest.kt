// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    /**
     * Validates that the current v3 schema can be created and basic DAO operations work.
     * Runs against an in-memory database — no schema files required.
     */
    @Test
    fun currentSchema_databaseCreatesSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        runBlocking {
            assertNull("Fresh database should have no user", db.userDao().get())
        }

        db.close()
    }

    /**
     * Validates the v2 → v3 auto-migration:
     * - pin, email, phone columns are dropped
     * - surviving columns (uuid, accountId, credentials, pid) retain their data
     */
    @Test
    fun migrate2To3_pinEmailPhoneDropped_survivingDataPreserved() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO user (id, pin, email, phone, uuid, accountId, credentials, pid) " +
                    "VALUES (0, 'secret-pin', 'test@test.com', '0701234567', NULL, 'acc-123', '[]', NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        db.query("SELECT * FROM user WHERE id = 0").use { cursor ->
            assertEquals("Row should survive migration", 1, cursor.count)
            cursor.moveToFirst()

            assertEquals("pin column should be removed", -1, cursor.getColumnIndex("pin"))
            assertEquals("email column should be removed", -1, cursor.getColumnIndex("email"))
            assertEquals("phone column should be removed", -1, cursor.getColumnIndex("phone"))

            assertEquals(
                "accountId should be preserved",
                "acc-123",
                cursor.getString(cursor.getColumnIndexOrThrow("accountId")),
            )
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
