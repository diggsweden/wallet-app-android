// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import se.digg.wallet.feature.credentialdetails.CredentialDetailsRoute
import se.digg.wallet.feature.dashboard.DashboardRoute
import se.digg.wallet.feature.issuance.DeepLinkedIssuanceRoute
import se.digg.wallet.feature.onboarding.OnboardingRoute
import se.digg.wallet.feature.onboarding.intro.IntroRoute
import se.digg.wallet.feature.presentation.PresentationRoute
import se.digg.wallet.feature.settings.SettingsRoute

@Composable
fun WalletNavDisplay(navigator: WalletNavigator, modifier: Modifier = Modifier) {
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        },
        popTransitionSpec = {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        },
        predictivePopTransitionSpec = {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        },
        entryProvider = entryProvider {
            entry<IntroKey> {
                IntroRoute(
                    onContinue = { navigator.navigate(OnboardingKey) },
                )
            }
            entry<OnboardingKey> {
                OnboardingRoute(
                    onBack = { navigator.goBack() },
                    onFinish = { navigator.resetToDashboard() },
                )
            }
            entry<DashboardKey> {
                DashboardRoute(
                    onCredentialClick = { navigator.navigate(CredentialDetailsKey(it)) },
                    onSettingsClick = { navigator.navigate(SettingsKey) },
                )
            }
            entry<CredentialDetailsKey> { key ->
                CredentialDetailsRoute(
                    credentialId = key.id,
                    onBack = { navigator.goBack() },
                )
            }
            entry<SettingsKey> {
                SettingsRoute(
                    onBack = { navigator.goBack() },
                    onLogout = { navigator.resetToOnboarding() },
                )
            }
            entry<IssuanceDeepLinkKey> { key ->
                DeepLinkedIssuanceRoute(
                    onBackClick = { navigator.goBack() },
                    onFinishClick = { navigator.goBack() },
                    credentialOfferUri = key.fullUri,
                )
            }
            entry<PresentationKey> { key ->
                PresentationRoute(
                    onBack = { navigator.goBack() },
                    onFinish = { navigator.goBack() },
                    onPopBack = { navigator.goBack() },
                    fullUri = key.fullUri,
                )
            }
        },
    )
}
