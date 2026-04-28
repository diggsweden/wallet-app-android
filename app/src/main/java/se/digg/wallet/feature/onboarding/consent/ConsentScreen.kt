// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.InfoCheckBox
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.TextWithLink
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun ConsentRoute(
    pageNumber: Int,
    onNext: () -> Unit,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ConsentUiEffect.OnNext -> onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConsentScreen(
        uiState = uiState,
        pageNumber = pageNumber,
        onNext = { viewModel.onEvent(ConsentUiEvent.NextClicked) },
        onConsent = {
            viewModel.onEvent(
                ConsentUiEvent.ConsentChanged(it),
            )
        },
    )
}

@Composable
private fun ConsentScreen(
    uiState: ConsentUiState,
    pageNumber: Int,
    onNext: () -> Unit,
    onConsent: (Boolean) -> Unit,
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
                R.string.onboarding_consent_page_title,
            ),
        )
        InfoCheckBox(
            title = stringResource(R.string.onboarding_consent_checkbox_title),
            description = stringResource(R.string.onboarding_consent_checkbox_description),
            showError = uiState.showError,
            onCheckedChange = { onConsent.invoke(it) },
            errorText = stringResource(R.string.onboarding_consent_checkbox_error),
            checked = uiState.hasConsent,
        )
        Spacer(Modifier.height(40.dp))
        TextWithLink(text = stringResource(R.string.onboarding_consent_terms), onClick = {})
        Spacer(Modifier.height(16.dp))
        TextWithLink(text = stringResource(R.string.onboarding_consent_personal_data), onClick = {})
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
private fun ConsentScreenPreview() {
    WalletPreview {
        ConsentScreen(uiState = ConsentUiState(), onNext = {}, onConsent = {}, pageNumber = 1)
    }
}
