// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.oauth

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import se.digg.wallet.core.getCustomTabsProvider
import timber.log.Timber

@Singleton
class OAuthCoordinator @Inject constructor(@ApplicationContext private val context: Context) :
    AuthorizationLauncher {

    private var pendingAuthResult: CompletableDeferred<OAuthResult>? = null

    fun hasOngoingPendingResult(): Boolean = pendingAuthResult != null

    override suspend fun authorize(
        url: String,
        redirectScheme: String,
        launchAuthTab: LaunchAuthTab,
    ): OAuthResult {
        check(pendingAuthResult == null) {
            "Auth session already ongoing"
        }

        val deferred = CompletableDeferred<OAuthResult>()
        pendingAuthResult = deferred

        try {
            val provider = getCustomTabsProvider(context)
            val uri = url.toUri()
            when {
                provider != null && CustomTabsClient.isAuthTabSupported(context, provider) -> {
                    Timber.d("OauthCoordinator - AuthTab started")
                    launchAuthTab(url, redirectScheme)
                }

                provider != null -> {
                    Timber.d("OauthCoordinator - CustomTabs started")
                    launchCustomTab(uri)
                }

                else -> {
                    Timber.d("OauthCoordinator - External browser started")
                    launchExternalBrowser(uri)
                }
            }

            return withTimeout(120_000) {
                deferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            return OAuthResult.Failure("Timeout")
        } finally {
            pendingAuthResult = null
        }
    }

    private fun launchExternalBrowser(url: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, url).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun launchCustomTab(url: Uri) {
        val customTabs = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .setUrlBarHidingEnabled(true)
            .build()
        customTabs.intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        customTabs.launchUrl(
            context,
            url,
        )
    }

    fun onResult(result: AuthTabIntent.AuthResult) {
        val response = when (result.resultCode) {
            AuthTabIntent.RESULT_CANCELED -> {
                OAuthResult.Cancelled
            }

            AuthTabIntent.RESULT_OK -> {
                result.resultUri?.let {
                    OAuthResult.Success(it.toString())
                } ?: OAuthResult.Failure("Missing redirect URI")
            }

            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
            -> {
                OAuthResult.Failure("Verification failed")
            }

            else -> {
                OAuthResult.Failure("Unknown error")
            }
        }

        pendingAuthResult?.complete(response)
    }

    fun onDeepLink(uri: Uri) {
        pendingAuthResult?.complete(OAuthResult.Success(uri.toString()))
    }
}

sealed interface OAuthResult {
    data class Success(val redirectUri: String) : OAuthResult
    data object Cancelled : OAuthResult
    data class Failure(val message: String) : OAuthResult
}
