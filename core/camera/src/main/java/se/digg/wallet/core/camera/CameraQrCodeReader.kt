// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import se.digg.wallet.core.camera.component.CameraPermissionDenied
import se.digg.wallet.core.camera.permission.CameraPermissionState
import se.digg.wallet.core.camera.permission.rememberCameraPermissionState
import se.digg.wallet.core.camera.scanner.QrCodeScanner
import se.digg.wallet.core.common.extensions.openAppSettings

@Composable
fun CameraQrCodeReader(onQrCodeScan: (Uri) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    when (rememberCameraPermissionState()) {
        CameraPermissionState.Requesting -> Unit

        CameraPermissionState.Granted -> QrCodeScanner(
            onQrCodeScan = onQrCodeScan,
            modifier = modifier,
        )

        CameraPermissionState.Denied -> CameraPermissionDenied(
            onOpenSettingsClick = { context.openAppSettings() },
            modifier = modifier,
        )
    }
}
