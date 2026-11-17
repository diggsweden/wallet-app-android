// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import javax.inject.Inject
import javax.inject.Singleton
import se.digg.wallet.core.network.NetworkConfig
import se.digg.wallet.core.network.di.BaseHttpClient
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.client.UtilV0ClientLogsClient
import se.wallet.client.gateway.models.ClientLogRequest

@Singleton
class GatewayLogSender @Inject constructor(
    @BaseHttpClient base: HttpClient,
    networkConfig: NetworkConfig,
) : LogSender {
    private val client = UtilV0ClientLogsClient(
        base.config {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = networkConfig.baseUrl
                }
                header("X-API-KEY", networkConfig.apiKey)
            }
        },
    )

    override suspend fun send(request: ClientLogRequest): NetworkResult<Unit> =
        client.createClientLog(request)
}
