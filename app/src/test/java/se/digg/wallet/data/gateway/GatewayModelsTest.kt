// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data.gateway

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.wallet.client.gateway.models.ApiInfoResponse
import se.wallet.client.gateway.models.AuthChallengeRequest
import se.wallet.client.gateway.models.AuthChallengeResponse
import se.wallet.client.gateway.models.CreateAccountRequest
import se.wallet.client.gateway.models.CreateAccountResponse
import se.wallet.client.gateway.models.EcJwkRequest
import se.wallet.client.gateway.models.EcJwkResponse
import se.wallet.client.gateway.models.HsmAsyncStatus
import se.wallet.client.gateway.models.HsmRequest
import se.wallet.client.gateway.models.HsmRequestType
import se.wallet.client.gateway.models.HsmResponse
import se.wallet.client.gateway.models.ProblemParameterResponse
import se.wallet.client.gateway.models.ProblemResponse
import se.wallet.client.gateway.models.RegisterStateRequest
import se.wallet.client.gateway.models.RegisterStateResponse
import se.wallet.client.gateway.models.SessionResponse
import se.wallet.client.gateway.models.WalletDeviceOS
import se.wallet.client.gateway.models.WuaResponse

/**
 * Round-trip tests for the Fabrikt-generated client-gateway models. These guard the wire contract
 * in `src/main/openapi/client-gateway.yaml`: a regenerated model that renames, retypes or drops a
 * property fails here rather than at runtime against the gateway.
 */
class GatewayModelsTest {

    // Mirrors networkJson in NetworkModule so the tests decode the way the app does.
    private val json = Json { ignoreUnknownKeys = true }

    private val ecJwkJson =
        """
        {"kty":"EC","kid":"device-1","alg":"ES256","use":"sig","crv":"P-256","x":"eG9v","y":"eW9v"}
        """.trimIndent()

    @Test
    fun `ApiInfoResponse round-trips every documented member`() {
        val decoded = json.decodeFromString<ApiInfoResponse>(
            """
            {"name":"client-gateway","version":"1.4.0","releaseDate":"2026-08-31",
             "status":"active","links":["https://example.test/docs"]}
            """.trimIndent(),
        )

        assertEquals("client-gateway", decoded.name)
        assertEquals("1.4.0", decoded.version)
        assertEquals("2026-08-31", decoded.releaseDate)
        assertEquals("active", decoded.status)
        assertEquals(listOf("https://example.test/docs"), decoded.links)
        assertEquals(decoded, json.decodeFromString<ApiInfoResponse>(json.encodeToString(decoded)))
    }

    @Test
    fun `ApiInfoResponse tolerates an absent links array and unknown members`() {
        val decoded = json.decodeFromString<ApiInfoResponse>(
            """{"name":"client-gateway","version":"1.4.0","releaseDate":"2026-08-31",
                "status":"beta","futureField":"ignored"}""",
        )

        assertNull(decoded.links)
        assertEquals("beta", decoded.status)
    }

    @Test
    fun `ProblemResponse maps the hyphenated RFC 9457 members to their Kotlin names`() {
        val decoded = json.decodeFromString<ProblemResponse>(
            """
            {"type":"https://example.test/problem/invalid-request","status":400,
             "title":"Bad Request","detail":"deviceKey is malformed","instance":"/accounts",
             "transaction-id":"tx-123",
             "invalid-parameters":[{"reason":"must be a P-256 key","value":"RSA","property":"kty"}]}
            """.trimIndent(),
        )

        assertEquals(400, decoded.status)
        assertEquals("Bad Request", decoded.title)
        assertEquals("tx-123", decoded.transactionId)
        assertEquals("/accounts", decoded.instance)
        assertEquals("deviceKey is malformed", decoded.detail)
        assertEquals(
            ProblemParameterResponse(
                reason = "must be a P-256 key",
                value = "RSA",
                property = "kty",
            ),
            decoded.invalidParameters?.single(),
        )

        val encoded = json.encodeToString(decoded)
        assertTrue(encoded.contains("\"transaction-id\":\"tx-123\""))
        assertTrue(encoded.contains("\"invalid-parameters\":["))
    }

    @Test
    fun `ProblemResponse keeps only status and title mandatory`() {
        val decoded = json.decodeFromString<ProblemResponse>(
            """{"status":503,"title":"Service Unavailable"}""",
        )

        assertNull(decoded.type)
        assertNull(decoded.detail)
        assertNull(decoded.instance)
        assertNull(decoded.transactionId)
        assertNull(decoded.invalidParameters)
        assertEquals(503, decoded.status)
    }

    @Test
    fun `ProblemParameterResponse defaults every member to null`() {
        val decoded = json.decodeFromString<ProblemParameterResponse>("{}")

        assertNull(decoded.reason)
        assertNull(decoded.value)
        assertNull(decoded.property)
    }

    @Test
    fun `EcJwkRequest and EcJwkResponse share the same wire shape`() {
        val request = json.decodeFromString<EcJwkRequest>(ecJwkJson)
        val response = json.decodeFromString<EcJwkResponse>(ecJwkJson)

        assertEquals(request.kty, response.kty)
        assertEquals(request.kid, response.kid)
        assertEquals(request.alg, response.alg)
        assertEquals(request.use, response.use)
        assertEquals(request.crv, response.crv)
        assertEquals(request.x, response.x)
        assertEquals(request.y, response.y)
        assertEquals(request, json.decodeFromString<EcJwkRequest>(json.encodeToString(request)))
        assertEquals(response, json.decodeFromString<EcJwkResponse>(json.encodeToString(response)))
    }

    @Test
    fun `EcJwk alg and use stay optional`() {
        val minimal =
            """{"kty":"EC","kid":"device-1","crv":"P-256","x":"eG9v","y":"eW9v"}"""

        assertNull(json.decodeFromString<EcJwkRequest>(minimal).alg)
        assertNull(json.decodeFromString<EcJwkRequest>(minimal).use)
        assertNull(json.decodeFromString<EcJwkResponse>(minimal).alg)
        assertNull(json.decodeFromString<EcJwkResponse>(minimal).use)
    }

    @Test
    fun `CreateAccountRequest carries both the correct and the misspelled email member`() {
        val request = CreateAccountRequest(
            personalIdentityNumber = "199001011234",
            email = "user@example.test",
            emailAdress = "user@example.test",
            telephoneNumber = "+46700000000",
            deviceKey = json.decodeFromString(ecJwkJson),
        )

        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"email\":\"user@example.test\""))
        assertTrue(encoded.contains("\"emailAdress\":\"user@example.test\""))
        assertEquals(request, json.decodeFromString<CreateAccountRequest>(encoded))
    }

    @Test
    fun `CreateAccountRequest requires only the device key`() {
        val decoded = json.decodeFromString<CreateAccountRequest>(
            """{"deviceKey":$ecJwkJson}""",
        )

        assertNull(decoded.personalIdentityNumber)
        assertNull(decoded.email)
        assertNull(decoded.emailAdress)
        assertNull(decoded.telephoneNumber)
        assertEquals("device-1", decoded.deviceKey.kid)
    }

    @Test
    fun `single-member responses expose their identifier`() {
        assertEquals(
            "account-1",
            json.decodeFromString<CreateAccountResponse>("""{"accountId":"account-1"}""").accountId,
        )
        assertEquals(
            "session-1",
            json.decodeFromString<SessionResponse>("""{"sessionId":"session-1"}""").sessionId,
        )
        assertEquals(
            "header.payload.signature",
            json.decodeFromString<WuaResponse>(
                """{"jwt":"header.payload.signature"}""",
            ).jwt,
        )
    }

    @Test
    fun `AuthChallengeRequest and AuthChallengeResponse round-trip`() {
        val request = AuthChallengeRequest(signedJwt = "header.payload.signature")
        assertEquals(
            request,
            json.decodeFromString<AuthChallengeRequest>(json.encodeToString(request)),
        )

        assertEquals(
            "nonce-1",
            json.decodeFromString<AuthChallengeResponse>("""{"nonce":"nonce-1"}""").nonce,
        )
        assertNull(json.decodeFromString<AuthChallengeResponse>("{}").nonce)
    }

    @Test
    fun `RegisterStateRequest keeps ttl optional`() {
        val withTtl = RegisterStateRequest(
            deviceKey = json.decodeFromString(ecJwkJson),
            ttl = "PT10M",
        )

        assertEquals(
            withTtl,
            json.decodeFromString<RegisterStateRequest>(json.encodeToString(withTtl)),
        )
        assertNull(
            json.decodeFromString<RegisterStateRequest>("""{"deviceKey":$ecJwkJson}""").ttl,
        )
    }

    @Test
    fun `RegisterStateResponse decodes the nested server JWS public key`() {
        val decoded = json.decodeFromString<RegisterStateResponse>(
            """
            {"status":"COMPLETE","devAuthorizationCode":"code-1",
             "serverJwsPublicKey":$ecJwkJson,"opaqueServerId":"opaque-1"}
            """.trimIndent(),
        )

        assertEquals("COMPLETE", decoded.status)
        assertEquals("code-1", decoded.devAuthorizationCode)
        assertEquals("device-1", decoded.serverJwsPublicKey?.kid)
        assertEquals("opaque-1", decoded.opaqueServerId)
        assertEquals(
            decoded,
            json.decodeFromString<RegisterStateResponse>(json.encodeToString(decoded)),
        )
    }

    @Test
    fun `RegisterStateResponse requires only a status`() {
        val decoded = json.decodeFromString<RegisterStateResponse>("""{"status":"PENDING"}""")

        assertEquals("PENDING", decoded.status)
        assertNull(decoded.devAuthorizationCode)
        assertNull(decoded.serverJwsPublicKey)
        assertNull(decoded.opaqueServerId)
    }

    @Test
    fun `HsmRequest and HsmResponse round-trip a pending async operation`() {
        val request = HsmRequest(outerRequestJws = "header.payload.signature")
        assertEquals(request, json.decodeFromString<HsmRequest>(json.encodeToString(request)))

        val pending = json.decodeFromString<HsmResponse>(
            """
            {"id":"op-1","status":"PENDING","resultUrl":"https://example.test/hsm/op-1"}
            """.trimIndent(),
        )

        assertEquals("op-1", pending.id)
        assertEquals(HsmAsyncStatus.PENDING, pending.status)
        assertNull(pending.result)
        assertEquals("https://example.test/hsm/op-1", pending.resultUrl)
    }

    @Test
    fun `HsmResponse carries the JWT result once complete`() {
        val complete = json.decodeFromString<HsmResponse>(
            """{"id":"op-1","status":"COMPLETE","result":"header.payload.signature"}""",
        )

        assertEquals(HsmAsyncStatus.COMPLETE, complete.status)
        assertEquals("header.payload.signature", complete.result)
        assertNull(complete.resultUrl)
        assertEquals(complete, json.decodeFromString<HsmResponse>(json.encodeToString(complete)))
    }

    // HsmAsyncStatus is a body member (HsmResponse.status). The generated enums are not annotated
    // @Serializable, so the @SerialName on each entry is inert and kotlinx matches on the entry
    // name -- safe here only because every entry name equals its wire value.
    @Test
    fun `HsmAsyncStatus maps every wire value in both directions`() {
        HsmAsyncStatus.entries.forEach { status ->
            assertEquals(status, HsmAsyncStatus.fromValue(status.value))
            assertEquals(status.value, status.toString())
            assertEquals(
                status,
                json.decodeFromString<HsmAsyncStatus>("\"${status.value}\""),
            )
        }
        assertNull(HsmAsyncStatus.fromValue("RETIRED"))
    }

    // HsmRequestType is a query parameter and WalletDeviceOS a request header, so neither is
    // serialized as JSON. They travel as `value`, which is what these tests pin.

    @Test
    fun `HsmRequestType maps every query value in both directions`() {
        HsmRequestType.entries.forEach { type ->
            assertEquals(type, HsmRequestType.fromValue(type.value))
            assertEquals(type.value, type.toString())
        }
        assertEquals(
            listOf(
                "CREATE_SESSION",
                "CREATE_KEY",
                "LIST_KEYS",
                "DELETE_KEY",
                "REGISTER_PIN",
                "CHANGE_PIN",
                "SIGN",
                "OTHER",
            ),
            HsmRequestType.entries.map { it.value },
        )
        assertNull(HsmRequestType.fromValue("UNLOCK_PIN"))
    }

    @Test
    fun `WalletDeviceOS keeps the lower-cased iOS header value`() {
        assertEquals(WalletDeviceOS.I_OS, WalletDeviceOS.fromValue("iOS"))
        assertEquals(WalletDeviceOS.ANDROID, WalletDeviceOS.fromValue("Android"))
        assertEquals("iOS", WalletDeviceOS.I_OS.toString())
        assertEquals("Android", WalletDeviceOS.ANDROID.toString())
        // The header is case-sensitive; the Kotlin entry name I_OS is not a valid wire value.
        assertNull(WalletDeviceOS.fromValue("android"))
        assertNull(WalletDeviceOS.fromValue("I_OS"))
    }
}
