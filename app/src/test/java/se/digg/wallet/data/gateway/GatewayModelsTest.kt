// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data.gateway

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.wallet.client.gateway.models.CreateAccountRequest
import se.wallet.client.gateway.models.HsmAsyncStatus
import se.wallet.client.gateway.models.HsmRequestType
import se.wallet.client.gateway.models.ProblemParameterResponse
import se.wallet.client.gateway.models.ProblemResponse
import se.wallet.client.gateway.models.WalletDeviceOS

/**
 * Pins the members of the Fabrikt-generated client-gateway models whose wire form does not follow
 * from the Kotlin name: hyphenated RFC 9457 members, a misspelling kept for compatibility, and the
 * enums that travel as a query parameter or header rather than as JSON. A regeneration that loses
 * one of these fails here rather than at runtime against the gateway.
 *
 * Plain round-trips of the other generated models are deliberately absent: they restate the
 * OpenAPI document in Kotlin and are exercised for real by the client tests
 * ([se.digg.wallet.core.network.WalletOpaqueClientTest] and friends).
 */
class GatewayModelsTest {

    // Mirrors networkJson in NetworkModule so the tests decode the way the app does.
    private val json = Json { ignoreUnknownKeys = true }

    private val ecJwkJson =
        """
        {"kty":"EC","kid":"device-1","alg":"ES256","use":"sig","crv":"P-256","x":"eG9v","y":"eW9v"}
        """.trimIndent()

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
