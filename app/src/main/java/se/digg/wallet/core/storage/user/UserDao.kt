// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: User)

    @Query("SELECT * FROM user WHERE id = 0")
    fun observe(): Flow<User?>

    @Query("SELECT * FROM user WHERE id = 0")
    suspend fun get(): User?

    @Query("DELETE FROM user") // wipes the table
    suspend fun clear()
}