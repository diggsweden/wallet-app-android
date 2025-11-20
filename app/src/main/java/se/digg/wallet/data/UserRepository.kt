package se.digg.wallet.data

import kotlinx.coroutines.flow.Flow
import okhttp3.RequestBody
import se.digg.wallet.core.network.ApiService
import se.digg.wallet.core.network.NonceResponseModel
import se.digg.wallet.core.network.PresentationResponseModel
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.core.storage.user.UserDao
import java.util.UUID
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) {
    val user: Flow<User?> = userDao.observe()

    suspend fun fetchCredential(
        url: String,
        accessToken: String,
        request: OldCredentialRequestModel
    ): CredentialResponseModel {
        return apiService.getOldCredential(
            url = url,
            accessToken = accessToken,
            request = request
        )
    }

    suspend fun fetchWua(request: WuaRequestModel): WuaResponseModel {
        return apiService.getWuaRequest(request = request)
    }

    suspend fun fetchNonce(url: String): NonceResponseModel {
        return apiService.getNonce(url = url)
    }

    suspend fun postVpToken(url: String, request: RequestBody): PresentationResponseModel {
        return apiService.postVpToken(url = url, request = request)
    }

    suspend fun createAccount(request: CreateAccountRequestDTO): CreateAccountResponseDTO {
        return apiService.createAccount(request = request)
    }

    suspend fun getPin(): String? = userDao.get()?.pin
    suspend fun getUuid(): UUID? = userDao.get()?.uuid
    suspend fun getAccountId(): String? = userDao.get()?.accountId
    suspend fun getWua(): String? = userDao.get()?.wua
    suspend fun getCredential(): String? = userDao.get()?.credential

    suspend fun setPin(pin: String) = updateUser { it.copy(pin = pin) }
    suspend fun setUuid(uuid: UUID) = updateUser { it.copy(uuid = uuid) }
    suspend fun setAccountId(accountId: String?) = updateUser { it.copy(accountId = accountId) }
    suspend fun setWua(wua: String) = updateUser { it.copy(wua = wua) }
    suspend fun setCredential(credential: String) = updateUser { it.copy(credential = credential) }

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