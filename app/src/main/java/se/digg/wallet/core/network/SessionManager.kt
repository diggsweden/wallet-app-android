package se.digg.wallet.core.network

import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.storage.user.UserDao
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import se.wallet.client.gateway.models.AuthChallengeResponseDto
import java.security.KeyPair

class SessionManager(
    val challengeClient: PublicAuthSessionChallengeClient,
    val validateClient: PublicAuthSessionResponseClient,
    val userDao: UserDao
) {
    private var sessionToken: String? = null

    suspend fun getToken(): String = sessionToken ?: initSession()

    fun reset() {
        sessionToken = null
    }

    suspend fun initSession(): String {
        val accountId = userDao.get()?.accountId ?: throw Exception("No account")
        val keyId = "myKey"
        val key = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
        val nonce = getChallenge(accountId, keyId)
        val sessionToken = validateChallenge(keyId = keyId, key = key, nonce = nonce)

        this.sessionToken = sessionToken
        return sessionToken
    }

    suspend fun getChallenge(accountId: String, keyId: String): String {
        return when (val result = challengeClient.initChallenge(accountId, keyId)) {
            is PublicAuthSessionChallengeClient.InitChallengeResult.Failure -> {
                throw Exception("Failed getting challenge")
            }

            is PublicAuthSessionChallengeClient.InitChallengeResult.Success -> {
                result.data.nonce ?: ""
            }
        }
    }

    suspend fun validateChallenge(keyId: String, key: KeyPair, nonce: String): String {
        val jwt = KeystoreManager.createJWT(
            keyPair = key,
            payload = mapOf("nonce" to nonce),
            headers = mapOf("kid" to keyId)
        )
        val result = validateClient.validateChallenge(AuthChallengeResponseDto(signedJwt = jwt))
        return when (result) {
            is PublicAuthSessionResponseClient.ValidateChallengeResult.Failure -> {
                throw Exception("Failed validating challenge")
            }

            is PublicAuthSessionResponseClient.ValidateChallengeResult.Success -> {
                result.response.headers["session"] ?: throw Exception("Could not get session ID")

            }
        }
    }
}
