// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

import android.content.Context
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import se.digg.wallet.core.getCustomTabsProvider
import timber.log.Timber

@Singleton
class BrowserWebAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
) : WebAuthenticator {

    private class PendingSession(val request: BrowserRequest, val callbackScheme: String) {
        val result = CompletableDeferred<WebAuthResult>()
        var browserShown = false
    }

    private var pendingSession: PendingSession? = null

    private val _browserRequests = Channel<BrowserRequest>(Channel.CONFLATED)

    val browserRequests: Flow<BrowserRequest> = _browserRequests.receiveAsFlow()

    override suspend fun authenticate(url: String, callbackScheme: String): WebAuthResult {
        pendingSession?.result?.complete(WebAuthResult.Cancelled)

        val request = browserRequestFor(url.toUri(), callbackScheme)
        val session = PendingSession(request, callbackScheme)
        pendingSession = session

        try {
            Timber.d("BrowserWebAuthenticator - ${request::class.simpleName} requested")
            _browserRequests.trySend(request)

            return session.result.await()
        } finally {
            if (pendingSession === session) {
                pendingSession = null
                _browserRequests.tryReceive()
            }
        }
    }

    private fun browserRequestFor(url: Uri, callbackScheme: String): BrowserRequest {
        val provider = getCustomTabsProvider(context)
        return when {
            provider == null -> {
                BrowserRequest.External(url)
            }

            CustomTabsClient.isAuthTabSupported(context, provider) -> {
                BrowserRequest.AuthTab(url, callbackScheme)
            }

            else -> {
                BrowserRequest.CustomTab(url)
            }
        }
    }

    fun onLaunchFailed(cause: Exception) {
        Timber.w(cause, "BrowserWebAuthenticator - Could not open the browser")
        pendingSession?.result?.complete(WebAuthResult.Failure("Could not open the browser"))
    }

    fun onAuthTabResult(result: AuthTabIntent.AuthResult) {
        val response = when (result.resultCode) {
            AuthTabIntent.RESULT_CANCELED -> {
                WebAuthResult.Cancelled
            }

            AuthTabIntent.RESULT_OK -> {
                result.resultUri?.let {
                    WebAuthResult.Success(it.toString())
                } ?: WebAuthResult.Failure("Missing callback URI")
            }

            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
            -> {
                WebAuthResult.Failure("Verification failed")
            }

            else -> {
                WebAuthResult.Failure("Unknown error")
            }
        }

        pendingSession?.result?.complete(response)
    }

    fun onDeepLink(uri: Uri): Boolean {
        val session = pendingSession ?: return false
        if (session.request is BrowserRequest.AuthTab ||
            !uri.scheme.equals(session.callbackScheme, ignoreCase = true)
        ) {
            return false
        }
        session.result.complete(WebAuthResult.Success(uri.toString()))
        return true
    }

    fun onHostPaused() {
        pendingSession?.browserShown = true
    }

    fun onHostResumed() {
        val session = pendingSession ?: return
        if (session.browserShown && session.request !is BrowserRequest.AuthTab) {
            session.result.complete(WebAuthResult.Cancelled)
        }
    }
}
