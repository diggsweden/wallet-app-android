// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object RegisterPinKey : NavKey

@Serializable
data class CredentialDetailsKey(val id: String) : NavKey

@Serializable
data object IntroKey : NavKey

@Serializable
data object OnboardingKey : NavKey

@Serializable
data class IssuanceDeepLinkKey(val fullUri: String) : NavKey

@Serializable
data class PresentationEudiKey(val fullUri: String) : NavKey

@Serializable
data class PresentationKey(val fullUri: String) : NavKey

fun Uri.toNavKey(): NavKey? = when (scheme) {
    "openid-credential-offer" -> IssuanceDeepLinkKey(toString())
    "eudi-openid4vp" -> PresentationEudiKey(toString())
    "openid4vp" -> PresentationKey(toString())
    else -> null
}
