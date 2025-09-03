package se.digg.wallet.core.navigation

enum class AppNavigation {
    HOME, CREDENTIAL_DETAILS
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(route = AppNavigation.HOME.name)
    object CredentialDetails : NavigationItem(route = AppNavigation.CREDENTIAL_DETAILS.name)
}
