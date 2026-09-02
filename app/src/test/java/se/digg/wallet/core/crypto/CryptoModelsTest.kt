// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

@Serializable
private data class TestPayload(val nonce: String, val htu: String)

class CryptoModelsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = JwtClaimsSerializer(TestPayload.serializer())

    private val claims = JwtClaims(
        defaults = DefaultJwtClaims(iat = 1000, nbf = 1000, exp = 1600),
        payload = TestPayload(nonce = "n-1", htu = "https://example.test"),
    )

    @Test
    fun `serializer flattens defaults and payload into one JSON object`() {
        val encoded = json.parseToJsonElement(
            json.encodeToString(serializer, claims),
        ).jsonObject

        assertEquals(1000, encoded["iat"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, encoded["nbf"]!!.jsonPrimitive.content.toInt())
        assertEquals(1600, encoded["exp"]!!.jsonPrimitive.content.toInt())
        assertEquals("n-1", encoded["nonce"]!!.jsonPrimitive.content)
        assertEquals("https://example.test", encoded["htu"]!!.jsonPrimitive.content)
        assertEquals(5, encoded.size)
    }

    @Test
    fun `serializer round trips a flattened claims object`() {
        val decoded = json.decodeFromString(serializer, json.encodeToString(serializer, claims))

        assertEquals(claims, decoded)
    }

    @Test
    fun `deserializer reads defaults and payload from the same flat object`() {
        val decoded = json.decodeFromString(
            serializer,
            """{"iat":5,"nbf":5,"exp":605,"nonce":"n-2","htu":"https://a.test"}""",
        )

        assertEquals(DefaultJwtClaims(5, 5, 605), decoded.defaults)
        assertEquals(TestPayload("n-2", "https://a.test"), decoded.payload)
    }

    @Test
    fun `a payload member shadows a default with the same name`() {
        val encoded = json.encodeToString(
            JwtClaimsSerializer(DefaultJwtClaims.serializer()),
            JwtClaims(
                defaults = DefaultJwtClaims(iat = 1, nbf = 1, exp = 2),
                payload = DefaultJwtClaims(iat = 9, nbf = 9, exp = 9),
            ),
        )

        assertEquals("""{"iat":9,"nbf":9,"exp":9}""", encoded)
    }

    @Test
    fun `serializer descriptor is named JwtClaims`() {
        assertEquals("JwtClaims", serializer.descriptor.serialName)
    }
}
