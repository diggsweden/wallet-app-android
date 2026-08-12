// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DpopProofBuilderTest {

    private val builder = DpopProofBuilder()
    private val endpoint = Url("https://issuer.example.com/credential")

    private suspend fun proof(
        endpoint: Url = this.endpoint,
        method: HttpMethod = HttpMethod.Post,
        accessToken: String? = "some-access-token",
        nonce: String? = null,
    ): SignedJWT = SignedJWT.parse(
        builder.proof(
            endpoint = endpoint,
            method = method,
            accessToken = accessToken,
            nonce = nonce,
        ),
    )

    @Test
    fun `header declares the dpop type and ES256`() = runTest {
        val header = proof().header

        assertEquals("dpop+jwt", header.type.toString())
        assertEquals(JWSAlgorithm.ES256, header.algorithm)
    }

    @Test
    fun `header carries the public key and no private parameters`() = runTest {
        val jwk = proof().header.jwk

        assertFalse(jwk.isPrivate)
        assertNull(jwk.toJSONObject()["d"])
    }

    @Test
    fun `the proof verifies with the key it advertises`() = runTest {
        val jwt = proof()

        assertTrue(jwt.verify(ECDSAVerifier(jwt.header.jwk.toECKey())))
    }

    @Test
    fun `every proof gets a fresh jti`() = runTest {
        val first = proof().jwtClaimsSet.jwtid
        val second = proof().jwtClaimsSet.jwtid

        assertNotEquals(first, second)
    }

    @Test
    fun `htm is the uppercase request method`() = runTest {
        val claims = proof(method = HttpMethod.Get).jwtClaimsSet

        assertEquals("GET", claims.getStringClaim("htm"))
    }

    @Test
    fun `htu drops query and fragment`() = runTest {
        val claims = proof(
            endpoint = Url("https://issuer.example.com/credential?foo=bar#section"),
        ).jwtClaimsSet

        assertEquals("https://issuer.example.com/credential", claims.getStringClaim("htu"))
    }

    @Test
    fun `htu keeps a non-default port and the path`() = runTest {
        val claims = proof(
            endpoint = Url("https://issuer.example.com:8443/pid-issuer/credential?x=1"),
        ).jwtClaimsSet

        assertEquals(
            "https://issuer.example.com:8443/pid-issuer/credential",
            claims.getStringClaim("htu"),
        )
    }

    @Test
    fun `htu leaves a url without query or fragment unchanged`() = runTest {
        val claims = proof().jwtClaimsSet

        assertEquals("https://issuer.example.com/credential", claims.getStringClaim("htu"))
    }

    @Test
    fun `ath is the base64url sha256 of the access token`() = runTest {
        val claims = proof(accessToken = "some-access-token").jwtClaimsSet

        assertEquals(
            "CRLvO23C6lecaPrHhPjC3ZQu3FiSgIydavbmtHEV0SY",
            claims.getStringClaim("ath"),
        )
    }

    @Test
    fun `ath is absent when the request has no access token`() = runTest {
        val claims = proof(accessToken = null).jwtClaimsSet

        assertNull(claims.getStringClaim("ath"))
    }

    @Test
    fun `nonce is present only when the server supplied one`() = runTest {
        val without = proof().jwtClaimsSet
        val with = proof(nonce = "nonce-abc").jwtClaimsSet

        assertNull(without.getStringClaim("nonce"))
        assertEquals("nonce-abc", with.getStringClaim("nonce"))
    }

    @Test
    fun `iat is issued`() = runTest {
        val claims = proof().jwtClaimsSet

        assertTrue(claims.issueTime.time > 0)
    }
}
