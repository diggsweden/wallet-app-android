package se.digg.wallet.core.deeplink

import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import se.digg.wallet.core.oauth.OAuthCoordinator

@Singleton
class DeepLinkHandler @Inject constructor(private val oauthCoordinator: OAuthCoordinator) {

    fun handle(intent: Intent): DeepLinkResult {
        val uri = intent.data ?: return DeepLinkResult.Consumed

        if (oauthCoordinator.hasOngoingPendingResult()) {
            oauthCoordinator.onDeepLink(uri)
            return DeepLinkResult.Consumed
        }

        return DeepLinkResult.Unhandled(uri)
    }
}

sealed interface DeepLinkResult {
    data object Consumed : DeepLinkResult
    data class Unhandled(val uri: Uri) : DeepLinkResult
}
