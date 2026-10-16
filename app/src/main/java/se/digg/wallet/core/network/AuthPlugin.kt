// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
}

private val sessionlessPaths = setOf(
    "/v0/accounts",
    "/util/v0/client-configs",
)

val authPlugin =
    createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
        on(Send) { request ->
            if (request.url.encodedPath in sessionlessPaths) {
                return@on proceed(request)
            }

            request.headers.append("session", pluginConfig.sessionManager.getToken())

            val call = proceed(request)
            if (call.response.status == HttpStatusCode.Forbidden) {
                pluginConfig.sessionManager.reset()
            }

            return@on call
        }
    }
