// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.deeplink

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.oauth.OAuthCoordinator

class DeepLinkHandlerTest {

    private val oAuthCoordinator = mockk<OAuthCoordinator>(relaxed = true)
    private val handler = DeepLinkHandler(oAuthCoordinator)

    private fun intent(uri: Uri?) = mockk<Intent>().also { every { it.data } returns uri }

    @Test
    fun `an intent without data is consumed`() {
        val result = handler.handle(intent(uri = null))

        assertEquals(DeepLinkResult.Consumed, result)
        verify(exactly = 0) { oAuthCoordinator.onDeepLink(any()) }
    }

    @Test
    fun `a redirect for an ongoing auth session is handed to the coordinator`() {
        val uri = mockk<Uri>()
        every { oAuthCoordinator.hasOngoingPendingResult() } returns true

        val result = handler.handle(intent(uri))

        assertEquals(DeepLinkResult.Consumed, result)
        verify(exactly = 1) { oAuthCoordinator.onDeepLink(uri) }
    }

    @Test
    fun `a link with no auth session in flight is passed on to the app`() {
        val uri = mockk<Uri>()
        every { oAuthCoordinator.hasOngoingPendingResult() } returns false

        val result = handler.handle(intent(uri))

        assertTrue(result is DeepLinkResult.Unhandled)
        assertEquals(uri, (result as DeepLinkResult.Unhandled).uri)
        verify(exactly = 0) { oAuthCoordinator.onDeepLink(any()) }
    }

    @Test
    fun `Unhandled is a value type keyed on its uri`() {
        val uri = mockk<Uri>()

        assertEquals(DeepLinkResult.Unhandled(uri), DeepLinkResult.Unhandled(uri))
        assertTrue(DeepLinkResult.Unhandled(uri) != DeepLinkResult.Consumed)
    }
}
