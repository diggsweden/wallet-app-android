// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

internal enum class CameraPermissionState { Requesting, Granted, Denied }

@Composable
internal fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(
            value = if (context.hasCameraPermission()) {
                CameraPermissionState.Granted
            } else {
                CameraPermissionState.Requesting
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionState = if (granted) {
            CameraPermissionState.Granted
        } else {
            CameraPermissionState.Denied
        }
    }

    LaunchedEffect(key1 = Unit) {
        if (permissionState == CameraPermissionState.Requesting) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (context.hasCameraPermission()) {
            permissionState = CameraPermissionState.Granted
        }
    }

    return permissionState
}

private fun Context.hasCameraPermission(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
