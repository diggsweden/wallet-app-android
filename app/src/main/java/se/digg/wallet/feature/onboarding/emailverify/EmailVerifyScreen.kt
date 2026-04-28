// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.emailverify

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.ConfirmCode
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun EmailVerifyRoute(
    pageNumber: Int,
    onNext: () -> Unit,
    viewModel: EmailVerifyViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.setEmail()
        viewModel.effects.collect { effect ->
            when (effect) {
                EmailVerifyUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EmailVerifyScreen(
        uiState = uiState,
        pageNumber = pageNumber,
        onNext = { viewModel.onEvent(EmailVerifyUiEvent.NextClicked) },
        onCodeChange = { viewModel.onEvent(EmailVerifyUiEvent.CodeChanged(it)) },
    )
}

@Composable
private fun EmailVerifyScreen(
    uiState: EmailVerifyUiState,
    pageNumber: Int,
    onNext: () -> Unit,
    onCodeChange: (String) -> Unit,
) {
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
                R.string.onboarding_email_verify_title,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                text =
                    stringResource(R.string.onboarding_email_verify_description_1, uiState.email),
                style = WalletTextStyle.BodyMD,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = null,
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildString {
                append(stringResource(R.string.onboarding_email_verify_description_2))
                append("\n\n")
                append(stringResource(R.string.onboarding_email_verify_description_3))
            },
            style = WalletTextStyle.BodySM,
        )
        Spacer(Modifier.height(64.dp))

        ConfirmCode(
            value = uiState.code,
            onValueChange = { onCodeChange.invoke(it) },
            onDone = { /* submit code */ },
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = {
                onNext.invoke()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun PhoneScreenPreview() {
    WalletPreview {
        EmailVerifyScreen(
            uiState = EmailVerifyUiState(email = "test@digg.se"),
            pageNumber = 5,
            onNext = {},
            onCodeChange = {},
        )
    }
}
