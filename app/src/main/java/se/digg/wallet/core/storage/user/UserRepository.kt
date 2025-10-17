package se.digg.wallet.core.storage.user

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(private val dao: UserDao) {

    val user: Flow<User?> = dao.observe()

    suspend fun getPin(): String? = dao.get()?.pin
    suspend fun getEmail(): String? = dao.get()?.email
    suspend fun getUuid(): UUID? = dao.get()?.uuid
    suspend fun getWua(): String? = dao.get()?.wua
    suspend fun setPin(pin: String) = updateUser { it.copy(pin = pin) }
    suspend fun setEmail(email: String?) = updateUser { it.copy(email = email) }
    suspend fun setUuid(uuid: UUID) = updateUser { it.copy(uuid = uuid) }
    suspend fun setWua(wua: String) = updateUser { it.copy(wua = wua) }

    suspend fun wipeAll() = dao.clear()

    private suspend inline fun updateUser(
        crossinline transform: (User) -> User
    ) {
        val current = dao.get() ?: User(
            id = 0,
            pin = null,
            email = null,
            uuid = null,
            wua = null
        )
        val next = transform(current)
        dao.upsert(next)
    }
}