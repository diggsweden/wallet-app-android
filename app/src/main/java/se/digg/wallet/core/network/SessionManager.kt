package se.digg.wallet.core.network

import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

class SessionManager(
    val challengeClient: PublicAuthSessionChallengeClient,
    val validateClient: PublicAuthSessionResponseClient,
    val accountId: String
) {
    private var sessionToken: String? = null

    suspend fun getToken(): String = sessionToken ?: initSession()

    suspend fun initSession(): String {
        // Get keyId

        val nonce = getChallenge(accountId, "keyId")
        // TODO: Validate nonce
        sessionToken = nonce
        return nonce
    }

    suspend fun getChallenge(accountId: String, keyId: String): String {
        return when (val result = challengeClient.initChallenge(accountId, keyId)) {
            is PublicAuthSessionChallengeClient.InitChallengeResult.Failure ->
                throw Exception("OH NO")

            is PublicAuthSessionChallengeClient.InitChallengeResult.Success ->
                result.data.nonce ?: ""
        }
    }
}
