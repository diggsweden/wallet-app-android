// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavigationTest {

    @Test
    fun `toNavKey maps openid-credential-offer to IssuanceDeepLinkKey`() {
        val uriString =
            "openid-credential-offer://credential_offer?credential_offer_uri=https://issuer/offer"
        val uri = Uri.parse(uriString)

        val key = uri.toNavKey()

        assertNotNull(key)
        assertTrue(key is IssuanceKey)
        assertEquals(uriString, (key as IssuanceKey).fullUri)
    }

    @Test
    fun `toNavKey maps haip-vci to IssuanceDeepLinkKey`() {
        val uriString =
            "haip-vci://credential_offer?credential_offer_uri=https://issuer/offer"
        val uri = Uri.parse(uriString)

        val key = uri.toNavKey()

        assertNotNull(key)
        assertTrue(key is IssuanceKey)
        assertEquals(uriString, (key as IssuanceKey).fullUri)
    }

    @Test
    fun `toNavKey maps openid4vp to PresentationKey`() {
        val uriString =
            "openid4vp://?client_id=https://verifier&request_uri=https://verifier/request"
        val uri = Uri.parse(uriString)

        val key = uri.toNavKey()

        assertNotNull(key)
        assertTrue(key is PresentationKey)
        assertEquals(uriString, (key as PresentationKey).fullUri)
    }

    @Test
    fun `toNavKey maps eudi-openid4vp to PresentationKey`() {
        val uriString = "eudi-openid4vp://?client_id=https%3A%2F%2Fverifier.example"
        val uri = Uri.parse(uriString)

        val key = uri.toNavKey()

        assertNotNull(key)
        assertTrue(key is PresentationKey)
        assertEquals(uriString, (key as PresentationKey).fullUri)
    }

    @Test
    fun `toNavKey maps haip-vp to PresentationKey`() {
        val uriString = "haip-vp://?client_id=https%3A%2F%2Fverifier.example"
        val uri = Uri.parse(uriString)

        val key = uri.toNavKey()

        assertNotNull(key)
        assertTrue(key is PresentationKey)
        assertEquals(uriString, (key as PresentationKey).fullUri)
    }

    @Test
    fun `toNavKey handles case-insensitive schemes`() {
        val issuanceUri = Uri.parse("OPENID-CREDENTIAL-OFFER://offer?id=123")
        val presentationUri = Uri.parse("OPENID4VP://request?id=456")

        val issuanceKey = issuanceUri.toNavKey()
        val presentationKey = presentationUri.toNavKey()

        assertTrue(issuanceKey is IssuanceKey)
        assertTrue(presentationKey is PresentationKey)
    }

    @Test
    fun `toNavKey returns null for unsupported schemes`() {
        val httpsUri = Uri.parse("https://example.com/test")
        val customUri = Uri.parse("myapp://dashboard")
        val fileUri = Uri.parse("file:///android_asset/test.json")

        assertNull(httpsUri.toNavKey())
        assertNull(customUri.toNavKey())
        assertNull(fileUri.toNavKey())
    }

    @Test
    fun `toNavKey returns null for empty or schemeless uri`() {
        val emptyUri = Uri.parse("")
        val relativeUri = Uri.parse("/path/to/resource")

        assertNull(emptyUri.toNavKey())
        assertNull(relativeUri.toNavKey())
    }
}
