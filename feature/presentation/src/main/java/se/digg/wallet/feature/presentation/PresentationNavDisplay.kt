// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import se.digg.wallet.core.navigation.slidePopTransitionSpec
import se.digg.wallet.core.navigation.slidePredictivePopTransitionSpec
import se.digg.wallet.core.navigation.slideTransitionSpec

@Composable
internal fun PresentationNavDisplay(
    uiState: PresentationUiState,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onShareClick: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onBackToReview: () -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(PresentationDestination.Review)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        transitionSpec = slideTransitionSpec(),
        popTransitionSpec = slidePopTransitionSpec(),
        predictivePopTransitionSpec = slidePredictivePopTransitionSpec(),
        entryProvider = entryProvider {
            entry<PresentationDestination.Review> {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = stepTransitionSpec(),
                    contentKey = { it::class },
                    label = "presentationStep",
                ) { state ->
                    PresentationScreen(
                        uiState = state,
                        onBackClick = onBack,
                        onShareClick = onShareClick,
                        onFinishClick = onFinish,
                        onSubmitPin = onSubmitPin,
                        onBackToReview = onBackToReview,
                        onOptionalClaimClick = onOptionalClaimClick,
                        onRecipientClick = {
                            backStack.add(PresentationDestination.Verification)
                        },
                        onSharedClaimsClick = {
                            backStack.add(PresentationDestination.SharedClaims)
                        },
                    )
                }
            }
            entry<PresentationDestination.SharedClaims> {
                PresentationSharedClaimsRoute(
                    uiState = uiState,
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
            entry<PresentationDestination.Verification> {
                PresentationVerificationRoute(
                    uiState = uiState,
                    onTechnicalDetailsClick = {
                        backStack.add(PresentationDestination.TechnicalDetails)
                    },
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
            entry<PresentationDestination.TechnicalDetails> {
                PresentationTechnicalDetailsRoute(
                    uiState = uiState,
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
