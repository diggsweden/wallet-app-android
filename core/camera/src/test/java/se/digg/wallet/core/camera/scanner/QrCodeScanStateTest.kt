// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.net.Uri
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeCaller : (Uri) -> Unit {
    val receivedUris = mutableListOf<Uri>()

    override fun invoke(uri: Uri) {
        receivedUris += uri
    }
}

@RunWith(RobolectricTestRunner::class)
class QrCodeScanStateTest {

    @Test
    fun `passes a scanned wallet URI to the caller`() {
        val values = listOf("openid4vp://open", "haip-vp://?request_uri=x", "haip-vci://?offer=x")

        values.forEach { value ->
            val state = QrCodeScanState()
            val caller = FakeCaller()

            state.onScanResult(value, caller)

            assertEquals(value, listOf(Uri.parse(value)), caller.receivedUris)
        }
    }

    @Test
    fun `lowercases the scheme of a scanned wallet URI`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()

        state.onScanResult("OPENID4VP://open", caller)

        assertEquals(listOf(Uri.parse("openid4vp://open")), caller.receivedUris)
    }

    @Test
    fun `trims whitespace around a scanned URI`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()

        state.onScanResult("  openid4vp://open\n", caller)

        assertEquals(listOf(Uri.parse("openid4vp://open")), caller.receivedUris)
    }

    @Test
    fun `rejects a value that is not an absolute URI without asking the caller`() {
        listOf("not a url", "", "   ", "/relative/path", "openid4vp:").forEach { value ->
            val state = QrCodeScanState()
            val caller = FakeCaller()

            state.onScanResult(value, caller)

            assertEquals(value, emptyList<Uri>(), caller.receivedUris)
            assertTrue(value, state.showsUnsupportedMessage)
        }
    }

    @Test
    fun `rejects an unsupported scheme without calling the caller`() {
        listOf("https://example.com", "other-scheme://open").forEach { value ->
            val state = QrCodeScanState()
            val caller = FakeCaller()

            state.onScanResult(value, caller)

            assertEquals(value, emptyList<Uri>(), caller.receivedUris)
            assertTrue(value, state.showsUnsupportedMessage)
        }
    }

    @Test
    fun `passes a wallet URI scanned while a rejection is showing`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        state.onScanResult("openid4vp://open", caller)

        assertEquals(listOf(Uri.parse("openid4vp://open")), caller.receivedUris)
    }

    @Test
    fun `checks a rejected code again after it leaves the viewfinder`() {
        val timeSource = TestTimeSource()
        val state = QrCodeScanState(timeSource)
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        timeSource += 500.milliseconds
        state.onScanResult(null, caller)
        val outcome = state.onScanResult("other-scheme://open", caller)

        assertEquals(ScanOutcome.Rejected, outcome)
    }

    @Test
    fun `a short gap in detection does not count as leaving the viewfinder`() {
        val timeSource = TestTimeSource()
        val state = QrCodeScanState(timeSource)
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        timeSource += 100.milliseconds
        state.onScanResult(null, caller)
        val outcome = state.onScanResult("other-scheme://open", caller)

        assertEquals(ScanOutcome.Ignored, outcome)
    }

    @Test
    fun `the unsupported message stays while the rejected code is in the viewfinder`() {
        val timeSource = TestTimeSource()
        val state = QrCodeScanState(timeSource)
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        timeSource += 10.seconds
        state.onScanResult("other-scheme://open", caller)
        state.onScanResult(null, caller)

        assertTrue(state.showsUnsupportedMessage)
    }

    @Test
    fun `the unsupported message hides once the rejected code has left the viewfinder`() {
        val timeSource = TestTimeSource()
        val state = QrCodeScanState(timeSource)
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        timeSource += 3.seconds
        state.onScanResult(null, caller)

        assertFalse(state.showsUnsupportedMessage)
    }

    @Test
    fun `the unsupported message shows for at least three seconds`() {
        val timeSource = TestTimeSource()
        val state = QrCodeScanState(timeSource)
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        timeSource += 1.seconds
        state.onScanResult(null, caller)

        assertTrue(state.showsUnsupportedMessage)
    }

    @Test
    fun `ignores every scan after a code is accepted`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()
        state.onScanResult("openid4vp://open", caller)

        state.onScanResult("haip-vp://?request_uri=x", caller)
        state.onScanResult("openid4vp://open", caller)

        assertEquals(listOf(Uri.parse("openid4vp://open")), caller.receivedUris)
    }

    @Test
    fun `an accepted code shows no rejection`() {
        val state = QrCodeScanState()

        state.onScanResult("openid4vp://open", FakeCaller())

        assertFalse(state.showsUnsupportedMessage)
    }

    @Test
    fun `a wallet URI is reported as accepted`() {
        val state = QrCodeScanState()

        val outcome = state.onScanResult("openid4vp://open", FakeCaller())

        assertEquals(ScanOutcome.Accepted, outcome)
    }

    @Test
    fun `an unsupported scheme is reported as rejected`() {
        val state = QrCodeScanState()

        val outcome = state.onScanResult("other-scheme://open", FakeCaller())

        assertEquals(ScanOutcome.Rejected, outcome)
    }

    @Test
    fun `a value that is not an absolute URI is reported as rejected`() {
        val state = QrCodeScanState()

        val outcome = state.onScanResult("not a url", FakeCaller())

        assertEquals(ScanOutcome.Rejected, outcome)
    }

    @Test
    fun `a rejected code still in the viewfinder is reported as ignored`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()
        state.onScanResult("other-scheme://open", caller)

        val outcome = state.onScanResult("other-scheme://open", caller)

        assertEquals(ScanOutcome.Ignored, outcome)
    }

    @Test
    fun `a scan after a code is accepted is reported as ignored`() {
        val state = QrCodeScanState()
        val caller = FakeCaller()
        state.onScanResult("openid4vp://open", caller)

        val outcome = state.onScanResult("haip-vp://?request_uri=x", caller)

        assertEquals(ScanOutcome.Ignored, outcome)
    }

    @Test
    fun `a frame without a code is reported as ignored`() {
        val state = QrCodeScanState()

        val outcome = state.onScanResult(null, FakeCaller())

        assertEquals(ScanOutcome.Ignored, outcome)
    }
}
