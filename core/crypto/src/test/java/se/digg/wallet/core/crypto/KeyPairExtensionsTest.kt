// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyPairExtensionsTest {

    private val ecJwk = ECKeyGenerator(Curve.P_256).generate()
    private val keyPair = ecJwk.toKeyPair()

    @Test
    fun `toECKey converts key pair to ECKey on curve P_256 without thumbprint by default`() {
        val ecKey = keyPair.toECKey()

        assertEquals(Curve.P_256, ecKey.curve)
        assertNull(ecKey.keyID)
        assertNull(ecKey.algorithm)
    }

    @Test
    fun `toECKey with thumbprint populates keyID`() {
        val ecKey = keyPair.toECKey(withThumbprint = true)

        assertNotNull(ecKey.keyID)
        assertEquals(ecKey.computeThumbprint().toString(), ecKey.keyID)
    }

    @Test
    fun `toECKey preserves specified algorithm`() {
        val ecKey = keyPair.toECKey(algorithm = JWSAlgorithm.ES256)

        assertEquals(JWSAlgorithm.ES256, ecKey.algorithm)
    }

    @Test
    fun `toECKey throws IllegalStateException on non-EC key pair`() {
        val rsaKeyPair = RSAKeyGenerator(2048).generate().toKeyPair()

        assertThrows(IllegalStateException::class.java) {
            rsaKeyPair.toECKey()
        }
    }
}
