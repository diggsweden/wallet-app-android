// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {

    @Test
    fun `the parameterless destinations are singletons`() {
        val keys: List<NavKey> = listOf(DashboardKey, SettingsKey, IntroKey, OnboardingKey)

        assertEquals(4, keys.distinct().size)
    }

    @Test
    fun `CredentialDetailsKey round trips its credential id`() {
        val key = CredentialDetailsKey(id = "cred-1")

        val decoded = Json.decodeFromString<CredentialDetailsKey>(Json.encodeToString(key))

        assertEquals(key, decoded)
        assertEquals("cred-1", decoded.id)
    }

    @Test
    fun `IssuanceDeepLinkKey round trips the full offer uri`() {
        val key = IssuanceDeepLinkKey(fullUri = "openid-credential-offer://host?a=b")

        val decoded = Json.decodeFromString<IssuanceDeepLinkKey>(Json.encodeToString(key))

        assertEquals(key, decoded)
    }

    @Test
    fun `PresentationKey round trips the full request uri`() {
        val key = PresentationKey(fullUri = "haip-vp://host?request_uri=x")

        val decoded = Json.decodeFromString<PresentationKey>(Json.encodeToString(key))

        assertEquals(key, decoded)
    }

    @Test
    fun `the parameterless destinations serialize as empty objects`() {
        assertEquals("{}", Json.encodeToString(DashboardKey))
        assertEquals("{}", Json.encodeToString(SettingsKey))
        assertEquals("{}", Json.encodeToString(IntroKey))
        assertEquals("{}", Json.encodeToString(OnboardingKey))
    }

    @Test
    fun `keys with different arguments are not equal`() {
        assertTrue(CredentialDetailsKey("a") != CredentialDetailsKey("b"))
        assertTrue(PresentationKey("a") != IssuanceDeepLinkKey("a"))
    }
}
