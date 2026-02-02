// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [User::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
