// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.models.ClientLogRequest

class FakeLogSender(vararg results: NetworkResult<Unit>) : LogSender {
    private val results = ArrayDeque(results.toList())
    val sent = mutableListOf<String>()

    override suspend fun send(request: ClientLogRequest): NetworkResult<Unit> {
        sent += request.message
        return results.removeFirstOrNull() ?: NetworkResult.Success(Unit)
    }
}
