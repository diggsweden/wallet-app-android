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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.ErrorDetail
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.error.AppError

@Composable
fun WalletSetupRoute(
    pageNumber: Int,
    pin: String,
    onNext: () -> Unit,
    onCancel: () -> Unit,
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
        onCancel = onCancel,
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
            errorTitle = stringResource(R.string.generic_error_screen_title),
            errorMessage = stringResource(R.string.generic_error_screen_message),
            errorDetails = uiState.error.problem?.toErrorDetails() ?: emptyList(),
            onPrimaryAction = onRetry,
            secondaryActionLabel = stringResource(R.string.generic_cancel),
            onSecondaryAction = onCancel,
        )
    }
}

private val timestampFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

@Composable
private fun AppError.Problem.toErrorDetails(): List<ErrorDetail> = buildList {
    add(
        ErrorDetail(
            title = stringResource(R.string.error_detail_timestamp),
            value = timestamp.format(timestampFormatter),
        ),
    )
    add(ErrorDetail(title = stringResource(R.string.error_detail_title), value = title))
    detail?.let { add(ErrorDetail(title = stringResource(R.string.error_detail_message), value = it)) }
    add(ErrorDetail(title = stringResource(R.string.error_detail_status), value = status.toString()))
    type?.let { add(ErrorDetail(title = stringResource(R.string.error_detail_type), value = it)) }
    instance?.let { add(ErrorDetail(title = stringResource(R.string.error_detail_instance), value = it)) }
    transactionId?.let {
        add(ErrorDetail(title = stringResource(R.string.error_detail_transaction_id), value = it))
    }
    invalidParameters.forEach { parameter ->
        val title = parameter.property ?: stringResource(R.string.error_detail_invalid_parameter)
        val value = listOfNotNull(parameter.reason, parameter.value).joinToString(separator = " · ")
        add(ErrorDetail(title = title, value = value.ifBlank { "-" }))
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
        Text(stringResource(step.displayName()))
    }
}

private fun SetupStep.displayName(): Int = when (this) {
    SetupStep.CREATE_ACCOUNT -> R.string.wallet_setup_step_create_account
    SetupStep.INIT_HSM -> R.string.wallet_setup_step_init_hsm
    SetupStep.REGISTER_PIN -> R.string.wallet_setup_step_register_pin
    SetupStep.AUTHENTICATE -> R.string.wallet_setup_step_authenticate
    SetupStep.POST_HSM_KEY -> R.string.wallet_setup_step_post_hsm_key
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
            uiState = WalletSetupUiState.Failed(
                SetupStep.AUTHENTICATE,
                WalletSetupErrorUiModel(title = null, message = null, problem = null),
            ),
            onRetry = {},
            onCancel = {},
        )
    }
}
