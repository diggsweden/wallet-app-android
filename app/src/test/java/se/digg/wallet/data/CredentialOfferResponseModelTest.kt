// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import java.net.URI
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialOfferResponseModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `CredentialRequestModel uses snake_case wire names`() {
        val encoded = json.encodeToString(
            CredentialRequestModel(
                credentialConfigurationId = "pid",
                proofs = Proof(jwt = listOf("a", "b")),
            ),
        )

        assertTrue(encoded.contains("\"credential_configuration_id\":\"pid\""))
        assertTrue(encoded.contains("\"jwt\":[\"a\",\"b\"]"))
    }

    @Test
    fun `CredentialRequestModel round trips with response encryption`() {
        val original = CredentialRequestModel(
            credentialConfigurationId = "pid",
            proofs = Proof(jwt = listOf("jwt")),
            credentialResponseEncryption = CredentialResponseEncryptionModel(
                jwk = JwkModel(kty = "EC", crv = "P-256", x = "x", y = "y"),
                enc = "A128GCM",
            ),
        )

        val decoded = json.decodeFromString<CredentialRequestModel>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("A128GCM", decoded.credentialResponseEncryption?.enc)
    }

    @Test
    fun `CredentialResponseModel decodes a list of credentials`() {
        val decoded = json.decodeFromString<CredentialResponseModel>(
            """{"credentials":[{"credential":"one"},{"credential":"two"}]}""",
        )

        assertEquals(listOf(Credential("one"), Credential("two")), decoded.credentials)
    }

    @Test
    fun `SavedCredential defaults issuedAt to now and generates a unique id`() {
        val before = System.currentTimeMillis()
        val first = SavedCredential(
            compactSerialized = "sd-jwt",
            claimDisplayNames = emptyMap(),
            issuer = null,
            displayData = null,
        )
        val second = first.copy(
            id = SavedCredential(
                compactSerialized = "sd-jwt",
                claimDisplayNames = emptyMap(),
                issuer = null,
                displayData = null,
            ).id,
        )

        assertTrue(first.issuedAt.time >= before)
        assertEquals("", first.type)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `SavedCredential round trips through JSON with all custom serializers`() {
        val original = SavedCredential(
            compactSerialized = "header.payload.signature~disclosure",
            claimDisplayNames = mapOf("given_name" to "Given name"),
            issuedAt = Date(1_700_000_000_000L),
            issuer = IssuerDisplay(
                name = "Digg",
                locale = Locale.forLanguageTag("sv-SE"),
                logo = IssuerDisplay.Logo(
                    uri = URI.create("https://example.test/logo.png"),
                    alternativeText = "Logo",
                ),
                description = "Issuer",
                backgroundImage = URI.create("https://example.test/bg.png"),
            ),
            type = CredentialType.PID.type,
            id = "fixed-id",
            displayData = CredentialDisplayData(name = "PID"),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SavedCredential>(encoded)

        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"issuedAt\":1700000000000"))
        assertTrue(encoded.contains("\"locale\":\"sv-SE\""))
        assertTrue(encoded.contains("\"uri\":\"https://example.test/logo.png\""))
    }

    @Test
    fun `IssuerDisplay leaves optional fields null when absent`() {
        val decoded = json.decodeFromString<IssuerDisplay>("""{"name":"Digg"}""")

        assertEquals("Digg", decoded.name)
        assertNull(decoded.locale)
        assertNull(decoded.logo)
        assertNull(decoded.description)
        assertNull(decoded.backgroundImage)
    }

    @Test
    fun `LocaleSerializer uses IETF BCP 47 language tags`() {
        val encoded = json.encodeToString(LocaleSerializer, Locale.forLanguageTag("en-US"))
        assertEquals("\"en-US\"", encoded)
        assertEquals(
            Locale.forLanguageTag("en-US"),
            json.decodeFromString(LocaleSerializer, encoded),
        )
        assertEquals("Locale", LocaleSerializer.descriptor.serialName)
    }

    @Test
    fun `JavaUriSerializer round trips a URI`() {
        val uri = URI.create("https://example.test/a?b=c")
        val encoded = json.encodeToString(JavaUriSerializer, uri)

        assertEquals("\"https://example.test/a?b=c\"", encoded)
        assertEquals(uri, json.decodeFromString(JavaUriSerializer, encoded))
        assertEquals("JavaURI", JavaUriSerializer.descriptor.serialName)
    }

    @Test
    fun `DateAsLongSerializer round trips epoch millis`() {
        val date = Date(42L)
        val encoded = json.encodeToString(DateAsLongSerializer, date)

        assertEquals("42", encoded)
        assertEquals(date, json.decodeFromString(DateAsLongSerializer, encoded))
        assertEquals("DateAsLong", DateAsLongSerializer.descriptor.serialName)
    }

    @Test
    fun `CredentialType PID carries the EUDI PID URN`() {
        assertEquals("urn:eudi:pid:1", CredentialType.PID.type)
        assertEquals(CredentialType.PID, CredentialType.valueOf("PID"))
    }

    @Test
    fun `CredentialData wraps a raw jwt`() {
        assertEquals("jwt", CredentialData("jwt").jwt)
    }
}
