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
    private var pendingAuthResult: CompletableDeferred<OAuthResult>? = null

    suspend fun authorize(
        url: Uri,
        launchAuthTab: LaunchAuthTab,
        redirectScheme: String,
    ): OAuthResult {
        check(pendingAuthResult == null) {
            "Auth session already ongoing"
        }

        pendingAuthResult = CompletableDeferred()

        return try {
            launchAuthTab(url, redirectScheme)

            withTimeout(120_000) {
                pendingAuthResult?.await()
                    ?: OAuthResult.Failure("No pending auth result found")
            }
        } catch (e: Exception) {
            OAuthResult.Failure("Timeout or unexpected error")
        } finally {
            pendingAuthResult = null
        }
    }

    fun onResult(result: AuthTabIntent.AuthResult) {
        val response = when (result.resultCode) {
            AuthTabIntent.RESULT_CANCELED -> {
                OAuthResult.Cancelled
            }

            AuthTabIntent.RESULT_OK -> {
                result.resultUri?.let {
                    OAuthResult.Success(it)
                } ?: OAuthResult.Failure("Inloggning misslyckades")
            }

            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
            -> {
                OAuthResult.Failure("Inloggning misslyckades")
            }

            else -> {
                OAuthResult.Failure("Något gick fel")
            }
        }

        pendingAuthResult?.complete(response)
    }
}

sealed interface OAuthResult {
    data class Success(val uri: Uri) : OAuthResult
    data object Cancelled : OAuthResult
    data class Failure(val message: String) : OAuthResult
}
