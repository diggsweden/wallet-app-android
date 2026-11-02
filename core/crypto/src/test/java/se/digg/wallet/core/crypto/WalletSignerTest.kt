// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletSignerTest {

    private val ecKey = ECKeyGenerator(Curve.P_256).generate()
    private val signer = WalletSigner(ecKey.toKeyPair())

    @Test
    fun `supported algorithms returns strictly ES256`() {
        assertEquals(setOf(JWSAlgorithm.ES256), signer.supportedJWSAlgorithms())
    }

    @Test
    fun `sign produces a 64-byte JOSE concatenated signature that verifies`() {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256).build()
        val signingInput = "header.payload".toByteArray(Charsets.UTF_8)

        val signature = signer.sign(header, signingInput)
        val signatureBytes = signature.decode()

        // ES256 signature in JOSE format is R || S, exactly 64 bytes (32 bytes each)
        assertEquals(64, signatureBytes.size)

        val verifier = ECDSAVerifier(ecKey.toECPublicKey())
        assertTrue(verifier.verify(header, signingInput, signature))
    }

    @Test
    fun `signs a SignedJWT successfully and verifies`() {
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256).build(),
            JWTClaimsSet.Builder()
                .subject("subject-123")
                .claim("aud", "https://example.com")
                .build(),
        )

        jwt.sign(signer)

        val verifier = ECDSAVerifier(ecKey.toECPublicKey())
        assertTrue(jwt.verify(verifier))
        assertEquals("subject-123", jwt.jwtClaimsSet.subject)
    }
}
