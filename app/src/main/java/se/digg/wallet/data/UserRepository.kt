package se.digg.wallet.data

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.AccountsV1Client
import se.wallet.client.gateway.client.AccountsV1Client.CreateAccountResult
import se.wallet.client.gateway.client.WuaV2Client
import se.wallet.client.gateway.client.WuaV2Client.CreateWua_1Result
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.CreateWuaDto
import java.util.UUID
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val gatewayClient: HttpClient
) {
    val user: Flow<User?> = userDao.observe()
    val accountsClient = AccountsV1Client(gatewayClient)
    val wuaClient = WuaV2Client(gatewayClient)

    suspend fun fetchWua(request: CreateWuaDto): CreateWua_1Result {
        return wuaClient.createWua_1(request)
    }

    suspend fun createAccount(request: CreateAccountRequestDto): CreateAccountResult {
        return accountsClient.createAccount(request)
    }

    suspend fun getPin(): String? = userDao.get()?.pin
    suspend fun getUuid(): UUID? = userDao.get()?.uuid
    suspend fun getAccountId(): String? = userDao.get()?.accountId
    suspend fun getWua(): String? = userDao.get()?.wua
    suspend fun getCredential(): String? = userDao.get()?.credential
    suspend fun getEmail(): String? = userDao.get()?.email
    suspend fun getPhone(): String? = userDao.get()?.phone

    suspend fun setPin(pin: String) = updateUser { it.copy(pin = pin) }
    suspend fun setUuid(uuid: UUID) = updateUser { it.copy(uuid = uuid) }
    suspend fun setAccountId(accountId: String?) = updateUser { it.copy(accountId = accountId) }
    suspend fun setWua(wua: String) = updateUser { it.copy(wua = wua) }
    suspend fun setCredential(credential: String) = updateUser { it.copy(credential = credential) }
    suspend fun setEmail(email: String) = updateUser { it.copy(email = email) }
    suspend fun setPhone(phone: String) = updateUser { it.copy(phone = phone) }

    suspend fun wipeAll() = userDao.clear()

    private suspend inline fun updateUser(
        crossinline transform: (User) -> User
    ) {
        val current = userDao.get() ?: User(
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
        userDao.upsert(next)
    }


}