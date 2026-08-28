// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyOperation
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JwkModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `JwkModel serializes the dotted x5t hash claim under its wire name`() {
        val encoded = json.encodeToString(
            JwkModel(
                kty = "EC",
                keyOps = listOf("sign"),
                x5u = "https://example.test/certs",
                x5c = listOf("cert"),
                x5t = "thumb",
                x5tS256 = "thumb256",
            ),
        )

        assertTrue(encoded.contains("\"key_ops\":[\"sign\"]"))
        assertTrue(encoded.contains("\"x5t#S256\":\"thumb256\""))
        assertEquals(
            "thumb256",
            json.decodeFromString<JwkModel>(encoded).x5tS256,
        )
    }

    @Test
    fun `JwkModel omits absent optional members`() {
        val decoded = json.decodeFromString<JwkModel>("""{"kty":"oct","k":"secret"}""")

        assertEquals("oct", decoded.kty)
        assertEquals("secret", decoded.k)
        assertNull(decoded.use)
        assertNull(decoded.keyOps)
        assertNull(decoded.alg)
        assertNull(decoded.crv)
    }

    @Test
    fun `toJwkModel maps an EC key to its public coordinates`() {
        val key: ECKey = ECKeyGenerator(Curve.P_256)
            .keyID("ec-kid")
            .keyUse(KeyUse.SIGNATURE)
            .keyOperations(setOf(KeyOperation.SIGN, KeyOperation.VERIFY))
            .algorithm(Algorithm("ES256"))
            .generate()

        val model = key.toJwkModel()

        assertEquals("EC", model.kty)
        assertEquals("P-256", model.crv)
        assertEquals(key.x.toString(), model.x)
        assertEquals(key.y.toString(), model.y)
        assertEquals("sig", model.use)
        assertEquals(setOf("sign", "verify"), model.keyOps?.toSet())
        assertEquals("ES256", model.alg)
        assertEquals("ec-kid", model.kid)
        assertNull(model.n)
        assertNull(model.k)
    }

    @Test
    fun `toJwkModel maps an RSA key to modulus and exponent`() {
        val key: RSAKey = RSAKeyGenerator(2048).keyID("rsa-kid").generate()

        val model = key.toJwkModel()

        assertEquals("RSA", model.kty)
        assertEquals(key.modulus.toString(), model.n)
        assertEquals(key.publicExponent.toString(), model.e)
        assertEquals("rsa-kid", model.kid)
        assertNull(model.use)
        assertNull(model.keyOps)
        assertNull(model.alg)
        assertNull(model.crv)
    }

    @Test
    fun `toJwkModel maps an octet sequence key to its raw value`() {
        val key: OctetSequenceKey = OctetSequenceKeyGenerator(256)
            .keyID("oct-kid")
            .keyUse(KeyUse.ENCRYPTION)
            .generate()

        val model = key.toJwkModel()

        assertEquals("oct", model.kty)
        assertEquals(key.keyValue.toString(), model.k)
        assertEquals("enc", model.use)
        assertEquals("oct-kid", model.kid)
    }

    @Test
    fun `toJwkModel rejects an unsupported key type`() {
        // An OKP key parsed from its wire form - no supported kty branch matches it.
        val key: JWK = JWK.parse(
            """{"kty":"OKP","crv":"Ed25519","x":"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"}""",
        )

        val error = runCatching { key.toJwkModel() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.startsWith("Unsupported JWK type:"))
    }
}
