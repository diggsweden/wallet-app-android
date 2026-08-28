// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import eu.europa.ec.eudi.openid4vci.AccessToken
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.network.DpopProofProvider
import se.digg.wallet.core.network.RequestAuthorization

private object StubProofProvider : DpopProofProvider {
    override suspend fun proof(
        endpoint: Url,
        method: HttpMethod,
        accessToken: String?,
        nonce: String?,
    ): String = "proof"
}

class AccessTokenExtensionsTest {

    private val proofProvider: DpopProofProvider = StubProofProvider

    @Test
    fun `a DPoP access token maps to DPoP authorization with the proof provider`() {
        val authorization =
            AccessToken.DPoP("dpop-token", expiresIn = null).toRequestAuthorization(proofProvider)

        assertTrue(authorization is RequestAuthorization.Dpop)
        authorization as RequestAuthorization.Dpop
        assertEquals("dpop-token", authorization.accessToken)
        assertSame(proofProvider, authorization.proofProvider)
    }

    @Test
    fun `a bearer access token maps to bearer authorization`() {
        val authorization = AccessToken.Bearer("bearer-token", expiresIn = null)
            .toRequestAuthorization(proofProvider)

        assertTrue(authorization is RequestAuthorization.Bearer)
        assertEquals("bearer-token", (authorization as RequestAuthorization.Bearer).accessToken)
    }
}
