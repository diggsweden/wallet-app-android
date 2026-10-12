// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import androidx.camera.core.TorchState
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class TorchUiState(val isAvailable: Boolean = false, val isOn: Boolean = false)

@Composable
internal fun rememberTorchUiState(
    cameraController: LifecycleCameraController,
): State<TorchUiState> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState(TorchUiState(), cameraController, lifecycleOwner) {
        val observer = Observer<Int> { torchState ->
            value = TorchUiState(
                isAvailable = cameraController.cameraInfo?.hasFlashUnit() == true,
                isOn = torchState == TorchState.ON,
            )
        }
        cameraController.torchState.observe(lifecycleOwner, observer)
        awaitDispose { cameraController.torchState.removeObserver(observer) }
    }
}
