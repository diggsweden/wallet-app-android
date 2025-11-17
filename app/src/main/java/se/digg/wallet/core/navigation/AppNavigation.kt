package se.digg.wallet.core.navigation

enum class AppNavigation {
    HOME, CREDENTIAL_DETAILS, SETTINGS
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(route = AppNavigation.HOME.name)
    object CredentialDetails : NavigationItem(route = AppNavigation.CREDENTIAL_DETAILS.name)
    object Settings : NavigationItem(route = AppNavigation.SETTINGS.name)
}

enum class EnrollmentNavigation {
    INTRO, ONBOARDING, TERMS, CONTACT, PIN, WUA, PID, DONE
}

sealed class EnrollmentNavItem(val route: String) {
    object Intro : EnrollmentNavItem(route = EnrollmentNavigation.INTRO.name)
    object Onboarding : EnrollmentNavItem(route = EnrollmentNavigation.ONBOARDING.name)
    object Terms : EnrollmentNavItem(route = EnrollmentNavigation.TERMS.name)
    object ContactInfo : EnrollmentNavItem(route = EnrollmentNavigation.CONTACT.name)
    object Pin : EnrollmentNavItem(route = EnrollmentNavigation.PIN.name)
    object Wua : EnrollmentNavItem(route = EnrollmentNavigation.WUA.name)
    object Pid : EnrollmentNavItem(route = EnrollmentNavigation.PID.name)
    object Done : EnrollmentNavItem(route = EnrollmentNavigation.DONE.name)
}

object RootGraph {
    const val Enrollment = "enrollment_graph"
    const val Dashboard = "dashboard_graph"
    const val Issuance = "issuance_graph"
    const val Presentation = "presentation_graph"
}