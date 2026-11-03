// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth.testing

import kotlinx.coroutines.CompletableDeferred
import se.digg.wallet.core.webauth.WebAuthResult
import se.digg.wallet.core.webauth.WebAuthenticator

class FakeWebAuthenticator : WebAuthenticator {
    var result: WebAuthResult =
        WebAuthResult.Success("wallet-app://authorize?code=code&state=state")
    var gate: CompletableDeferred<Unit>? = null
    var count = 0

    override suspend fun authenticate(url: String, callbackScheme: String): WebAuthResult {
        count++
        gate?.await()
        return result
    }
}
