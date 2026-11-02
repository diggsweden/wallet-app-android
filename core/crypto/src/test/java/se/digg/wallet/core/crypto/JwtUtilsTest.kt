// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JwtUtilsTest {

    @Serializable
    private data class CustomHeader(
        override val typ: String? = "JWT",
        override val kid: String? = "device-key-id",
        override val jwk: JsonObject? = null,
    ) : JwtHeader

    @Serializable
    private data class SamplePayload(val sub: String, val challenge: String)

    private val ecKey = ECKeyGenerator(Curve.P_256).generate()
    private val keyPair = ecKey.toKeyPair()

    @Test
    fun `signJwt produces valid ES256 SignedJWT containing header, defaults, and payload`() {
        val header = CustomHeader()
        val payload = SamplePayload(sub = "user-123", challenge = "nonce-abc")

        val signedJwt = JwtUtils.signJwt(keyPair, header, payload)

        // Header checks
        assertEquals(JWSAlgorithm.ES256, signedJwt.header.algorithm)
        assertEquals("device-key-id", signedJwt.header.keyID)
        assertEquals("JWT", signedJwt.header.type.toString())

        // Claims checks
        val claims = signedJwt.jwtClaimsSet
        assertEquals("user-123", claims.getStringClaim("sub"))
        assertEquals("nonce-abc", claims.getStringClaim("challenge"))
        assertNotNull(claims.issueTime)
        assertNotNull(claims.notBeforeTime)
        assertNotNull(claims.expirationTime)

        // Expiration is 600s after issue time
        val expDiffSeconds = (claims.expirationTime.time - claims.issueTime.time) / 1000
        assertEquals(600L, expDiffSeconds)

        // Cryptographic verification
        val verifier = ECDSAVerifier(ecKey.toECPublicKey())
        assertTrue(signedJwt.verify(verifier))
    }

    @Test
    fun `signJwtWith delegates signature to external signer and produces compact JWS`() = runTest {
        val header = CustomHeader(kid = "hsm-key-id")
        val payload = SamplePayload(sub = "user-hsm", challenge = "challenge-xyz")

        var signingInputCaptured: ByteArray? = null
        val token = JwtUtils.signJwtWith(header, payload) { input ->
            signingInputCaptured = input
            "base64url-fake-signature"
        }

        assertNotNull(signingInputCaptured)
        val parts = token.split(".")
        assertEquals(3, parts.size)
        assertEquals("base64url-fake-signature", parts[2])
    }

    @Test
    fun `encryptJwe and decryptJwe successfully roundtrip payload using ECDH`() {
        val recipientJwk = ECKeyGenerator(Curve.P_256).generate()
        val recipientKeyPair = recipientJwk.toKeyPair()
        val payload = SamplePayload(sub = "confidential-subject", challenge = "secure-seed")

        val jweString = JwtUtils.encryptJwe(
            payload = payload,
            recipientKey = recipientJwk,
            encryptionMethod = EncryptionMethod.A128GCM,
            algorithm = JWEAlgorithm.ECDH_ES,
        )

        // Compact JWE has 5 parts: header.encryptedKey.iv.ciphertext.authTag
        val parts = jweString.split(".")
        assertEquals(5, parts.size)

        // Decrypt
        val decrypted = JwtUtils.decryptJwe<SamplePayload>(
            compactString = jweString,
            decryptionKeyPair = recipientKeyPair,
        )

        assertEquals("confidential-subject", decrypted.sub)
        assertEquals("secure-seed", decrypted.challenge)
    }
}
