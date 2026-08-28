// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.core.storage.user.UserDao

/** In-memory stand-in for the Room DAO's single-row user table. */
class FakeUserDao(initial: User? = null) : UserDao {

    private val state = MutableStateFlow(initial)

    var clearCount: Int = 0
        private set

    val current: User? get() = state.value

    override suspend fun upsert(user: User) {
        state.value = user
    }

    override fun observe(): Flow<User?> = state

    override suspend fun get(): User? = state.value

    override suspend fun clear() {
        clearCount++
        state.value = null
    }
}
