// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.oauth

interface AuthorizationLauncher {
    /**
     * Opens [url] and suspends until a redirect to [redirectScheme] arrives, the user cancels,
     * or the session times out.
     */
    suspend fun authorize(
        url: String,
        redirectScheme: String,
        launchAuthTab: LaunchAuthTab,
    ): OAuthResult
}
