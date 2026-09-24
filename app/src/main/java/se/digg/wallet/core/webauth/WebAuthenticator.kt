// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

interface WebAuthenticator {
    suspend fun authenticate(url: String, callbackScheme: String): WebAuthResult
}

sealed interface WebAuthResult {
    data class Success(val callbackUri: String) : WebAuthResult
    data object Cancelled : WebAuthResult
    data class Failure(val message: String) : WebAuthResult
}
