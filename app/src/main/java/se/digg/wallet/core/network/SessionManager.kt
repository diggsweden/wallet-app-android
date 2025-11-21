package se.digg.wallet.core.network

import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import java.security.KeyPair

class SessionManager(
    val challengeClient: PublicAuthSessionChallengeClient,
    val validateClient: PublicAuthSessionResponseClient,
    val accountId: String
) {
    private var sessionToken: String? = null
    private val keyStoreManager = se.digg.wallet.feature.issuance.KeystoreManager

    suspend fun getToken(): String = sessionToken ?: initSession()

    suspend fun initSession(): String {
        val keyId = "bolibompa"
        val key = keyStoreManager.getOrCreateEs256Key("device_key")
        val jwk = keyStoreManager.exportJwk(keyId, key)

        return "secretToken"
//        val nonce = getChallenge(accountId, keyId)
//        val sessionToken = validateChallenge(keyId = keyId, key = key, nonce = nonce)
//        this.sessionToken = sessionToken
//        return sessionToken
    }

    suspend fun getChallenge(accountId: String, keyId: String): String {
        return when (val result = challengeClient.initChallenge(accountId, keyId)) {
            is PublicAuthSessionChallengeClient.InitChallengeResult.Failure -> {
                throw Exception("OH NO")
            }

            is PublicAuthSessionChallengeClient.InitChallengeResult.Success -> {
                result.data.nonce ?: ""
            }
        }
    }

    suspend fun validateChallenge(keyId: String, key: KeyPair, nonce: String): String {
        val jwt = keyStoreManager.createJWT(
            keyPair = key,
            payload = mapOf("nonce" to nonce),
            headers = mapOf("kid" to keyId)
        )

        return when (val result = validateClient.validateChallenge("signedJwt: $jwt")) {
            is PublicAuthSessionResponseClient.ValidateChallengeResult.Failure -> {
                throw Exception("Oh no")
            }

            is PublicAuthSessionResponseClient.ValidateChallengeResult.Success -> {
                result.response.headers["JSESSION"] ?: ""
            }
        }
    }
}
