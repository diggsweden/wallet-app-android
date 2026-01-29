package se.digg.wallet.core.network

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
}

val authPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    on(Send) { request ->
        if (request.url.encodedPath.startsWith("/oidc") ) {
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
