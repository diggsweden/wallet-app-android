// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import se.digg.wallet.core.common.schemas.isSupportedWalletUri

private val UnsupportedMessageMinDuration = 3.seconds

private val LeftViewfinderDelay = 500.milliseconds

internal enum class ScanOutcome { Accepted, Rejected, Ignored }

@Stable
internal class QrCodeScanState(private val timeSource: TimeSource = TimeSource.Monotonic) {
    var showsUnsupportedMessage by mutableStateOf(false)
        private set

    private var hasAccepted = false
    private var rejectedValue: String? = null
    private var lastSeen = timeSource.markNow()
    private var lastRejected = timeSource.markNow()

    /** [value] is null when no QR code is inside the viewfinder. */
    fun onScanResult(value: String?, onQrCodeScan: (Uri) -> Unit): ScanOutcome {
        if (value == null) {
            onViewfinderEmpty()
            return ScanOutcome.Ignored
        }
        lastSeen = timeSource.markNow()
        if (hasAccepted || value == rejectedValue) return ScanOutcome.Ignored

        val uri = value.toWalletUriOrNull()
        return if (uri != null) {
            hasAccepted = true
            onQrCodeScan(uri)
            ScanOutcome.Accepted
        } else {
            rejectedValue = value
            lastRejected = timeSource.markNow()
            showsUnsupportedMessage = true
            ScanOutcome.Rejected
        }
    }

    private fun onViewfinderEmpty() {
        if (lastSeen.elapsedNow() < LeftViewfinderDelay) return

        rejectedValue = null
        if (lastRejected.elapsedNow() >= UnsupportedMessageMinDuration) {
            showsUnsupportedMessage = false
        }
    }
}

@Composable
internal fun rememberQrCodeScanState(): QrCodeScanState = remember { QrCodeScanState() }

internal fun String.toWalletUriOrNull(): Uri? = trim().toUri().normalizeScheme().takeIf {
    it.isSupportedWalletUri() && !it.schemeSpecificPart.isNullOrEmpty()
}
