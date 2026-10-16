// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.AnimatedLinearProgress
import se.digg.wallet.core.designsystem.component.WalletBackButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
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
    var closeVisible by remember(uiState.currentStep) { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            OnboardingTopBar(
                backVisible = uiState.enableBack.contains(uiState.currentStep),
                closeVisible = closeVisible,
                onBackClick = { onAction(OnboardingAction.Back(uiState.currentStep)) },
                onCloseClick = { onAction(OnboardingAction.Close) },
            )
        },
        content = { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                OnboardingProgress(currentStep = currentStep, totalSteps = uiState.totalSteps)
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = { stepTransition() },
                    label = "OnboardingStepTransition",
                ) { contentStep ->
                    OnboardingStepContent(
                        pageNumber = currentStep,
                        step = contentStep,
                        capturedPin = uiState.capturedPin,
                        credentialOfferUri = uiState.credentialOfferUri,
                        onAction = onAction,
                        onCloseVisibleChange = { closeVisible = it },
                    )
                }
            }
        },
    )
}

@Composable
private fun OnboardingTopBar(
    backVisible: Boolean,
    closeVisible: Boolean,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    WalletTopAppBar(
        navigationIcon = {
            Row(Modifier.fillMaxWidth()) {
                if (backVisible) {
                    WalletBackButton(onClick = onBackClick)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (closeVisible) {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(R.drawable.close_x),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun OnboardingProgress(currentStep: Int, totalSteps: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.onboarding_step_title, currentStep, totalSteps),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedLinearProgress(
            targetProgress = currentStep.toFloat() / totalSteps.toFloat(),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/** Slides forward steps in from the right and backward steps in from the left. */
private fun AnimatedContentTransitionScope<OnboardingStep>.stepTransition(): ContentTransform {
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

    return slideIn togetherWith slideOut using SizeTransform(clip = false)
}

@Composable
private fun OnboardingStepContent(
    pageNumber: Int,
    step: OnboardingStep,
    capturedPin: String,
    credentialOfferUri: String,
    onAction: (OnboardingAction) -> Unit,
    onCloseVisibleChange: (Boolean) -> Unit,
) {
    when (step) {
        OnboardingStep.SETUP_PIN -> PinSetupRoute(
            onPinEnter = { onAction(OnboardingAction.PinEntered(it, step)) },
        )

        OnboardingStep.VERIFY_PIN -> PinSetupRoute(
            verifyPin = true,
            onPinEnter = {},
            onPinVerify = { onAction(OnboardingAction.PinVerified(it, step)) },
            onBack = { onAction(OnboardingAction.Back(step)) },
        )

        OnboardingStep.SETUP_WALLET -> WalletSetupRoute(
            pin = capturedPin,
            onNext = { onAction(OnboardingAction.Next(step)) },
            onCancel = { onAction(OnboardingAction.Close) },
        )

        OnboardingStep.SETUP_PID -> PidSetupRoute(
            onNext = { onAction(OnboardingAction.Finish) },
            onCredentialOfferFetch = {
                onAction(OnboardingAction.CredentialOfferFetched(it, step))
            },
            pageNumber = pageNumber,
        )

        OnboardingStep.CREDENTIAL_OFFER -> OnboardingIssuanceRoute(
            credentialOfferUri = credentialOfferUri,
            onFinish = { onAction(OnboardingAction.Finish) },
            onDismissibleChange = onCloseVisibleChange,
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
