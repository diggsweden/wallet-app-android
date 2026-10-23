// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import se.digg.wallet.core.navigation.AboutKey
import se.digg.wallet.core.navigation.AppSetupKey
import se.digg.wallet.core.navigation.CredentialDetailsKey
import se.digg.wallet.core.navigation.DashboardKey
import se.digg.wallet.core.navigation.HelpKey
import se.digg.wallet.core.navigation.IntroKey
import se.digg.wallet.core.navigation.IssuanceKey
import se.digg.wallet.core.navigation.LanguageKey
import se.digg.wallet.core.navigation.LicensesKey
import se.digg.wallet.core.navigation.OnboardingKey
import se.digg.wallet.core.navigation.PresentationKey
import se.digg.wallet.core.navigation.QrScannerKey
import se.digg.wallet.core.navigation.SettingsKey
import se.digg.wallet.core.navigation.ThemeKey
import se.digg.wallet.core.navigation.WalletNavigator
import se.digg.wallet.feature.about.AboutRoute
import se.digg.wallet.feature.about.LicensesRoute
import se.digg.wallet.feature.appsetup.AppSetupRoute
import se.digg.wallet.feature.credentialdetails.CredentialDetailsRoute
import se.digg.wallet.feature.dashboard.DashboardRoute
import se.digg.wallet.feature.issuance.DeepLinkedIssuanceRoute
import se.digg.wallet.feature.onboarding.OnboardingRoute
import se.digg.wallet.feature.onboarding.intro.IntroRoute
import se.digg.wallet.feature.presentation.PresentationRoute
import se.digg.wallet.feature.qrscanner.QrScannerRoute
import se.digg.wallet.feature.settings.HelpRoute
import se.digg.wallet.feature.settings.LanguageRoute
import se.digg.wallet.feature.settings.SettingsRoute
import se.digg.wallet.feature.settings.ThemeRoute

@Composable
fun WalletNavDisplay(
    navigator: WalletNavigator,
    initialDeepLinkKey: NavKey?,
    modifier: Modifier = Modifier,
) {
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
            entry<AppSetupKey> {
                AppSetupRoute(
                    onNavigateToIntro = { navigator.resetToOnboarding() },
                    onNavigateToDashboard = {
                        navigator.resetToDashboard()
                        initialDeepLinkKey?.let { navigator.navigate(it) }
                    },
                )
            }
            entry<IntroKey> {
                IntroRoute(
                    onContinue = { navigator.navigate(OnboardingKey) },
                    onSettingsClick = { navigator.navigate(SettingsKey(isFromIntro = true)) },
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
                    onSettingsClick = { navigator.navigate(SettingsKey()) },
                    onScanQrCodeClick = { navigator.navigate(QrScannerKey) },
                )
            }
            entry<QrScannerKey> {
                QrScannerRoute(
                    onBack = { navigator.goBack() },
                    onWalletLinkScan = { key ->
                        navigator.goBack()
                        navigator.navigate(key)
                    },
                )
            }
            entry<CredentialDetailsKey> { key ->
                CredentialDetailsRoute(
                    credentialId = key.id,
                    onBack = { navigator.goBack() },
                )
            }
            entry<SettingsKey> { key ->
                SettingsRoute(
                    isFromIntro = key.isFromIntro,
                    onBack = { navigator.goBack() },
                    onLogout = { navigator.resetToOnboarding() },
                    onAbout = { navigator.navigate(AboutKey) },
                    onLanguage = { navigator.navigate(LanguageKey) },
                    onTheme = { navigator.navigate(ThemeKey) },
                    onHelp = { navigator.navigate(HelpKey) },
                )
            }
            entry<AboutKey> {
                AboutRoute(
                    onBack = { navigator.goBack() },
                    onLicensesClick = { navigator.navigate(LicensesKey) },
                )
            }
            entry<LicensesKey> {
                LicensesRoute(onBack = { navigator.goBack() })
            }
            entry<LanguageKey> {
                LanguageRoute(onBack = { navigator.goBack() })
            }
            entry<ThemeKey> {
                ThemeRoute(onBack = { navigator.goBack() })
            }
            entry<HelpKey> {
                HelpRoute(onBack = { navigator.goBack() })
            }
            entry<IssuanceKey> { key ->
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
