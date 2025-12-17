// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

enum class AppNavigation {
    HOME, CREDENTIAL_DETAILS, SETTINGS
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(route = AppNavigation.HOME.name)
    object CredentialDetails : NavigationItem(route = AppNavigation.CREDENTIAL_DETAILS.name)
    object Settings : NavigationItem(route = AppNavigation.SETTINGS.name)
}

enum class OnboardingNavigation {
    INTRO, ONBOARDING, TERMS, CONTACT, PIN, WUA, DONE
}

sealed class OnboardingNavItem(val route: String) {
    object Intro : OnboardingNavItem(route = OnboardingNavigation.INTRO.name)
    object Onboarding : OnboardingNavItem(route = OnboardingNavigation.ONBOARDING.name)
    object Terms : OnboardingNavItem(route = OnboardingNavigation.TERMS.name)
    object ContactInfo : OnboardingNavItem(route = OnboardingNavigation.CONTACT.name)
    object Pin : OnboardingNavItem(route = OnboardingNavigation.PIN.name)
    object Wua : OnboardingNavItem(route = OnboardingNavigation.WUA.name)
    object Done : OnboardingNavItem(route = OnboardingNavigation.DONE.name)
}
