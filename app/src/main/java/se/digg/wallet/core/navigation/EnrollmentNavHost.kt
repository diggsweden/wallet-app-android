package se.digg.wallet.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import se.digg.wallet.feature.enrollment.OnboardingViewModel

const val ONBOARDING_GRAPH = "onboarding"

@Composable
fun EnrollmentNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: OnboardingViewModel = viewModel(),
    onFinish: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
/*
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

 */
/*
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
 */
}
