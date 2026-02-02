// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

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

enum class EnrollmentNavigation {
    INTRO,
    ONBOARDING,
}

sealed class EnrollmentNavItem(val route: String) {
    object Intro : EnrollmentNavItem(route = EnrollmentNavigation.INTRO.name)
    object Onboarding : EnrollmentNavItem(route = EnrollmentNavigation.ONBOARDING.name)
}

object RootGraph {
    const val ENROLLMENT = "enrollment_graph"
    const val DASHBOARD = "dashboard_graph"
}
