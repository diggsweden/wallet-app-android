// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

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
import androidx.navigation.toRoute
import se.digg.wallet.feature.credentialdetails.CredentialDetailsRoute
import se.digg.wallet.feature.dashboard.DashboardRoute
import se.digg.wallet.feature.issuance.DeepLinkedIssuanceRoute
import se.digg.wallet.feature.onboarding.OnboardingRoute
import se.digg.wallet.feature.onboarding.intro.IntroRoute
import se.digg.wallet.feature.presentation.PresentationRoute
import se.digg.wallet.feature.settings.SettingsRoute

@Composable
fun WalletNavHost(
    onLogout: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isEnrolled: Boolean = false,
) {
    val startDestination = if (isEnrolled) RootGraph.DASHBOARD else RootGraph.ONBOARDING

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
            DeepLinkedIssuanceRoute(
                onBackClick = { navController.navigateUp() },
                onFinishClick = { navController.navigateUp() },
                credentialOfferUri = fullUri.toString(),
            )
        }

        // Enrolled
        navigation(
            startDestination = NavigationItem.Home.route,
            route = RootGraph.DASHBOARD,
        ) {
            composable(NavigationItem.Home.route) {
                DashboardRoute(navController = navController)
            }

            composable<CredentialDetailsRoute> {
                val credential = it.toRoute<CredentialDetailsRoute>()
                CredentialDetailsRoute(
                    credentialId = credential.id,
                    navController = navController,
                )
            }

            composable(NavigationItem.Settings.route) {
                SettingsRoute(navController = navController, onLogout = { onLogout.invoke() })
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
                PresentationRoute(navController = navController, fullUri = fullUri.toString())
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
                PresentationRoute(navController = navController, fullUri = fullUri.toString())
            }
        }

        // Non-Enrolled
        navigation(
            startDestination = OnboardingNavItem.Intro.route,
            route = RootGraph.ONBOARDING,
        ) {
            composable(OnboardingNavItem.Intro.route) {
                IntroRoute(
                    onContinue = {
                        navController.navigate(OnboardingNavItem.Onboarding.route)
                    },
                )
            }
            composable(OnboardingNavItem.Onboarding.route) {
                OnboardingRoute(
                    navController = navController,
                    onFinish = {
                        navController.navigate(NavigationItem.Home.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

fun NavBackStackEntry.deepLinkIntent(): Intent? =
    arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT)

fun NavBackStackEntry.deepLinkUri(): Uri? = deepLinkIntent()?.data
