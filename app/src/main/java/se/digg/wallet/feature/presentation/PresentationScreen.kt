// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.PinInput
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.WalletBackButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.component.claims.ClaimList
import se.digg.wallet.core.designsystem.component.claims.SelectiveDisclosureList
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.PresentationItem

@Composable
fun PresentationRoute(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onPopBack: () -> Unit,
    fullUri: String,
    modifier: Modifier = Modifier,
    viewModel: PresentationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val currentOnPopBack by rememberUpdatedState(onPopBack)

    LaunchedEffect(Unit) {
        viewModel.init(fullUri)

        viewModel.effects.collect { effect ->
            when (effect) {
                is PresentationUiEffect.OpenUrl -> {
                    uriHandler.openUri(effect.url)
                    currentOnPopBack()
                }
            }
        }
    }
    PresentationScreen(
        onBackClick = onBack,
        onShareClick = viewModel::onAccept,
        onFinishClick = onFinish,
        onSubmitPin = viewModel::sendData,
        onOptionalClaimClick = viewModel::onOptionalClaimCheckedChanged,
        uiState = uiState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresentationScreen(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    uiState: PresentationUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(navigationIcon = { WalletBackButton(onClick = onBackClick) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            when (uiState) {
                is PresentationUiState.Error -> {
                    GenericErrorScreen(errorMessage = uiState.message)
                }

                PresentationUiState.Loading -> {
                    GenericLoading()
                }

                is PresentationUiState.EnterPin -> {
                    EnterPinContent(onSubmitPin = onSubmitPin)
                }

                is PresentationUiState.PresentClaims -> {
                    PresentClaimsContent(
                        verifierName = uiState.verifierName,
                        requiredClaims = uiState.requiredClaims,
                        optionalClaims = uiState.optionalClaims,
                        onOptionalClaimClick = onOptionalClaimClick,
                        onShareClick = onShareClick,
                    )
                }

                is PresentationUiState.ShareSuccess -> {
                    ShareSuccess(onFinishClick = onFinishClick)
                }
            }
        }
    }
}

@Composable
private fun EnterPinContent(onSubmitPin: (String) -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.presentation_pin_title),
            style = WalletTextStyle.H1,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.presentation_pin_description),
            style = WalletTextStyle.BodyLG,
        )
        Spacer(Modifier.height(64.dp))
        PinInput(
            buttonLabel = stringResource(R.string.generic_share),
            onSubmit = onSubmitPin,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PresentClaimsContent(
    verifierName: String,
    requiredClaims: List<PresentationItem>,
    optionalClaims: List<PresentationItem>,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    onShareClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.presentation_share_title, verifierName),
                style = WalletTextStyle.H2,
            )
            Spacer(Modifier.height(8.dp))
            requiredClaims.forEach { item ->
                ClaimList(claims = item.claims, showShare = true)
            }
            if (optionalClaims.isNotEmpty()) {
                SelectiveDisclosureList(
                    onClaimClick = onOptionalClaimClick,
                    presentationItems = optionalClaims,
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.generic_share),
            onClick = onShareClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ShareSuccess(onFinishClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.presentation_share_successful_title),
            style = WalletTextStyle.H1,
        )
        Spacer(Modifier.height(50.dp))
        Image(
            painter = painterResource(id = R.drawable.presentation_successful),
            contentDescription = null,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.presentation_share_successful_info),
            style = WalletTextStyle.BodyMD,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_close),
            onClick = onFinishClick,
        )
    }
}

@PreviewsWallet
@Composable
private fun PresentationPreview() {
    WalletPreview {
        PresentationScreen(
            onBackClick = { },
            onShareClick = { },
            onFinishClick = { },
            onOptionalClaimClick = { _, _ -> },
            onSubmitPin = {},
            uiState = PresentationUiState.EnterPin,
        )
    }
}
