// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

private const val CUSTOM_TABS_ACTION = "android.support.customtabs.action.CustomTabsService"

fun getAppVersion(context: Context): AppVersionInfo = try {
    val packageInfo = context.packageManager.getPackageInfo(
        context.packageName,
        0,
    )

    val versionName = packageInfo.versionName ?: ""
    val versionCode = packageInfo.longVersionCode

    AppVersionInfo(versionName = versionName, versionCode = versionCode)
} catch (e: Exception) {
    Timber.d("Unable to get version")
    AppVersionInfo(versionName = "?", versionCode = 0)
}

data class AppVersionInfo(val versionName: String, val versionCode: Long)

fun getCustomTabsProvider(context: Context): String? {
    val packageManager = context.packageManager

    val browserPackage = packageManager.resolveActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
        0,
    )?.activityInfo?.packageName ?: return null

    val service = packageManager.queryIntentServices(
        Intent(CUSTOM_TABS_ACTION).setPackage(browserPackage),
        0,
    ).firstOrNull()

    val provider = service?.serviceInfo?.packageName

    Timber.d("CustomTabs provider: $provider")

    return provider
}
