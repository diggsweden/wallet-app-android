// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.issuance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.BaseScreen
import se.digg.wallet.core.designsystem.component.CredentialOfferHeader
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.PinInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.claims.ClaimList
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.IssuerDisplay

@Composable
fun IssuanceRoute(
    credentialOfferUri: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissibleChange: (Boolean) -> Unit = {},
    headerContent: (@Composable () -> Unit)? = null,
    viewModel: IssuanceViewModel = hiltViewModel<IssuanceViewModel, IssuanceViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(credentialOfferUri)
        },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val issuer by viewModel.issuerDisplay.collectAsStateWithLifecycle()
    val currentOnDismissibleChange by rememberUpdatedState(onDismissibleChange)
    val dismissible = uiState.isDismissible

    LaunchedEffect(dismissible) {
        currentOnDismissibleChange(dismissible)
    }

    IssuanceScreen(
        uiState = uiState,
        issuer = issuer,
        onLogin = viewModel::login,
        onPinEnter = viewModel::enterPin,
        onRetry = viewModel::retry,
        onComplete = onComplete,
        modifier = modifier,
        headerContent = headerContent,
    )
}

@Composable
private fun IssuanceScreen(
    uiState: IssuanceState,
    issuer: IssuerDisplay?,
    onLogin: () -> Unit,
    onPinEnter: (String) -> Unit,
    onRetry: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
) {
    when (uiState) {
        is IssuanceState.Failed -> {
            GenericErrorScreen(onPrimaryAction = onRetry)
        }

        is IssuanceState.AtStep -> {
            BaseScreen(
                floatingContent = {
                    StepButton(
                        step = uiState.step,
                        onLogin = onLogin,
                        onComplete = onComplete,
                    )
                },
            ) {
                IssuanceSteps(
                    headerContent = headerContent,
                    step = uiState.step,
                    issuer = issuer,
                    onPinEnter = onPinEnter,
                )
            }
        }
    }
}

@Composable
private fun IssuedCredentialContent(issued: IssuedCredential) {
    Spacer(modifier = Modifier.height(30.dp))
    ClaimList(claims = issued.claims)
}

@Composable
private fun IssuanceSteps(
    headerContent: (@Composable () -> Unit)?,
    step: IssuanceStep,
    issuer: IssuerDisplay?,
    onPinEnter: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        if (headerContent != null) {
            headerContent()
        } else {
            Spacer(Modifier.height(26.dp))
        }

        if (step != IssuanceStep.AwaitingPin) {
            issuer?.let {
                CredentialOfferHeader(
                    logoUrl = it.logo?.uri?.toString(),
                    issuerName = it.name,
                )
            }
        }

        when (step) {
            IssuanceStep.PreparingToAuthorize -> {}

            IssuanceStep.AwaitingPin -> {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text =
                        stringResource(
                            R.string.onboarding_issuance_ready_to_sign_description,
                        ),
                    style = WalletTextStyle.BodyLG,
                )
                Spacer(modifier = Modifier.weight(1f))
                PinInput(
                    buttonLabel = stringResource(
                        R.string.onboarding_issuance_ready_to_sign_confirm_button,
                    ),
                    onSubmit = onPinEnter,
                )
            }

            is IssuanceStep.SavingCredential -> {
                IssuedCredentialContent(
                    issued = step.issued,
                )
            }

            is IssuanceStep.Issued -> {
                IssuedCredentialContent(
                    issued = step.issued,
                )
            }

            else -> {
                GenericLoading()
            }
        }
    }
}

@Composable
private fun StepButton(step: IssuanceStep, onLogin: () -> Unit, onComplete: (() -> Unit)?) {
    when (step) {
        is IssuanceStep.PreparingToAuthorize -> {
            PrimaryButton(
                text = stringResource(R.string.generic_login),
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is IssuanceStep.Issued -> {
            if (onComplete != null) {
                PrimaryButton(
                    text = stringResource(R.string.generic_continue),
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        else -> {}
    }
}

@Composable
@PreviewsWallet
private fun IssuancePreparingToAuthorizePreview() {
    WalletPreview {
        IssuanceScreen(
            uiState = IssuanceState.AtStep(IssuanceStep.PreparingToAuthorize),
            issuer = null,
            onLogin = {},
            onPinEnter = {},
            onRetry = {},
            onComplete = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun IssuanceAwaitingPinPreview() {
    WalletPreview {
        IssuanceScreen(
            uiState = IssuanceState.AtStep(IssuanceStep.AwaitingPin),
            issuer = null,
            onLogin = {},
            onPinEnter = {},
            onRetry = {},
            onComplete = {},
        )
    }
}
