// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.passkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun PasskeySetupRoute(
    pageNumber: Int,
    pin: String,
    onPasskeyCreated: () -> Unit,
    viewModel: PasskeySetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activityContext = LocalContext.current
    val currentOnPasskeyCreated by rememberUpdatedState(onPasskeyCreated)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is PasskeySetupUiEvent.PasskeyCreated) {
                currentOnPasskeyCreated()
            }
        }
    }

    PasskeySetupScreen(
        pageNumber = pageNumber,
        uiState = uiState,
        onCreateClick = { viewModel.createPasskey(activityContext, pin) },
    )
}

@Composable
private fun PasskeySetupScreen(
    pageNumber: Int,
    uiState: PasskeySetupUiState,
    onCreateClick: () -> Unit,
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
            pageTitle = stringResource(R.string.onboarding_passkey_title),
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.onboarding_passkey_description),
            style = WalletTextStyle.BodyLG,
        )
        if (uiState is PasskeySetupUiState.Error) {
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(
                    R.string.onboarding_passkey_error,
                    uiState.message.orEmpty(),
                ),
                style = WalletTextStyle.BodyLG,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.onboarding_passkey_create_button),
            onClick = onCreateClick,
            enabled = uiState !is PasskeySetupUiState.Creating,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun PasskeySetupScreenPreview() {
    WalletPreview {
        PasskeySetupScreen(
            pageNumber = 2,
            uiState = PasskeySetupUiState.Idle,
            onCreateClick = {},
        )
    }
}
