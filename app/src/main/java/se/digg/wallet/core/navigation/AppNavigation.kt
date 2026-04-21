// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.digg.wallet.data.SavedCredential

enum class AppNavigation {
    HOME,
    CREDENTIAL_DETAILS,
    SETTINGS,
    ISSUANCE,
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(route = AppNavigation.HOME.name)
    object CredentialDetails : NavigationItem(route = AppNavigation.CREDENTIAL_DETAILS.name)
    object Settings : NavigationItem(route = AppNavigation.SETTINGS.name)
}

enum class OnboardingNavigation {
    INTRO,
    ONBOARDING,
}

@Serializable
data class CredentialDetailsRoute(val id: String)

sealed class OnboardingNavItem(val route: String) {
    object Intro : OnboardingNavItem(route = OnboardingNavigation.INTRO.name)
    object Onboarding : OnboardingNavItem(route = OnboardingNavigation.ONBOARDING.name)
}

object RootGraph {
    const val ONBOARDING = "onboarding_graph"
    const val DASHBOARD = "dashboard_graph"
}
