// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.models.ClientLogRequest

interface LogSender {
    suspend fun send(request: ClientLogRequest): NetworkResult<Unit>
}
