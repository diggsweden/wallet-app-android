package se.digg.wallet.core.network

import java.security.KeyPair
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import se.wallet.client.gateway.models.ValidateAuthChallengeRequestDto

class SessionManager(
    val challengeClient: PublicAuthSessionChallengeClient,
    val validateClient: PublicAuthSessionResponseClient,
    val userDao: UserDao,
) {
    private var sessionToken: String? = null

    suspend fun getToken(): String = sessionToken ?: initSession()

    fun reset() {
        sessionToken = null
    }

    suspend fun initSession(): String {
        val accountId = userDao.get()?.accountId ?: throw Exception("No account")
        val key = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
        val keyId = JwtUtils.exportJwk(key).keyID
        val nonce = getChallenge(accountId, keyId)
        val sessionToken = validateChallenge(keyId = keyId, key = key, nonce = nonce)

        this.sessionToken = sessionToken
        return sessionToken
    }

    suspend fun getChallenge(accountId: String, keyId: String): String =
        when (val result = challengeClient.initChallenge(accountId, keyId)) {
            is NetworkResult.Failure -> {
                throw Exception("Failed getting challenge")
            }

            is NetworkResult.Success -> {
                result.data.nonce ?: ""
            }
        }

    suspend fun validateChallenge(keyId: String, key: KeyPair, nonce: String): String {
        val jwt =
            JwtUtils.signJWT(
                keyPair = key,
                payload = mapOf("nonce" to nonce),
                headers = mapOf("kid" to keyId),
            )
        val result = validateClient.validateChallenge(
            ValidateAuthChallengeRequestDto(signedJwt = jwt),
        )
        return when (result) {
            is NetworkResult.Failure -> {
                throw Exception("Failed validating challenge")
            }

            is NetworkResult.Success -> {
                result.data.sessionId
            }
        }
    }
}
