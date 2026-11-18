// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Singleton
import se.digg.wallet.core.network.di.GatewayHttpClient
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.UtilV0ClientLogsClient
import se.wallet.client.gateway.models.ClientLogRequest

@Singleton
class GatewayLogSender @Inject constructor(@GatewayHttpClient httpClient: HttpClient) : LogSender {
    private val client = UtilV0ClientLogsClient(httpClient)

    override suspend fun send(request: ClientLogRequest): NetworkResult<Unit> =
        client.createClientLog(request)
}
