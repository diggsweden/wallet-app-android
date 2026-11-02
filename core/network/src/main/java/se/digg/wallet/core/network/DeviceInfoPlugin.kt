// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.plugins.api.createClientPlugin

data class DeviceInfo(
    val os: String,
    val osVersion: String,
    val model: String,
    val appVersion: String,
)

class DeviceInfoPluginConfig {
    lateinit var deviceInfo: DeviceInfo
}

val deviceInfoPlugin =
    createClientPlugin("DeviceInfoPlugin", ::DeviceInfoPluginConfig) {
        val deviceInfo = pluginConfig.deviceInfo
        onRequest { request, _ ->
            request.headers.append("Wallet-Device-OS", deviceInfo.os)
            request.headers.append("Wallet-Device-OS-Version", deviceInfo.osVersion)
            request.headers.append("Wallet-Device-Model", deviceInfo.model)
            request.headers.append("Wallet-App-Version", deviceInfo.appVersion)
        }
    }
