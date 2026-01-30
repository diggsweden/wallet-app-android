package se.digg.wallet.core.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import se.digg.wallet.feature.credentialdetails.CredentialDetailsScreen
import se.digg.wallet.feature.dashboard.DashboardScreen
import se.digg.wallet.feature.enrollment.EnrollmentScreen
import se.digg.wallet.feature.enrollment.intro.IntroScreen
import se.digg.wallet.feature.issuance.IssuanceScreen
import se.digg.wallet.feature.presentation.PresentationScreen
import se.digg.wallet.feature.settings.SettingsScreen

@Composable
fun WalletNavHost(
    onLogout: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isEnrolled: Boolean = false,
) {
    val startDestination = if (isEnrolled) RootGraph.DASHBOARD else RootGraph.ENROLLMENT

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
    ) {
        composable(
            route = "homescreen?credentialOfferUri={credential_offer_uri}",
            arguments =
                listOf(
                    navArgument("credential_offer_uri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            deepLinks =
                listOf(
                    // TODO look into uriPattern
                    navDeepLink {
                        uriPattern =
                            "openid-credential-offer://?credential_offer={credential_offer_uri}"
                    },
                ),
        ) { backStackEntry ->
            val fullUri = backStackEntry.deepLinkUri()
            IssuanceScreen(navController = navController, fullUri.toString())
        }

        // Enrolled
        navigation(
            startDestination = NavigationItem.Home.route,
            route = RootGraph.DASHBOARD,
        ) {
            composable(NavigationItem.Home.route) {
                DashboardScreen(navController = navController)
            }

            composable(NavigationItem.CredentialDetails.route) {
                CredentialDetailsScreen(navController = navController)
            }

            composable(NavigationItem.Settings.route) {
                SettingsScreen(navController = navController, onLogout = { onLogout.invoke() })
            }

            composable(
                route = "presentation_eudi",
                arguments =
                    listOf(
                        navArgument("requestUri") {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
                deepLinks =
                    listOf(
                        navDeepLink {
                            uriPattern = "eudi-openid4vp://?{requestUri}"
                        },
                    ),
            ) { backStackEntry ->
                val fullUri = backStackEntry.deepLinkUri()
                PresentationScreen(navController = navController, fullUri = fullUri.toString())
            }
            composable(
                route = "presentation",
                arguments =
                    listOf(
                        navArgument("requestUri") {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
                deepLinks =
                    listOf(
                        navDeepLink {
                            uriPattern = "openid4vp://?{requestUri}"
                        },
                    ),
            ) { backStackEntry ->
                val fullUri = backStackEntry.deepLinkUri()
                PresentationScreen(navController = navController, fullUri = fullUri.toString())
            }
        }

        // Non-Enrolled
        navigation(
            startDestination = EnrollmentNavItem.Intro.route,
            route = RootGraph.ENROLLMENT,
        ) {
            composable(EnrollmentNavItem.Intro.route) {
                IntroScreen(navController = navController, onContinue = {
                    navController.navigate(EnrollmentNavItem.Onboarding.route)
                })
            }
            composable(EnrollmentNavItem.Onboarding.route) {
                EnrollmentScreen(navController = navController, onFinish = {
                    navController.navigate(NavigationItem.Home.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}

fun NavBackStackEntry.deepLinkIntent(): Intent? =
    arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT)

fun NavBackStackEntry.deepLinkUri(): Uri? = deepLinkIntent()?.data
