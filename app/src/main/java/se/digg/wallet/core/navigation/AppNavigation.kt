package se.digg.wallet.core.navigation

enum class AppNavigation {
    HOME
}

sealed class NavigationItem(val route: String) {
    object Home : NavigationItem(AppNavigation.HOME.name)
}
