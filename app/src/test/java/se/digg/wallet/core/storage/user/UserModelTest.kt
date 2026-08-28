// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.storage.user

import java.util.Date
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.data.CredentialDisplayData
import se.digg.wallet.data.CredentialType
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.SavedCredential

class UserModelTest {

    private val converters = DbConverters()

    private fun credential(id: String) = SavedCredential(
        compactSerialized = "sd-jwt-$id",
        claimDisplayNames = mapOf("given_name" to "Given name"),
        issuedAt = Date(1_700_000_000_000L),
        issuer = IssuerDisplay(name = "Digg"),
        type = CredentialType.PID.type,
        id = id,
        displayData = CredentialDisplayData(name = "PID"),
    )

    @Test
    fun `User defaults to the single row id and no opaque session`() {
        val user = User(uuid = null, accountId = null, credentials = emptyList(), pid = null)

        assertEquals(0, user.id)
        assertNull(user.opaqueSession)
    }

    @Test
    fun `uuid converters round trip`() {
        val uuid = UUID.fromString("6d3f2b4c-0000-4000-8000-000000000001")

        assertEquals(uuid.toString(), converters.uuidToString(uuid))
        assertEquals(uuid, converters.uuidFromString(uuid.toString()))
    }

    @Test
    fun `uuid converters tolerate null and malformed input`() {
        assertNull(converters.uuidToString(null))
        assertNull(converters.uuidFromString(null))
        assertNull(converters.uuidFromString("not-a-uuid"))
    }

    @Test
    fun `credential converters round trip a single credential`() {
        val original = credential("id-1")

        val stored = converters.credentialToString(original)

        assertTrue(stored!!.contains("id-1"))
        assertEquals(original, converters.credentialFromString(stored))
    }

    @Test
    fun `credential converters tolerate null and malformed input`() {
        assertNull(converters.credentialToString(null))
        assertNull(converters.credentialFromString(null))
        assertNull(converters.credentialFromString("{ not json"))
    }

    @Test
    fun `credential list converters round trip`() {
        val list = listOf(credential("id-1"), credential("id-2"))

        val stored = converters.savedCredentialListToString(list)

        assertEquals(list, converters.stringToSavedCredentialList(stored))
    }

    @Test
    fun `credential list converter falls back to an empty list`() {
        assertEquals(emptyList<SavedCredential>(), converters.stringToSavedCredentialList(null))
        assertEquals(
            emptyList<SavedCredential>(),
            converters.stringToSavedCredentialList("{ not json"),
        )
        assertEquals("[]", converters.savedCredentialListToString(emptyList()))
    }

    @Test
    fun `opaque session converters round trip`() {
        val session = OpaqueSession(
            serverPublicKeyJwk = """{"kty":"EC"}""",
            opaqueServerId = "server",
            opaqueContext = "context",
            stateId = "state",
        )

        val stored = converters.opaqueSessionToString(session)

        assertEquals(session, converters.stringToOpaqueSession(stored))
    }

    @Test
    fun `opaque session converters tolerate null and malformed input`() {
        assertNull(converters.opaqueSessionToString(null))
        assertNull(converters.stringToOpaqueSession(null))
        assertNull(converters.stringToOpaqueSession("{ not json"))
    }

    @Test
    fun `opaque session allows a null state id`() {
        val session = OpaqueSession(
            serverPublicKeyJwk = "{}",
            opaqueServerId = "server",
            opaqueContext = "context",
            stateId = null,
        )

        assertNull(
            converters.stringToOpaqueSession(converters.opaqueSessionToString(session))!!.stateId,
        )
    }
}
