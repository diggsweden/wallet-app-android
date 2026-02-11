package se.digg.wallet.data

import io.ktor.client.HttpClient
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.OidcAccountsV1Client
import se.wallet.client.gateway.client.WuaV3Client
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.CreateAccountResponseDto
import se.wallet.client.gateway.models.WuaDto

class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val gatewayClient: HttpClient,
) {
    val user: Flow<User?> = userDao.observe()
    val accountsClient = OidcAccountsV1Client(gatewayClient)
    val wuaClient = WuaV3Client(gatewayClient)
    private var sessionId: String? = null

    suspend fun fetchWua(nonce: String? = null): NetworkResult<WuaDto> =
        wuaClient.createWua1(nonce = nonce)

    suspend fun createAccount(
        request: CreateAccountRequestDto,
    ): NetworkResult<CreateAccountResponseDto> {
        val session = sessionId ?: throw IllegalStateException("SessionId is null")
        return accountsClient.createAccount(createAccountRequestDto = request, sESSION = session)
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
    fun setSessionId(id: String) {
        sessionId = id
    }

    suspend fun wipeAll() = userDao.clear()

    private suspend inline fun updateUser(crossinline transform: (User) -> User) {
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
