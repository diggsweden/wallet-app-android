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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CredentialOfferHeader
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PinInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.component.claims.ClaimList
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.oauth.LocalAuthTabLauncher
import se.digg.wallet.data.IssuerDisplay

@Composable
fun IssuanceScreen(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    credentialOfferUri: String,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    viewModel: IssuanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val issuer by viewModel.issuerDisplay.collectAsState()

    val launchAuthTab = LocalAuthTabLauncher.current
    LaunchedEffect(Unit) { viewModel.start(credentialOfferUri) }

    when (val currentState = uiState) {
        is IssuanceState.Failed -> {
            GenericErrorScreen(onPrimaryAction = { viewModel.retry() })
        }

        IssuanceState.Idle,
        is IssuanceState.AtStep,
        -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (headerContent != null) {
                    headerContent()
                } else {
                    Spacer(Modifier.height(26.dp))
                }

                when (val step = (currentState as? IssuanceState.AtStep)?.step) {
                    IssuanceStep.PreparingToAuthorize -> {
                        CredentialOfferHeader(
                            logoUrl = issuer?.logo?.uri?.toString(),
                            issuerName = issuer?.name,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        PrimaryButton(
                            text = stringResource(R.string.generic_login),
                            onClick = { viewModel.login(launchAuthTab) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

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
                            onSubmit = { pin -> viewModel.enterPin(pin) },
                        )
                    }

                    is IssuanceStep.SavingCredential -> {
                        IssuedCredentialContent(issuer = issuer, issued = step.issued)
                    }

                    is IssuanceStep.Issued -> {
                        IssuedCredentialContent(
                            issuer = issuer,
                            issued = step.issued,
                            onFinishClick = onFinishClick,
                        )
                    }

                    else -> {
                        GenericLoading()
                    }
                }
            }
        }
    }
}

@Composable
private fun IssuedCredentialContent(
    issuer: IssuerDisplay?,
    issued: IssuedCredential,
    onFinishClick: (() -> Unit)? = null,
) {
    CredentialOfferHeader(
        logoUrl = issuer?.logo?.uri?.toString(),
        issuerName = issuer?.name,
    )
    Spacer(modifier = Modifier.height(30.dp))
    ClaimList(claims = issued.claims)
    Spacer(modifier = Modifier.height(24.dp))
    if (onFinishClick != null) {
        PrimaryButton(
            text = stringResource(R.string.issuance_approve_button),
            onClick = onFinishClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun DeepLinkedIssuanceRoute(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    credentialOfferUri: String,
    modifier: Modifier = Modifier,
    viewModel: IssuanceViewModel = hiltViewModel(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.issuance_app_bar_title),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.dismiss()
                            onBackClick.invoke()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            IssuanceScreen(
                onBackClick = { onBackClick.invoke() },
                onFinishClick = { onFinishClick.invoke() },
                credentialOfferUri = credentialOfferUri,
                viewModel = viewModel,
            )
        }
    }
}
