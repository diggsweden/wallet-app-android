// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

interface DeviceAuthenticator {
    suspend fun getAccountId(): String
    suspend fun getDeviceKeyId(): String
    suspend fun signChallenge(nonce: String): String
}
