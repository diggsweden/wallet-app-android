// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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
    private val certificate = selfSignedCertificate()

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

    @Test
    fun `a jws of the expected type is verified`() {
        val jws = signedJws(type = "rc-wrp+jwt")

        val payload = JwtUtils.verifyJws<SamplePayload>(jws, expectedType = "rc-wrp+jwt") {
            it.first().publicKey
        }

        assertEquals(SamplePayload(sub = "user-123", challenge = "nonce-abc"), payload)
    }

    @Test
    fun `a jws of another type is rejected`() {
        val jws = signedJws(type = "JWT")

        val thrown = assertThrows(IllegalStateException::class.java) {
            JwtUtils.verifyJws<SamplePayload>(jws, expectedType = "rc-wrp+jwt") {
                it.first().publicKey
            }
        }

        assertEquals(
            "Invalid JWS header type, expected: rc-wrp+jwt, got: JWT",
            thrown.message,
        )
    }

    @Test
    fun `a jws without a type is rejected when a type is expected`() {
        val jws = signedJws(type = null)

        val thrown = assertThrows(IllegalStateException::class.java) {
            JwtUtils.verifyJws<SamplePayload>(jws, expectedType = "rc-wrp+jwt") {
                it.first().publicKey
            }
        }

        assertEquals(
            "Invalid JWS header type, expected: rc-wrp+jwt, got: null",
            thrown.message,
        )
    }

    @Test
    fun `a jws of any type is verified when no type is expected`() {
        val jws = signedJws(type = "JWT")

        val payload = JwtUtils.verifyJws<SamplePayload>(jws) { it.first().publicKey }

        assertEquals(SamplePayload(sub = "user-123", challenge = "nonce-abc"), payload)
    }

    private fun signedJws(type: String?): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(type?.let(::JOSEObjectType))
            .x509CertChain(listOf(Base64.encode(certificate.encoded)))
            .build()
        return JWSObject(header, Payload("""{"sub":"user-123","challenge":"nonce-abc"}"""))
            .apply { sign(ECDSASigner(ecKey)) }
            .serialize()
    }

    private fun selfSignedCertificate(): X509Certificate {
        val name = X500Name("CN=Signer")
        val now = Instant.now()
        val holder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.ONE,
            Date.from(now.minus(Duration.ofDays(1))),
            Date.from(now.plus(Duration.ofDays(1))),
            name,
            keyPair.public,
        ).build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private))
        return JcaX509CertificateConverter().getCertificate(holder)
    }
}
