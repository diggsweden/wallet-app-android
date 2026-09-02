// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.extensions.toECKey

@Serializable
private data class Payload(val nonce: String, val htu: String)

class JwtUtilsTest {

    private val payload = Payload(nonce = "nonce-1", htu = "https://example.test/token")

    private fun keyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    @Test
    fun `signJwt produces a verifiable ES256 JWT with default time claims`() {
        val keyPair = keyPair()

        val jwt = JwtUtils.signJwt(
            keyPair = keyPair,
            payload = payload,
            headers = mapOf("typ" to "dpop+jwt"),
        )

        assertEquals("ES256", jwt.header.algorithm.name)
        assertEquals("dpop+jwt", jwt.header.getCustomParam("typ"))
        assertNull(jwt.header.jwk)

        val claims = jwt.jwtClaimsSet
        assertEquals("nonce-1", claims.getClaim("nonce"))
        assertEquals("https://example.test/token", claims.getClaim("htu"))
        // iat/nbf/exp are registered claims, so they parse back as Dates.
        assertEquals(claims.issueTime, claims.notBeforeTime)
        assertEquals(
            600L,
            (claims.expirationTime.time - claims.issueTime.time) / 1_000,
        )
    }

    @Test
    fun `signJwt can embed the public key in the protected header`() {
        val keyPair = keyPair()

        val jwt = JwtUtils.signJwt(
            keyPair = keyPair,
            payload = payload,
            headers = emptyMap(),
            includeJwk = true,
        )

        assertEquals(keyPair.toECKey().toJSONString(), jwt.header.jwk.toJSONString())
        assertTrue(!jwt.header.jwk.isPrivate)
    }

    @Test
    fun `a signed jwt round trips through its compact serialization`() {
        val jwt = JwtUtils.signJwt(keyPair(), payload, mapOf("kid" to "kid-1"))

        val parsed = SignedJWT.parse(jwt.serialize())

        assertEquals("kid-1", parsed.header.keyID)
        assertEquals("nonce-1", parsed.jwtClaimsSet.getClaim("nonce"))
    }

    @Test
    fun `signJwtWith delegates the signature to the supplied signer`() = runTest {
        val captured = mutableListOf<ByteArray>()
        val jwk = ECKeyGenerator(Curve.P_256).generate().toPublicJWK()

        val compact = JwtUtils.signJwtWith(
            payload = payload,
            headers = mapOf("typ" to "openid4vci-proof+jwt"),
            jwk = jwk,
        ) { input ->
            captured += input
            "c2lnbmF0dXJl"
        }

        val (header, body, signature) = compact.split(".")
        assertEquals("c2lnbmF0dXJl", signature)
        assertEquals("$header.$body", captured.single().toString(Charsets.US_ASCII))
        assertEquals(
            jwk.toJSONString(),
            SignedJWT.parse("$header.$body.${Base64URL.encode(ByteArray(64))}")
                .header.jwk.toJSONString(),
        )
    }

    @Test
    fun `signJwtWith omits the jwk header when no key is given`() = runTest {
        val compact = JwtUtils.signJwtWith(
            payload = payload,
            headers = mapOf("kid" to "kid-1"),
        ) { "c2ln" }

        val header = compact.substringBefore('.')
        val decoded = Base64URL(header).decodeToString()

        assertTrue(!decoded.contains("\"jwk\""))
        assertTrue(decoded.contains("\"kid\":\"kid-1\""))
    }

    @Test
    fun `a JWE round trips through the recipient key`() {
        val recipient = keyPair()

        val compact = JwtUtils.encryptJwe(
            payload = payload,
            recipientKey = recipient.toECKey(),
            encryptionMethod = EncryptionMethod.A128GCM,
        )

        assertEquals(EncryptionMethod.A128GCM, JWEObject.parse(compact).header.encryptionMethod)
        assertEquals(payload, JwtUtils.decryptJwe<Payload>(compact, recipient))
    }

    @Test
    fun `decryptJwe ignores members the payload does not declare`() {
        val recipient = keyPair()
        val compact = JwtUtils.encryptJwe(
            payload = mapOf("nonce" to "nonce-1", "htu" to "https://a.test", "extra" to "x"),
            recipientKey = recipient.toECKey(),
            encryptionMethod = EncryptionMethod.A256GCM,
        )

        assertEquals(
            Payload("nonce-1", "https://a.test"),
            JwtUtils.decryptJwe<Payload>(compact, recipient),
        )
    }
}
