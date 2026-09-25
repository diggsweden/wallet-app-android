// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

fun ComponentActivity.hostWebAuthenticator(authenticator: BrowserWebAuthenticator) {
    val authTabLauncher = AuthTabIntent.registerActivityResultLauncher(this) { result ->
        authenticator.onAuthTabResult(result)
    }

    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            authenticator.browserRequests.collect { request ->
                try {
                    launchBrowser(request, authTabLauncher)
                } catch (e: Exception) {
                    authenticator.onLaunchFailed(e)
                }
            }
        }
    }

    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                if (!isChangingConfigurations) {
                    authenticator.onHostPaused()
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                authenticator.onHostResumed()
            }
        },
    )
}

private fun Activity.launchBrowser(
    request: BrowserRequest,
    authTabLauncher: ActivityResultLauncher<Intent>,
) {
    when (request) {
        is BrowserRequest.AuthTab -> {
            AuthTabIntent.Builder()
                .build()
                .launch(authTabLauncher, request.url, request.callbackScheme)
        }

        is BrowserRequest.CustomTab -> {
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(this, request.url)
        }

        is BrowserRequest.External -> {
            startActivity(
                Intent(Intent.ACTION_VIEW, request.url).addCategory(Intent.CATEGORY_BROWSABLE),
            )
        }
    }
}
