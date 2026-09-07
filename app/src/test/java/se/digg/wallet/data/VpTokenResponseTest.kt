// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import java.net.URLDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.digg.wallet.core.crypto.CryptoSpec

class VpTokenResponseTest {

    private val response = VpTokenResponse(
        vpToken = mapOf("pid" to listOf("a.b.c~d~")),
        nonce = "n once",
        state = "st&te",
    )

    private val recipientKey: ECKey = ECKeyGenerator(Curve.P_256).generate()

    private val cryptoSpec = CryptoSpec(
        jwk = recipientKey.toPublicJWK(),
        encryptionMethod = EncryptionMethod.A256GCM,
    )

    @Test
    fun `form encoding escapes values and includes state`() {
        val encoded = response.toDirectPost()
        val decoded = encoded.split("&").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            key to URLDecoder.decode(value, Charsets.UTF_8)
        }

        assertFalse(encoded.contains(" "))
        assertFalse(encoded.contains("{"))
        assertEquals("""{"pid":["a.b.c~d~"]}""", decoded["vp_token"])
        assertEquals("n once", decoded["nonce"])
        assertEquals("st&te", decoded["state"])
    }

    @Test
    fun `form encoding omits absent state`() {
        val encoded = response.copy(state = null).toDirectPost()

        assertFalse(encoded.contains("state"))
    }

    @Test
    fun `json encoding uses vp_token as an object`() {
        val json = Json.encodeToString(response)
        val element = Json.parseToJsonElement(json).jsonObject

        val presentations = element.getValue("vp_token").jsonObject.getValue("pid").jsonArray
        assertEquals("a.b.c~d~", presentations.single().jsonPrimitive.content)
        assertEquals("n once", element.getValue("nonce").jsonPrimitive.content)
        assertEquals("st&te", element.getValue("state").jsonPrimitive.content)
    }

    @Test
    fun `jwt encoding wraps an encrypted jwe in the response parameter`() {
        val encoded = response.toDirectPostJwt(cryptoSpec)
        val decoded = decodeForm(encoded)

        assertEquals(setOf("response"), decoded.keys)
        assertFalse(encoded.contains("vp_token"))
        assertFalse(encoded.contains("n once"))

        val jwe = JWEObject.parse(decoded.getValue("response"))
        assertEquals(JWEAlgorithm.ECDH_ES, jwe.header.algorithm)
        assertEquals(EncryptionMethod.A256GCM, jwe.header.encryptionMethod)
    }

    @Test
    fun `jwt encoding payload decrypts to the response claims`() {
        val payload = decrypt(response.toDirectPostJwt(cryptoSpec))

        val presentations = payload.getValue("vp_token").jsonObject.getValue("pid").jsonArray
        assertEquals("a.b.c~d~", presentations.single().jsonPrimitive.content)
        assertEquals("n once", payload.getValue("nonce").jsonPrimitive.content)
        assertEquals("st&te", payload.getValue("state").jsonPrimitive.content)
    }

    @Test
    fun `jwt encoding omits absent state`() {
        val payload = decrypt(response.copy(state = null).toDirectPostJwt(cryptoSpec))

        assertNull(payload["state"])
    }

    @Test
    fun `jwt encoding uses the requested encryption algorithm`() {
        val spec = cryptoSpec.copy(encryptionAlgorithm = JWEAlgorithm.ECDH_ES_A256KW)

        val encoded = response.toDirectPostJwt(spec)
        val jwe = JWEObject.parse(decodeForm(encoded).getValue("response"))

        assertEquals(JWEAlgorithm.ECDH_ES_A256KW, jwe.header.algorithm)
        assertEquals("n once", decrypt(encoded).getValue("nonce").jsonPrimitive.content)
    }

    @Test
    fun `jwt encoding differs per call because of ephemeral keys`() {
        val first = response.toDirectPostJwt(cryptoSpec)
        val second = response.toDirectPostJwt(cryptoSpec)

        assertNotEquals(first, second)
    }

    private fun decodeForm(encoded: String): Map<String, String> =
        encoded.split("&").associate { pair ->
            val (key, value) = pair.split("=", limit = 2)
            key to URLDecoder.decode(value, Charsets.UTF_8)
        }

    private fun decrypt(encoded: String): JsonObject {
        val jwe = JWEObject.parse(decodeForm(encoded).getValue("response"))
        jwe.decrypt(ECDHDecrypter(recipientKey))
        return Json.parseToJsonElement(jwe.payload.toString()).jsonObject
    }
}
