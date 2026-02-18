package se.digg.wallet.core

import android.content.Context
import timber.log.Timber

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
