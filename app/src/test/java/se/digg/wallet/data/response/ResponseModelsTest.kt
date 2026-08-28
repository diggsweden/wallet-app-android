// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data.response

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.data.CredentialsOfferRequestModel
import se.digg.wallet.data.CredentialsOfferResponseModel
import se.digg.wallet.feature.issuance.IssuanceProofPayload

class ResponseModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `NonceResponseModel reads the c_nonce wire name`() {
        val model = json.decodeFromString<NonceResponseModel>("""{"c_nonce":"nonce-1"}""")

        assertEquals("nonce-1", model.nonce)
        assertTrue(json.encodeToString(model).contains("\"c_nonce\":\"nonce-1\""))
    }

    @Test
    fun `PresentationResponseModel reads the redirect_uri wire name`() {
        val model = json.decodeFromString<PresentationResponseModel>(
            """{"redirect_uri":"https://verifier.example.test/done"}""",
        )

        assertEquals("https://verifier.example.test/done", model.redirectUri)
        assertNull(json.decodeFromString<PresentationResponseModel>("{}").redirectUri)
    }

    @Test
    fun `the credentials offer request and response round trip`() {
        val request = CredentialsOfferRequestModel(credentialIds = listOf("pid", "mdl"))

        assertEquals(
            request,
            json.decodeFromString<CredentialsOfferRequestModel>(json.encodeToString(request)),
        )
        assertEquals(
            "openid-credential-offer://offer",
            json
                .decodeFromString<CredentialsOfferResponseModel>(
                    """{"credentialsOffer":"openid-credential-offer://offer"}""",
                )
                .credentialsOffer,
        )
        assertNull(json.decodeFromString<CredentialsOfferResponseModel>("{}").credentialsOffer)
    }

    @Test
    fun `IssuanceProofPayload allows a missing nonce`() {
        val payload = IssuanceProofPayload(
            aud = "https://issuer.example.test",
            nonce = null,
            iss = "wallet",
        )

        val decoded = json.decodeFromString<IssuanceProofPayload>(json.encodeToString(payload))

        assertEquals(payload, decoded)
        assertNull(decoded.nonce)
    }
}
