// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

data class NetworkConfig(val baseUrl: String, val apiKey: String, val isDebug: Boolean = false)
