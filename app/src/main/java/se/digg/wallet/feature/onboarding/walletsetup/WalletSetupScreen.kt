// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun WalletSetupRoute(
    pageNumber: Int,
    pin: String,
    onNext: () -> Unit,
    viewModel: WalletSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WalletSetupUiEffect.OnNext -> onNext.invoke()
            }
        }
    }
    LaunchedEffect(pin) {
        viewModel.start(pin)
    }
    WalletSetupScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onCancel = {},
    )
}

@Composable
private fun WalletSetupScreen(
    uiState: WalletSetupUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    when (uiState) {
        is WalletSetupUiState.InProgress -> StepInProgressScreen(uiState.step)

        is WalletSetupUiState.Failed -> GenericErrorScreen(
            errorTitle = stringResource(
                R.string.error_screen_failed_description,
                uiState.step.displayName(),
            ),
            onPrimaryAction = onRetry,
            secondaryActionLabel = stringResource(R.string.generic_cancel),
            onSecondaryAction = onCancel,
        )
    }
}

@Composable
private fun StepInProgressScreen(step: SetupStep, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Text(step.displayName())
    }
}

@Composable
private fun StepFailedScreen(
    step: SetupStep,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${step.displayName()} failed")
        PrimaryButton(text = stringResource(R.string.generic_retry), onClick = onRetry)
        PrimaryButton(text = stringResource(R.string.generic_cancel), onClick = onCancel)
    }
}

private fun SetupStep.displayName(): String = when (this) {
    SetupStep.CREATE_ACCOUNT -> "Create account"
    SetupStep.INIT_HSM -> "Init HSM"
    SetupStep.REGISTER_PIN -> "Register PIN"
    SetupStep.AUTHENTICATE -> "Authenticate"
    SetupStep.POST_HSM_KEY -> "Post HSM key"
}

@Composable
@PreviewsWallet
private fun WalletSetupInProgressPreview() {
    WalletPreview {
        WalletSetupScreen(
            uiState = WalletSetupUiState.InProgress(SetupStep.REGISTER_PIN),
            onRetry = {},
            onCancel = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun WalletSetupFailedPreview() {
    WalletPreview {
        WalletSetupScreen(
            uiState = WalletSetupUiState.Failed(SetupStep.AUTHENTICATE),
            onRetry = {},
            onCancel = {},
        )
    }
}
