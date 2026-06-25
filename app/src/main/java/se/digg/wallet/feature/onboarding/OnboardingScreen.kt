// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.AnimatedLinearProgress
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.issuance.OnboardingIssuanceRoute
import se.digg.wallet.feature.onboarding.pidsetup.PidSetupRoute
import se.digg.wallet.feature.onboarding.pin.PinSetupRoute
import se.digg.wallet.feature.onboarding.walletsetup.WalletSetupRoute

@Composable
fun OnboardingRoute(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is OnboardingUiEvent.LocalStorageCleared) {
                currentOnBack()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        if (uiState.enableBack.contains(uiState.currentStep)) {
            viewModel.onAction(OnboardingAction.Back(uiState.currentStep))
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = { action ->
            if (action is OnboardingAction.Finish) onFinish() else viewModel.onAction(action)
        },
    )
}

@Composable
private fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStep = uiState.currentStep.ordinal + 1
    val progress = currentStep.toFloat() / uiState.totalSteps.toFloat()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {},
                navigationIcon = {
                    Row(Modifier.fillMaxWidth()) {
                        if (uiState.enableBack.contains(uiState.currentStep)) {
                            IconButton(
                                onClick = { onAction(OnboardingAction.Back(uiState.currentStep)) },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_left),
                                    contentDescription = null,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { onAction(OnboardingAction.Close) }) {
                            Icon(
                                painter = painterResource(R.drawable.close_x),
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(
                        R.string.onboarding_step_title,
                        currentStep,
                        uiState.totalSteps,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedLinearProgress(
                    targetProgress = progress,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal

                        val slideIn = slideInHorizontally(
                            animationSpec = tween(durationMillis = 250),
                        ) { fullWidth ->
                            if (forward) fullWidth else -fullWidth
                        } + fadeIn(animationSpec = tween(250))

                        val slideOut = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 250),
                        ) { fullWidth ->
                            if (forward) -fullWidth else fullWidth
                        } + fadeOut(animationSpec = tween(250))

                        slideIn togetherWith slideOut using SizeTransform(clip = false)
                    },
                    label = "OnboardingStepTransition",
                ) { contentStep ->
                    OnboardingStepContent(
                        pageNumber = currentStep,
                        step = contentStep,
                        capturedPin = uiState.capturedPin,
                        onAction = onAction,
                    )
                }
            }
        },
    )
}

@Composable
fun OnboardingStepContent(
    pageNumber: Int,
    step: OnboardingStep,
    capturedPin: String,
    onAction: (OnboardingAction) -> Unit,
) {
    when (step) {
        OnboardingStep.SETUP_PIN -> PinSetupRoute(
            pageNumber = pageNumber,
            onPinEntered = { onAction(OnboardingAction.PinEntered(it, step)) },
        )

        OnboardingStep.VERIFY_PIN -> PinSetupRoute(
            pageNumber = pageNumber,
            verifyPin = true,
            onPinEntered = {},
            onPinVerified = { onAction(OnboardingAction.PinVerified(it, step)) },
            onBack = { onAction(OnboardingAction.Back(step)) },
        )

        OnboardingStep.SETUP_WALLET -> WalletSetupRoute(
            pageNumber = pageNumber,
            pin = capturedPin,
            onNext = { onAction(OnboardingAction.Next(step)) },
        )

        OnboardingStep.SETUP_PID -> PidSetupRoute(
            onNext = { onAction(OnboardingAction.Finish) },
            onCredentialOfferFetch = {
                onAction(OnboardingAction.CredentialOfferFetched(it, step))
            },
            pageNumber = pageNumber,
        )

        OnboardingStep.CREDENTIAL_OFFER -> OnboardingIssuanceRoute(
            onBack = {},
            onFinish = { onAction(OnboardingAction.Finish) },
            pageNumber = pageNumber,
        )
    }
}

@Composable
@PreviewsWallet
private fun EnrollmentPreview() {
    WalletPreview {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = OnboardingStep.SETUP_PIN),
            onAction = {},
        )
    }
}
