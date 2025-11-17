package se.digg.wallet.core.storage.user

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(private val dao: UserDao) {

    val user: Flow<User?> = dao.observe()

    suspend fun getPin(): String? = dao.get()?.pin
    suspend fun getUuid(): UUID? = dao.get()?.uuid
    suspend fun getAccountId(): String? = dao.get()?.accountId
    suspend fun getWua(): String? = dao.get()?.wua
    suspend fun getCredential(): String? = dao.get()?.credential

    suspend fun setPin(pin: String) = updateUser { it.copy(pin = pin) }
    suspend fun setUuid(uuid: UUID) = updateUser { it.copy(uuid = uuid) }
    suspend fun setAccountId(accountId: String?) = updateUser { it.copy(accountId = accountId) }
    suspend fun setWua(wua: String) = updateUser { it.copy(wua = wua) }
    suspend fun setCredential(credential: String) = updateUser { it.copy(credential = credential) }

    suspend fun wipeAll() = dao.clear()

    private suspend inline fun updateUser(
        crossinline transform: (User) -> User
    ) {
        val current = dao.get() ?: User(
            id = 0,
            pin = null,
            email = null,
            phone = null,
            uuid = null,
            accountId = null,
            wua = null,
            credential = null,
        )
        val next = transform(current)
        dao.upsert(next)
    }
}