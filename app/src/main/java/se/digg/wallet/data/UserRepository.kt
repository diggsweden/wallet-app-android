// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import io.ktor.client.HttpClient
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.AccountsClient
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.WuaClient
import se.wallet.client.gateway.models.CreateAccountRequestDto
import se.wallet.client.gateway.models.CreateAccountResponseDto

class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val gatewayClient: HttpClient,
) {
    val user: Flow<User?> = userDao.observe()
    val accountsClient = AccountsClient(gatewayClient)
    val wuaClient = WuaClient(gatewayClient)

    suspend fun fetchWua(nonce: String? = null): String =
        when (val response = wuaClient.createWua(nonce = nonce)) {
            is NetworkResult.Failure -> {
                throw IllegalStateException(
                    "Wallet Unit Attestation (WUA) is missing",
                )
            }

            is NetworkResult.Success -> {
                return response.data.jwt
            }
        }

    suspend fun createAccount(
        request: CreateAccountRequestDto,
    ): NetworkResult<CreateAccountResponseDto> =
        accountsClient.createAccount1(createAccountRequestDto = request)

    suspend fun isOnboarded() = !(getPid() == null || getAccountId() == null)
    suspend fun getPin(): String? = userDao.get()?.pin
    suspend fun getPid(): SavedCredential? = userDao.get()?.pid
    suspend fun getCredentials(): List<SavedCredential> = userDao.get()?.credentials ?: emptyList()
    suspend fun getCredential(id: String): SavedCredential {
        val user = checkNotNull(userDao.get())
        val matchingCredential =
            listOfNotNull(user.pid, *user.credentials.toTypedArray()).firstOrNull { it.id == id }
        checkNotNull(value = matchingCredential) {
            "Cant find credential matching id"
        }
        return matchingCredential
    }

    suspend fun getEmail(): String? = userDao.get()?.email
    suspend fun getPhone(): String? = userDao.get()?.phone
    suspend fun getAccountId(): String? = userDao.get()?.accountId

    suspend fun setPin(pin: String) = updateUser { it.copy(pin = pin) }
    suspend fun setUuid(uuid: UUID) = updateUser { it.copy(uuid = uuid) }
    suspend fun setAccountId(accountId: String?) = updateUser { it.copy(accountId = accountId) }
    suspend fun setPid(credential: SavedCredential) {
        check(credential.type == CredentialType.PID.type) {
            "Invalid PID credential"
        }
        updateUser { it.copy(pid = credential) }
    }

    suspend fun addCredentials(credentials: List<SavedCredential>) =
        updateUser { it.copy(credentials = it.credentials + credentials) }

    suspend fun setEmail(email: String) = updateUser { it.copy(email = email) }
    suspend fun setPhone(phone: String) = updateUser { it.copy(phone = phone) }

    suspend fun wipeAll() = userDao.clear()

    private suspend inline fun updateUser(crossinline transform: (User) -> User) {
        val current = userDao.get() ?: User(
            id = 0,
            pin = null,
            email = null,
            phone = null,
            uuid = null,
            accountId = null,
            pid = null,
            credentials = emptyList(),
        )
        val next = transform(current)
        userDao.upsert(next)
    }
}
