// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationTest {

    @Test
    fun `each parameterless destination is a single shared instance`() {
        val keys: List<NavKey> = listOf(
            DashboardKey,
            IntroKey,
            OnboardingKey,
            AboutKey,
            LanguageKey,
            ThemeKey,
            HelpKey,
            LicensesKey,
        )

        keys.forEach { assertSame(it, keys.single { other -> other === it }) }
        assertEquals(8, keys.distinct().size)
    }

    @Test
    fun `CredentialDetailsKey round trips its credential id`() {
        val key = CredentialDetailsKey(id = "cred-1")

        val decoded = Json.decodeFromString<CredentialDetailsKey>(Json.encodeToString(key))

        assertEquals(key, decoded)
        assertEquals("cred-1", decoded.id)
    }

    @Test
    fun `SettingsKey round trips the flag that says it was opened from the intro`() {
        val fromIntro = SettingsKey(isFromIntro = true)

        assertEquals(fromIntro, Json.decodeFromString<SettingsKey>(Json.encodeToString(fromIntro)))
        assertTrue(Json.decodeFromString<SettingsKey>(Json.encodeToString(fromIntro)).isFromIntro)
    }

    @Test
    fun `SettingsKey opened from anywhere but the intro is the default`() {
        assertEquals(SettingsKey(isFromIntro = false), SettingsKey())
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
        assertEquals("{}", Json.encodeToString(IntroKey))
        assertEquals("{}", Json.encodeToString(OnboardingKey))
        assertEquals("{}", Json.encodeToString(AboutKey))
        assertEquals("{}", Json.encodeToString(LanguageKey))
        assertEquals("{}", Json.encodeToString(ThemeKey))
        assertEquals("{}", Json.encodeToString(HelpKey))
        assertEquals("{}", Json.encodeToString(LicensesKey))
    }

    @Test
    fun `keys with different arguments are not equal`() {
        assertTrue(CredentialDetailsKey("a") != CredentialDetailsKey("b"))
        assertTrue(PresentationKey("a") != IssuanceDeepLinkKey("a"))
    }

    /**
     * `Uri.parse` is one of the Android stubs that throws on the JVM, and `toNavKey` only ever
     * reads the scheme and the full string, so the incoming link is stood in for directly.
     */
    private fun deepLink(scheme: String?, fullUri: String) = mockk<Uri>().also {
        every { it.scheme } returns scheme
        every { it.toString() } returns fullUri
    }

    @Test
    fun `every credential offer scheme opens the issuance destination with the link intact`() {
        listOf("openid-credential-offer", "haip-vci").forEach { scheme ->
            val fullUri = "$scheme://issuer.example?credential_offer_uri=x"

            assertEquals(
                IssuanceDeepLinkKey(fullUri),
                deepLink(scheme, fullUri).toNavKey(),
            )
        }
    }

    @Test
    fun `every presentation scheme opens the presentation destination with the link intact`() {
        listOf("eudi-openid4vp", "openid4vp", "haip-vp").forEach { scheme ->
            val fullUri = "$scheme://verifier.example?request_uri=x"

            assertEquals(
                PresentationKey(fullUri),
                deepLink(scheme, fullUri).toNavKey(),
            )
        }
    }

    @Test
    fun `a scheme outside the two allow lists opens nothing`() {
        // The allow list is what keeps an arbitrary link from reaching the issuance and
        // presentation flows, so near-misses have to fall through as well as plain http.
        listOf("https", "openid", "openid4vci", "haip", "eudi-openid4vp-x", "")
            .forEach { scheme ->
                assertNull(deepLink(scheme, "$scheme://host").toNavKey())
            }
    }

    @Test
    fun `a link without a scheme opens nothing`() {
        assertNull(deepLink(scheme = null, fullUri = "host?a=b").toNavKey())
    }
}
