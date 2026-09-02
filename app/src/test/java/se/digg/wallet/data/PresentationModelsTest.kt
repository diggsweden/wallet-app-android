// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

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
}
