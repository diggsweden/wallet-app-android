// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import java.security.Signature
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the `Signer` / `ProvisionDPoPSigner` surface the openid4vci library drives. */
class DpopProofBuilderSignerTest {

    private val key: ECKey = ECKeyGenerator(Curve.P_256).keyID("dpop-kid").generate()
    private val builder = DpopProofBuilder(key = key)

    @Test
    fun `the builder advertises ES256 as its proof algorithm`() {
        assertEquals("ES256", builder.popAlgorithm.name)
        assertEquals("SHA256withECDSA", builder.javaAlgorithm)
    }

    @Test
    fun `provisioning a signer for an authorization server reuses the builder`() = runTest {
        val signer = builder.invoke(HttpsUrl("https://as.example.test").getOrThrow())

        assertSame(builder, signer)
    }

    @Test
    fun `acquire exposes only the public key`() = runTest {
        val operation = builder.acquire()

        assertEquals(key.toPublicJWK(), operation.publicMaterial)
        assertTrue(!operation.publicMaterial.isPrivate)
    }

    @Test
    fun `the acquired sign function produces a DER signature the public key verifies`() = runTest {
        val operation = builder.acquire()
        val input = "header.payload".toByteArray(Charsets.US_ASCII)

        val signature = operation.function.sign(input)

        val verifier = Signature.getInstance("SHA256withECDSA").apply {
            initVerify(key.toECPublicKey())
            update(input)
        }
        assertTrue(verifier.verify(signature))
        // DER-encoded ECDSA signatures start with a SEQUENCE tag.
        assertEquals(0x30.toByte(), signature.first())
    }
}
