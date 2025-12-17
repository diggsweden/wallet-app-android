// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import se.digg.wallet.feature.enrollment.OnboardingEffect
import se.digg.wallet.feature.enrollment.OnboardingEvent
import se.digg.wallet.feature.enrollment.OnboardingViewModel
import se.digg.wallet.feature.enrollment.contactinfo.ContactInfoScreen
import se.digg.wallet.feature.enrollment.done.DoneScreen
import se.digg.wallet.feature.enrollment.intro.IntroScreen
import se.digg.wallet.feature.enrollment.pin.PinScreen
import se.digg.wallet.feature.enrollment.activation.WalletSetupScreen
import se.digg.wallet.feature.enrollment.terms.TermsScreen

const val ONBOARDING_GRAPH = "onboarding"

@Composable
fun EnrollmentNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: OnboardingViewModel = viewModel(),
    onFinish: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OnboardingEffect.Navigate -> {
                    navController.navigate(route = effect.route)
                }

                OnboardingEffect.NavigateToDashboard -> {}
            }
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = OnboardingNavItem.Intro.route
    ) {
        composable(OnboardingNavItem.Intro.route) {
            IntroScreen(navController = navController, onContinue = {
                viewModel.onEvent(
                    OnboardingEvent.Continue
                )
            })
        }
        composable(OnboardingNavItem.Terms.route) {
            TermsScreen(navController = navController, onContinue = {
                viewModel.onEvent(
                    OnboardingEvent.Continue
                )
            })
        }
        composable(OnboardingNavItem.ContactInfo.route) {
            ContactInfoScreen(navController = navController, onContinue = {
                viewModel.onEvent(
                    OnboardingEvent.Continue
                )
            })
        }
        composable(OnboardingNavItem.Pin.route) {
            PinScreen(navController = navController, onContinue = {
                viewModel.onEvent(
                    OnboardingEvent.Continue
                )
            })
        }
        composable(OnboardingNavItem.Wua.route) {
            WalletSetupScreen(navController = navController, onContinue = {
                viewModel.onEvent(
                    OnboardingEvent.Continue
                )
            })
        }
        composable(OnboardingNavItem.Done.route) {
            DoneScreen(navController = navController, onFinish = {
                onFinish.invoke()
            })
        }
    }
}
