package se.digg.wallet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.util.AttributeKey
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

class AuthPlugin(val accountId: String) {
    class Config {
        var accountId: String = ""
    }

    companion object : HttpClientPlugin<Config, AuthPlugin> {
        private val IsInternalCallKey =
            AttributeKey<Boolean>("challenge-auth-internal")

        override val key: AttributeKey<AuthPlugin> =
            AttributeKey("ChallengeAuthPlugin")

        override fun prepare(block: Config.() -> Unit): AuthPlugin {
            val cfg = Config().apply(block)

            require(cfg.accountId.isNotBlank()) {
                "Account ID required"
            }

            return AuthPlugin(accountId = cfg.accountId)
        }

        override fun install(
            plugin: AuthPlugin,
            scope: HttpClient
        ) {
            val sessionManager = SessionManager(
                challengeClient = PublicAuthSessionChallengeClient(scope),
                validateClient = PublicAuthSessionResponseClient(scope),
                accountId = plugin.accountId
            )

            scope.sendPipeline.intercept(HttpSendPipeline.State) { content ->
                if (context.attributes.getOrNull(IsInternalCallKey) == true) {
                    return@intercept
                }

                context.headers.append(
                    "X-SESSION-ID",
                    sessionManager.getToken()
                )
            }
        }
    }
}