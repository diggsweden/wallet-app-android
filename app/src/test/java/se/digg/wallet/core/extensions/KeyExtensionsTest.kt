// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import java.security.KeyPairGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.wallet.client.gateway.models.EcJwkResponse

class KeyExtensionsTest {

    private fun ecKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    @Test
    fun `toECKey exposes the public key on the P-256 curve`() {
        val keyPair = ecKeyPair()

        val ecKey = keyPair.toECKey()

        assertEquals(Curve.P_256, ecKey.curve)
        assertNull(ecKey.keyID)
        assertNull(ecKey.algorithm)
        assertTrue(!ecKey.isPrivate)
    }

    @Test
    fun `toECKey can derive a thumbprint key id and set an algorithm`() {
        val keyPair = ecKeyPair()

        val ecKey = keyPair.toECKey(withThumbprint = true, algorithm = JWSAlgorithm.ES256)

        assertNotNull(ecKey.keyID)
        assertEquals(ecKey.computeThumbprint().toString(), ecKey.keyID)
        assertEquals(JWSAlgorithm.ES256, ecKey.algorithm)
    }

    @Test
    fun `toECKey derives the same thumbprint for the same key`() {
        val keyPair = ecKeyPair()

        assertEquals(
            keyPair.toECKey(withThumbprint = true).keyID,
            keyPair.toECKey(withThumbprint = true).keyID,
        )
    }

    @Test
    fun `toECKey rejects a non-EC key pair`() {
        val rsa = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        val error = runCatching { rsa.toECKey() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("No publicKey", error!!.message)
    }

    @Test
    fun `EcJwkResponse toECKey rebuilds the key from its wire coordinates`() {
        val source = ECKeyGenerator(Curve.P_256).keyID("kid-1").generate()

        val rebuilt = EcJwkResponse(
            kty = source.keyType.value,
            kid = "kid-1",
            crv = source.curve.name,
            x = source.x.toString(),
            y = source.y.toString(),
        ).toECKey()

        assertEquals(Curve.P_256, rebuilt.curve)
        assertEquals(source.x, rebuilt.x)
        assertEquals(source.y, rebuilt.y)
        assertEquals("kid-1", rebuilt.keyID)
        assertEquals(source.toECPublicKey().w, rebuilt.toECPublicKey().w)
    }
}
