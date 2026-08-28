// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationModelsTest {

    @Test
    fun `KeybindingPayload serializes sd_hash under its wire name`() {
        val payload = KeybindingPayload(
            aud = "https://verifier.example.test",
            nonce = "nonce-1",
            sdHash = "hash-1",
        )

        val encoded = Json.encodeToString(payload)

        assertTrue(encoded.contains("\"sd_hash\":\"hash-1\""))
        assertTrue(encoded.contains("\"aud\":\"https://verifier.example.test\""))
        assertEquals(payload, Json.decodeFromString<KeybindingPayload>(encoded))
    }

    @Test
    fun `CredentialQuery defaults to required`() {
        val query = CredentialQuery(
            id = "pid",
            claimPaths = setOf(ClaimPath.claim("given_name")),
        )

        assertTrue(query.required)
        assertEquals("pid", query.id)
        assertEquals(1, query.claimPaths.size)
    }

    @Test
    fun `CredentialQuery can be optional and carry several claim paths`() {
        val query = CredentialQuery(
            id = "pid",
            required = false,
            claimPaths = setOf(
                ClaimPath.claim("given_name"),
                ClaimPath.claim("address").claim("locality"),
            ),
        )

        assertTrue(!query.required)
        assertEquals(2, query.claimPaths.size)
    }

    @Test
    fun `CredentialQuery is a value type`() {
        val paths = setOf(ClaimPath.claim("given_name"))

        assertEquals(CredentialQuery("pid", true, paths), CredentialQuery("pid", true, paths))
        assertTrue(CredentialQuery("pid", true, paths) != CredentialQuery("other", true, paths))
    }
}
