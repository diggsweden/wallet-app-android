// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.oauth

import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

@Singleton
class OAuthCoordinator @Inject constructor() {
    private var pendingAuthResult: CompletableDeferred<Uri>? = null

    suspend fun authorize(url: Uri, launchAuthTab: LaunchAuthTab, redirectScheme: String): Uri {
        check(pendingAuthResult == null) {
            "Auth session already ongoing"
        }

        pendingAuthResult = CompletableDeferred()

        try {
            launchAuthTab(url, redirectScheme)
            return withTimeout(120_000) {
                pendingAuthResult?.await() ?: throw Exception("No pending auth result found")
            }
        } finally {
            pendingAuthResult = null
        }
    }

    fun onResult(result: AuthTabIntent.AuthResult) {
        when (result.resultCode) {
            AuthTabIntent.RESULT_CANCELED -> {
                pendingAuthResult?.completeExceptionally(Error("Inloggning avbruten"))
            }

            AuthTabIntent.RESULT_OK -> {
                result.resultUri?.let { pendingAuthResult?.complete(it) }
                    ?: pendingAuthResult?.completeExceptionally(Error("Inloggning misslyckades"))
            }

            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
            -> {
                pendingAuthResult?.completeExceptionally(Error("Inloggning misslyckades"))
            }

            else -> {
                pendingAuthResult?.completeExceptionally(Error("Något gick fel"))
            }
        }
    }
}
