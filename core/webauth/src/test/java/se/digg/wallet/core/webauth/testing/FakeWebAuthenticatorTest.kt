// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth.testing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.webauth.WebAuthResult

class FakeWebAuthenticatorTest {

    private val fake = FakeWebAuthenticator()

    @Test
    fun `default behavior returns success and increments count`() = runTest {
        assertEquals(0, fake.count)

        val result = fake.authenticate("https://example.com", "wallet-app")

        assertEquals(1, fake.count)
        assertTrue(result is WebAuthResult.Success)
        assertEquals(
            "wallet-app://authorize?code=code&state=state",
            (result as WebAuthResult.Success).callbackUri,
        )
    }

    @Test
    fun `custom result is returned when configured`() = runTest {
        fake.result = WebAuthResult.Failure("Custom test error")

        val result = fake.authenticate("https://example.com", "wallet-app")

        assertTrue(result is WebAuthResult.Failure)
        assertEquals("Custom test error", (result as WebAuthResult.Failure).message)
    }

    @Test
    fun `gate suspends authentication until released`() = runTest {
        val gate = CompletableDeferred<Unit>()
        fake.gate = gate

        val authDeferred = async {
            fake.authenticate("https://example.com", "wallet-app")
        }
        testScheduler.runCurrent()

        assertEquals(1, fake.count)
        assertFalse(authDeferred.isCompleted)

        gate.complete(Unit)
        testScheduler.runCurrent()

        assertTrue(authDeferred.isCompleted)
        assertTrue(authDeferred.await() is WebAuthResult.Success)
    }
}
