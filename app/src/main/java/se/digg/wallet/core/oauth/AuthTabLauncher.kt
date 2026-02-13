// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.oauth

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

typealias LaunchAuthTab = (url: Uri, redirectScheme: String) -> Unit

val LocalAuthTabLauncher = staticCompositionLocalOf<LaunchAuthTab> {
    error("No AuthTabLauncher provided")
}

@Composable
fun ProvideAuthTabLauncher(launcher: LaunchAuthTab, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAuthTabLauncher provides launcher, content)
}
