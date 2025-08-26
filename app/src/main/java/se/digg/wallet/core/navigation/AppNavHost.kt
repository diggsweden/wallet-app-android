package se.digg.wallet.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import se.digg.wallet.feature.dashboard.DashboardScreen
import se.digg.wallet.feature.issuance.IssuanceScreen
import se.digg.wallet.feature.presentation.PresentationScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavigationItem.Home.route
) {

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Home.route) {
            DashboardScreen(navController = navController)
        }

        composable(
            route = "homescreen?credentialOfferUri={credential_offer_uri}",
            arguments = listOf(
                navArgument("credential_offer_uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        "openid-credential-offer://credential_offer?credential_offer={credential_offer_uri}"
                }
            )
        ) { backStackEntry ->
            val credentialOfferUri = backStackEntry.arguments?.getString("credential_offer_uri")
            IssuanceScreen(navController = navController, credentialOfferUri)
        }
        composable(
            route = "presentation",
            arguments = listOf(
                /*
                navArgument("clientId") { type = NavType.StringType; nullable = true },
                navArgument("requestUri") { type = NavType.StringType; nullable = true },
                navArgument("requestUriMethod") { type = NavType.StringType; nullable = true }
                 */
                navArgument("requestUri") { type = NavType.StringType; nullable = true }
            ),
            deepLinks = listOf(
                navDeepLink {
                    //uriPattern = "eudi-openid4vp://?client_id={clientId}&request_uri={requestUri}&request_uri_method={requestUriMethod}"
                    uriPattern = "eudi-openid4vp://?{requestUri}"
                }
            )
        ) { backStackEntry ->
            PresentationScreen(navController = navController)
        }
    }
}