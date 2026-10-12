// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executor
import se.digg.wallet.core.camera.component.ViewfinderSize

@Composable
internal fun QrCodeCameraPreview(
    cameraController: LifecycleCameraController,
    onScanResult: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanResult by rememberUpdatedState(onScanResult)
    val viewfinderSizePx = with(LocalDensity.current) { ViewfinderSize.roundToPx() }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    DisposableEffect(cameraController, lifecycleOwner, viewfinderSizePx) {
        val mainExecutor = context.mainExecutor
        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )

        cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        cameraController.setImageAnalysisAnalyzer(
            mainExecutor,
            qrCodeAnalyzer(
                barcodeScanner = barcodeScanner,
                executor = mainExecutor,
                currentScanArea = { scanArea(previewSize, viewfinderSizePx) },
                onScanResult = { currentOnScanResult(it) },
            ),
        )
        cameraController.bindToLifecycle(lifecycleOwner)

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { PreviewView(it).apply { controller = cameraController } },
        modifier = modifier.onSizeChanged { previewSize = it },
    )
}

private fun qrCodeAnalyzer(
    barcodeScanner: BarcodeScanner,
    executor: Executor,
    currentScanArea: () -> Rect,
    onScanResult: (String?) -> Unit,
): MlKitAnalyzer = MlKitAnalyzer(
    listOf(barcodeScanner),
    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
    executor,
) { result ->
    val scanArea = currentScanArea()
    val value = result.getValue(barcodeScanner)
        ?.filter { it.boundingBox?.let(scanArea::containsCenterOf) == true }
        ?.firstNotNullOfOrNull { it.rawValue }
    // Also called with null, so the scan state can tell when a code has left the viewfinder
    onScanResult(value)
}
