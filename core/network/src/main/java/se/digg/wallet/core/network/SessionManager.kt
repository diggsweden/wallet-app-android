// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import se.digg.wallet.core.extensions.getOrThrow
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import se.wallet.client.gateway.models.AuthChallengeRequest

class SessionManager(
    val challengeClient: PublicAuthSessionChallengeClient,
    val validateClient: PublicAuthSessionResponseClient,
    val authenticator: DeviceAuthenticator,
) {
    private var sessionToken: String? = null

    suspend fun getToken(): String = sessionToken ?: initSession()

    fun reset() {
        sessionToken = null
    }

    suspend fun initSession(): String {
        val accountId = authenticator.getAccountId()
        val keyId = authenticator.getDeviceKeyId()
        val nonce = getChallenge(accountId, keyId)
        val jwt = authenticator.signChallenge(nonce)
        val sessionToken = validateClient.validateChallenge(
            AuthChallengeRequest(signedJwt = jwt),
        ).getOrThrow().sessionId

        this.sessionToken = sessionToken
        return sessionToken
    }

    suspend fun getChallenge(accountId: String, keyId: String): String =
        challengeClient.initChallenge(accountId, keyId).getOrThrow().nonce ?: ""
}
