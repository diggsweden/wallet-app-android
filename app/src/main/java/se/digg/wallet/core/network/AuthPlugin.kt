package se.digg.wallet.core.network

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

data class AuthPluginConfig(
    var accountId: String = ""
)

val authPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val sessionManager = SessionManager(
        challengeClient = PublicAuthSessionChallengeClient(client),
        validateClient = PublicAuthSessionResponseClient(client),
        accountId = pluginConfig.accountId
    )

    on(Send) { request ->
        val url = request.url.toString()
        if (url.contains("session") || url.contains("account")) {
            return@on proceed(request)
        }

        request.headers.append("X-SESSION-ID", sessionManager.getToken())
        return@on proceed(request)
    }
}
