// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [User::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migration2To3::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6, spec = AppDatabase.Migration5To6::class),
    ],
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    @DeleteColumn(tableName = "user", columnName = "pin")
    @DeleteColumn(tableName = "user", columnName = "email")
    @DeleteColumn(tableName = "user", columnName = "phone")
    class Migration2To3 : AutoMigrationSpec

    // Unused column: device identity is the AndroidKeyStore key under KeyAlias.DEVICE_KEY.
    @DeleteColumn(tableName = "user", columnName = "uuid")
    class Migration5To6 : AutoMigrationSpec

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE `user`
                    SET `credentials` = CASE
                            WHEN `credentials` IS NULL OR trim(`credentials`) IN ('', '[]')
                                THEN '[' || `pid` || ']'
                            ELSE '[' || `pid` || ',' || substr(trim(`credentials`), 2)
                        END
                    WHERE `pid` IS NOT NULL AND trim(`pid`) <> ''
                    """.trimIndent(),
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_new` (`id` INTEGER NOT NULL, " +
                        "`uuid` TEXT, `accountId` TEXT, `credentials` TEXT NOT NULL, " +
                        "`opaqueSession` TEXT, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `user_new` " +
                        "(`id`, `uuid`, `accountId`, `credentials`, `opaqueSession`) " +
                        "SELECT `id`, `uuid`, `accountId`, `credentials`, `opaqueSession` " +
                        "FROM `user`",
                )
                db.execSQL("DROP TABLE `user`")
                db.execSQL("ALTER TABLE `user_new` RENAME TO `user`")
            }
        }
    }
}
