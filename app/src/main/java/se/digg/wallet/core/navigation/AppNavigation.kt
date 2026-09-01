// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DashboardKey : NavKey

@Serializable
data class SettingsKey(val isFromIntro: Boolean = false) : NavKey

@Serializable
data object AboutKey : NavKey

@Serializable
data object LanguageKey : NavKey

@Serializable
data object ThemeKey : NavKey

@Serializable
data object HelpKey : NavKey

@Serializable
data object LicensesKey : NavKey

@Serializable
data class CredentialDetailsKey(val id: String) : NavKey

@Serializable
data object IntroKey : NavKey

@Serializable
data object OnboardingKey : NavKey

@Serializable
data class IssuanceDeepLinkKey(val fullUri: String) : NavKey

@Serializable
data class PresentationKey(val fullUri: String) : NavKey

fun Uri.toNavKey(): NavKey? = when (scheme) {
    "openid-credential-offer", "haip-vci" -> IssuanceDeepLinkKey(toString())
    "eudi-openid4vp", "openid4vp", "haip-vp" -> PresentationKey(toString())
    else -> null
}
