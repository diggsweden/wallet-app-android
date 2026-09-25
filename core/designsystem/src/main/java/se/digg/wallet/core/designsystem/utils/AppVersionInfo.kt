// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.utils

import android.content.Context
import android.util.Log

data class AppVersionInfo(val versionName: String, val versionCode: Long)

fun getAppVersion(context: Context): AppVersionInfo = try {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    AppVersionInfo(
        versionName = packageInfo.versionName ?: "",
        versionCode = packageInfo.longVersionCode,
    )
} catch (e: Exception) {
    Log.d("AppVersion", "Unable to get version")
    AppVersionInfo(versionName = "?", versionCode = 0)
}
