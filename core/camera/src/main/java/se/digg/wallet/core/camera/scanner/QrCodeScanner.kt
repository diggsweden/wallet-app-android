// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.net.Uri
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.camera.component.ScannerMessage
import se.digg.wallet.core.camera.component.ScannerViewfinder
import se.digg.wallet.core.camera.component.TorchButton

@Composable
internal fun QrCodeScanner(onQrCodeScan: (Uri) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val cameraController = remember(context) { LifecycleCameraController(context) }
    val scanState = rememberQrCodeScanState()
    val torch by rememberTorchUiState(cameraController)

    Box(modifier = modifier) {
        QrCodeCameraPreview(
            cameraController = cameraController,
            onScanResult = { value ->
                scanState.onScanResult(value, onQrCodeScan).hapticFeedback
                    ?.let(haptics::performHapticFeedback)
            },
            modifier = Modifier.fillMaxSize(),
        )
        ScannerViewfinder(modifier = Modifier.align(Alignment.Center))
        if (torch.isAvailable) {
            TorchButton(
                checked = torch.isOn,
                onCheckedChange = { cameraController.enableTorch(it) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
        }
        ScannerMessage(
            showUnsupportedMessage = scanState.showsUnsupportedMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 30.dp)
                .padding(bottom = 48.dp),
        )
    }
}

private val ScanOutcome.hapticFeedback: HapticFeedbackType?
    get() = when (this) {
        ScanOutcome.Accepted -> HapticFeedbackType.Confirm
        ScanOutcome.Rejected -> HapticFeedbackType.Reject
        ScanOutcome.Ignored -> null
    }
