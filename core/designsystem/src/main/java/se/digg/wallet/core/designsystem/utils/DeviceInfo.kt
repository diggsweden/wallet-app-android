// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    OTHER,
    OFFLINE,
    UNKNOWN,
}

data class DeviceInfo(
    val brand: String,
    val model: String,
    val osVersion: String,
    val sdkVersion: Int,
    val networkType: NetworkType,
    val appVersionName: String,
    val appVersionCode: Long,
)

fun getDeviceInfo(context: Context): DeviceInfo {
    val appVersion = getAppVersion(context)
    return DeviceInfo(
        brand = Build.BRAND,
        model = Build.MODEL,
        osVersion = Build.VERSION.RELEASE ?: "?",
        sdkVersion = Build.VERSION.SDK_INT,
        networkType = getNetworkType(context),
        appVersionName = appVersion.versionName,
        appVersionCode = appVersion.versionCode,
    )
}

private fun getNetworkType(context: Context): NetworkType {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.UNKNOWN
    val network = connectivityManager.activeNetwork ?: return NetworkType.OFFLINE
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.OFFLINE
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
        else -> NetworkType.OTHER
    }
}
