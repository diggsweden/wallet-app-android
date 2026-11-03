// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.webauth

import android.net.Uri

sealed interface BrowserRequest {
    val url: Uri

    data class AuthTab(override val url: Uri, val callbackScheme: String) : BrowserRequest

    data class CustomTab(override val url: Uri) : BrowserRequest

    data class External(override val url: Uri) : BrowserRequest
}
