// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.OutLinedInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun EmailRoute(pageNumber: Int, onNext: () -> Unit, viewModel: EmailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmailUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailScreen(
        uiState = uiState,
        pageNumber = pageNumber,
        onNext = { viewModel.onEvent(EmailUiEvent.NextClicked) },
        onEmailChange = { viewModel.onEvent(EmailUiEvent.EmailChanged(it)) },
        onEmailFocusChange = { viewModel.onEvent(EmailUiEvent.EmailFocusedChanged(it)) },
        onVerifyEmailChange = { viewModel.onEvent(EmailUiEvent.VerifyEmailChanged(it)) },
        onVerifyEmailFocusChange = {
            viewModel.onEvent(EmailUiEvent.VerifyEmailFocusedChanged(it))
        },
    )
}

@Composable
private fun EmailScreen(
    uiState: EmailUiState,
    pageNumber: Int,
    onNext: () -> Unit,
    onEmailChange: (String) -> Unit,
    onEmailFocusChange: (Boolean) -> Unit,
    onVerifyEmailChange: (String) -> Unit,
    onVerifyEmailFocusChange: (Boolean) -> Unit,
) {
    val emailErrorCopy = when (uiState.emailError) {
        EmailValidationError.EMPTY -> stringResource(
            R.string.onboarding_email_error_validation_empty,
        )

        EmailValidationError.NOT_VALID_EMAIL -> stringResource(
            R.string.onboarding_email_error_validation_invalid_email,
        )

        EmailValidationError.NOT_SAME -> stringResource(
            R.string.onboarding_email_error_validation_not_same,
        )

        null -> stringResource(R.string.generic_error)
    }

    val verifyEmailErrorCopy = when (uiState.verifyEmailError) {
        EmailValidationError.EMPTY -> stringResource(
            R.string.onboarding_email_error_validation_empty,
        )

        EmailValidationError.NOT_VALID_EMAIL -> stringResource(
            R.string.onboarding_email_error_validation_invalid_email,
        )

        EmailValidationError.NOT_SAME -> stringResource(
            R.string.onboarding_email_error_validation_not_same,
        )

        null -> stringResource(R.string.generic_error)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingDefaults.HorizontalPadding)
            .padding(bottom = OnboardingDefaults.BottomPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingHeader(
            pageNumber = pageNumber,
            pageTitle = stringResource(
                R.string.onboarding_email_title,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = null,
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buildString {
                    append(stringResource(R.string.onboarding_email_description_1))
                    append("\n")
                    append(stringResource(R.string.onboarding_email_description_2))
                },
                style = WalletTextStyle.BodyMD,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(48.dp))
        OutLinedInput(
            value = uiState.email,
            labelText = stringResource(R.string.onboarding_email_input_label),
            onValueChange = { onEmailChange.invoke(it) },
            modifier = Modifier.onFocusChanged { state ->
                onEmailFocusChange.invoke(state.isFocused)
            },
            hintText = stringResource(R.string.onboarding_email_input_placeholder),
            errorText = emailErrorCopy,
            isError = uiState.emailError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutLinedInput(
            value = uiState.verifyEmail,
            labelText = stringResource(R.string.onboarding_email_input_label_2),
            onValueChange = { onVerifyEmailChange.invoke(it) },
            modifier = Modifier.onFocusChanged { state ->
                onVerifyEmailFocusChange.invoke(state.isFocused)
            },
            hintText = stringResource(R.string.onboarding_email_input_placeholder),
            errorText = verifyEmailErrorCopy,
            isError = uiState.verifyEmailError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = { onNext.invoke() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun EmailScreenPreview() {
    WalletPreview {
        EmailScreen(
            uiState = EmailUiState(),
            pageNumber = 4,
            onNext = {},
            onEmailChange = {},
            onEmailFocusChange = {},
            onVerifyEmailChange = {},
            onVerifyEmailFocusChange = {},
        )
    }
}
