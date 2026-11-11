// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.core.crypto.KeystoreManager
import se.digg.wallet.core.database.OpaqueSession
import se.digg.wallet.core.database.User
import se.digg.wallet.core.database.UserDao
import se.digg.wallet.core.model.SavedCredential
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.network.di.GatewayHttpClient
import se.digg.wallet.core.network.extensions.getOrThrow
import se.digg.wallet.core.network.extensions.toEcJwkRequest
import se.digg.wallet.core.network.extensions.toEcJwkRequests
import se.wallet.client.gateway.client.AttestationV0KeyAttestationsClient
import se.wallet.client.gateway.client.AttestationV0WalletInstanceAttestationsClient
import se.wallet.client.gateway.client.UtilV0ClientConfigsClient
import se.wallet.client.gateway.client.V0AccountsClient
import se.wallet.client.gateway.models.ClientConfigResponse
import se.wallet.client.gateway.models.CreateAccountRequest
import se.wallet.client.gateway.models.KeyAttestationRequest
import se.wallet.client.gateway.models.WalletInstanceAttestationRequest

@Singleton
class UserRepository(
    private val userDao: UserDao,
    gatewayClient: HttpClient,
    private val sessionManager: SessionManager,
    private val wipeKeystore: () -> Unit = { KeystoreManager.removeKey() },
) : CredentialStore,
    KeyAttestationProvider,
    WalletInstanceAttestationProvider,
    AccountManager {

    @Inject
    constructor(
        userDao: UserDao,
        @GatewayHttpClient gatewayClient: HttpClient,
        sessionManager: SessionManager,
    ) : this(
        userDao = userDao,
        gatewayClient = gatewayClient,
        sessionManager = sessionManager,
        wipeKeystore = { KeystoreManager.removeKey() },
    )

    val user: Flow<User?> = userDao.observe()
    override val credentials: Flow<List<SavedCredential>> = user.map { it?.credentials.orEmpty() }
    private val accountsClient = V0AccountsClient(gatewayClient)
    private val attestationsClient = AttestationV0KeyAttestationsClient(gatewayClient)
    private val walletInstanceAttestationsClient =
        AttestationV0WalletInstanceAttestationsClient(gatewayClient)
    private val clientConfigsClient = UtilV0ClientConfigsClient(gatewayClient)

    override suspend fun getKeyAttestation(keys: List<ECKey>, nonce: String?): String =
        attestationsClient.createKeyAttestation(
            KeyAttestationRequest(
                keys = keys.toEcJwkRequests(),
                nonce = nonce,
            ),
        ).getOrThrow().jwt

    override suspend fun getWalletInstanceAttestation(key: ECKey): String =
        walletInstanceAttestationsClient.createWalletInstanceAttestation(
            WalletInstanceAttestationRequest(key = key.toEcJwkRequest()),
        ).getOrThrow().jwt

    suspend fun createAccount(request: CreateAccountRequest): String =
        accountsClient.createAccount(createAccountRequest = request).getOrThrow().accountId

    suspend fun getClientConfig(): ClientConfigResponse =
        clientConfigsClient.getClientConfig().getOrThrow()

    suspend fun getUser(): User? = userDao.get()

    override suspend fun getCredentials(): List<SavedCredential> =
        userDao.get()?.credentials ?: emptyList()

    override suspend fun getCredential(id: String): SavedCredential {
        val matchingCredential = getCredentials().firstOrNull { it.id == id }
        checkNotNull(matchingCredential) {
            "Cant find credential matching id"
        }
        return matchingCredential
    }

    suspend fun setAccountId(accountId: String?) = updateUser { it.copy(accountId = accountId) }

    suspend fun setBackendGeneration(backendGeneration: Int) =
        updateUser { it.copy(backendGeneration = backendGeneration) }

    override suspend fun addCredentials(credentials: List<SavedCredential>) =
        updateUser { it.copy(credentials = it.credentials + credentials) }

    suspend fun saveServerParameters(params: ServerParameters) {
        val jwk = ECKey.Builder(Curve.P_256, params.serverPublicKey).build().toJSONString()
        updateUser {
            it.copy(
                opaqueSession = OpaqueSession(
                    serverPublicKeyJwk = jwk,
                    opaqueServerId = params.opaqueServerId,
                    opaqueContext = params.opaqueContext,
                ),
            )
        }
    }

    suspend fun getServerParameters(): ServerParameters {
        val session = checkNotNull(userDao.get()?.opaqueSession) {
            "User was null. Should never be null."
        }
        val publicKey = ECKey.parse(session.serverPublicKeyJwk).toECPublicKey()
        return ServerParameters(
            serverPublicKey = publicKey,
            opaqueServerId = session.opaqueServerId,
            opaqueContext = session.opaqueContext,
        )
    }

    override suspend fun wipeAll() {
        wipeKeystore()
        sessionManager.reset()
        userDao.clear()
    }

    private suspend inline fun updateUser(crossinline transform: (User) -> User) {
        val current = userDao.get() ?: User(
            id = 0,
            accountId = null,
            credentials = emptyList(),
        )
        val next = transform(current)
        userDao.upsert(next)
    }
}
