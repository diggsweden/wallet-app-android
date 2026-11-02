// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CryptoModelsTest {

    @Serializable
    private data class TestPayload(val accountId: String, val role: String)

    @Test
    fun `JwtClaimsSerializer flattens defaults and payload into a single JSON object`() {
        val claims = JwtClaims(
            defaults = DefaultJwtClaims(iat = 100, nbf = 100, exp = 700),
            payload = TestPayload(accountId = "acc-42", role = "user"),
        )

        val jsonString = Json.encodeToString(JwtClaimsSerializer(TestPayload.serializer()), claims)
        val jsonElement = Json.parseToJsonElement(jsonString).jsonObject

        // Flat keys at root level
        assertEquals("100", jsonElement["iat"]?.jsonPrimitive?.content)
        assertEquals("100", jsonElement["nbf"]?.jsonPrimitive?.content)
        assertEquals("700", jsonElement["exp"]?.jsonPrimitive?.content)
        assertEquals("acc-42", jsonElement["accountId"]?.jsonPrimitive?.content)
        assertEquals("user", jsonElement["role"]?.jsonPrimitive?.content)
        assertNull(jsonElement["defaults"])
        assertNull(jsonElement["payload"])
    }

    @Test
    fun `JwtClaimsSerializer deserializes flat JSON into JwtClaims structure`() {
        val jsonString = """{"iat":200,"nbf":200,"exp":800,"accountId":"acc-99","role":"admin"}"""

        val claims = Json.decodeFromString(
            JwtClaimsSerializer(TestPayload.serializer()),
            jsonString,
        )

        assertEquals(200, claims.defaults.iat)
        assertEquals(200, claims.defaults.nbf)
        assertEquals(800, claims.defaults.exp)
        assertEquals("acc-99", claims.payload.accountId)
        assertEquals("admin", claims.payload.role)
    }

    @Test
    fun `CryptoSpec holds JWK and encryption parameters`() {
        val jwk = ECKeyGenerator(Curve.P_256).generate()
        val spec = CryptoSpec(
            jwk = jwk,
            encryptionMethod = EncryptionMethod.A128GCM,
            encryptionAlgorithm = JWEAlgorithm.ECDH_ES,
        )

        assertEquals(jwk, spec.jwk)
        assertEquals(EncryptionMethod.A128GCM, spec.encryptionMethod)
        assertEquals(JWEAlgorithm.ECDH_ES, spec.encryptionAlgorithm)
        assertNotNull(spec.jwk.toECKey())
    }
}
