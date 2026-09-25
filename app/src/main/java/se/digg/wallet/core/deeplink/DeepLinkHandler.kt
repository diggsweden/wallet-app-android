// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.deeplink

import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import se.digg.wallet.core.webauth.BrowserWebAuthenticator

@Singleton
class DeepLinkHandler @Inject constructor(private val webAuthenticator: BrowserWebAuthenticator) {

    fun handle(intent: Intent): DeepLinkResult {
        val uri = intent.data ?: return DeepLinkResult.Consumed

        return if (webAuthenticator.onDeepLink(uri)) {
            DeepLinkResult.Consumed
        } else {
            DeepLinkResult.Unhandled(uri)
        }
    }
}

sealed interface DeepLinkResult {
    data object Consumed : DeepLinkResult
    data class Unhandled(val uri: Uri) : DeepLinkResult
}
