@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.enrollment

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.AnimatedLinearProgress
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.enrollment.consent.ConsentScreen
import se.digg.wallet.feature.enrollment.email.EmailScreen
import se.digg.wallet.feature.enrollment.emailverify.EmailVerifyScreen
import se.digg.wallet.feature.enrollment.fetchid.FetchIdScreen
import se.digg.wallet.feature.enrollment.issuance.OnboardingIssuanceScreen
import se.digg.wallet.feature.enrollment.phone.PhoneScreen
import se.digg.wallet.feature.enrollment.phoneverify.PhoneVerifyScreen
import se.digg.wallet.feature.enrollment.pin.PinSetupScreen

@Composable
fun EnrollmentScreen(
    navController: NavController,
    onFinish: () -> Unit,
    viewModel: EnrollmentViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is EnrollmentUiEvent.LocalStorageCleared) {
                navController.popBackStack()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        if (uiState.enableBack.contains(uiState.currentStep)) {
            viewModel.goBack()
        }
    }

    EnrollmentScreen(
        uiState = uiState,
        onNext = { viewModel.goNext() },
        onBack = { viewModel.goBack() },
        onSkip = { viewModel.onSkip() },
        onCloseOnboarding = { viewModel.closeOnboarding() },
        onFinishOnboarding = { onFinish.invoke() },
        onCredentialOfferFetch = { viewModel.setFetchedCredentialOffer(it) },
    )
}

@Composable
fun EnrollmentScreen(
    uiState: EnrollmentUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onCloseOnboarding: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onCredentialOfferFetch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStep = uiState.currentStep.ordinal + 1
    val progress = currentStep.toFloat() / uiState.totalSteps.toFloat()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar =
            {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    title = {
                    },
                    navigationIcon = {
                        Row(Modifier.fillMaxWidth()) {
                            if (uiState.enableBack.contains(uiState.currentStep)) {
                                IconButton(onClick = { onBack.invoke() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_left),
                                        contentDescription = "",
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { onCloseOnboarding.invoke() }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_x),
                                    contentDescription = "",
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
                    text = "Steg $currentStep av ${uiState.totalSteps}",
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
                ) { animatedStep ->
                    OnboardingStepContent(
                        pageNumber = currentStep,
                        step = animatedStep,
                        onNext = { onNext.invoke() },
                        onBack = { onBack.invoke() },
                        onSkip = { onSkip.invoke() },
                        onFinish = { onFinishOnboarding.invoke() },
                        onCredentialOfferFetch = { onCredentialOfferFetch.invoke(it) },
                    )
                }
            }
        },
    )
}

@Composable
fun OnboardingStepContent(
    pageNumber: Int,
    step: EnrollmentStep,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onCredentialOfferFetch: (String) -> Unit,
) {
    when (step) {
        EnrollmentStep.NOTIFICATION -> ConsentScreen(
            onNext = {
                onNext.invoke()
            },
            pageNumber = pageNumber,
        )

        EnrollmentStep.PHONE_NUMBER -> PhoneScreen(
            onNext = { onNext.invoke() },
            onSkip = { onSkip.invoke() },
            pageNumber = pageNumber,
        )

        EnrollmentStep.VERIFY_PHONE -> PhoneVerifyScreen(
            onNext = { onNext.invoke() },
            pageNumber = pageNumber,
        )

        EnrollmentStep.EMAIL -> EmailScreen(onNext = { onNext.invoke() }, pageNumber = pageNumber)

        EnrollmentStep.VERIFY_EMAIL -> EmailVerifyScreen(
            onNext = { onNext.invoke() },
            pageNumber = pageNumber,
        )

        EnrollmentStep.PIN -> PinSetupScreen(
            onNext = { onNext.invoke() },
            onBack = {},
            pageNumber = pageNumber,
        )

        EnrollmentStep.VERIFY_PIN -> PinSetupScreen(
            onNext = { onNext.invoke() },
            verifyPin = true,
            onBack = { onBack.invoke() },
            pageNumber = pageNumber,
        )

        EnrollmentStep.FETCH_PID -> FetchIdScreen(
            onNext = { onFinish.invoke() },
            onCredentialOfferFetch = { onCredentialOfferFetch.invoke(it) },
            pageNumber = pageNumber,
        )

        EnrollmentStep.CREDENTIAL_OFFER -> OnboardingIssuanceScreen(
            onBack = {},
            onFinish = { onFinish.invoke() },
            pageNumber = pageNumber,
        )
    }
}

@Composable
@PreviewsWallet
private fun EnrollmentPreview() {
    WalletTheme {
        Surface {
            EnrollmentScreen(
                uiState = EnrollmentUiState(currentStep = EnrollmentStep.NOTIFICATION),
                onNext = {},
                onBack = {},
                onSkip = {},
                onCloseOnboarding = {},
                onFinishOnboarding = {},
                onCredentialOfferFetch = {},
            )
        }
    }
}
