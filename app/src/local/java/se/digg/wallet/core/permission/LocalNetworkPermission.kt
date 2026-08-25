// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val ANDROID_17 = 37
private const val REQUEST_CODE = 4001

fun Activity.requestLocalNetworkAccess() {
    if (Build.VERSION.SDK_INT < ANDROID_17) return
    if (checkSelfPermission(ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED) return

    requestPermissions(arrayOf(ACCESS_LOCAL_NETWORK), REQUEST_CODE)
}
