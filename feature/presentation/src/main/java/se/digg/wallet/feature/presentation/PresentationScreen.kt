// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.WalletBackButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.utils.PreviewsWalletScreen
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.presentation.component.pin.PresentationPinContent

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
    PresentationNavDisplay(
        uiState = uiState,
        onBack = onBack,
        onFinish = onFinish,
        onShareClick = viewModel::onAccept,
        onSubmitPin = viewModel::sendData,
        onBackToReview = viewModel::onBackToReview,
        onOptionalClaimClick = viewModel::onOptionalClaimCheckedChanged,
        modifier = modifier,
    )
}

@Composable
internal fun PresentationScreen(
    uiState: PresentationUiState,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onBackToReview: () -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    onRecipientClick: () -> Unit,
    onSharedClaimsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        PresentationUiState.Loading -> {
            PresentationLoadingScreen(onCloseClick = onBackClick, modifier = modifier)
        }

        is PresentationUiState.PresentClaims -> {
            PresentationReviewScreen(
                uiState = uiState,
                onCloseClick = onBackClick,
                onShareClick = onShareClick,
                onRecipientClick = onRecipientClick,
                onSharedClaimsClick = onSharedClaimsClick,
                onOptionalClaimClick = onOptionalClaimClick,
                modifier = modifier,
            )
        }

        is PresentationUiState.EnterPin -> {
            BackHandler(onBack = onBackToReview)
            PresentationStepScaffold(
                modifier = modifier,
                topBar = { BackTopAppBar(onBackClick = onBackToReview) },
            ) {
                PresentationPinContent(isWrongPin = uiState.isWrongPin, onSubmit = onSubmitPin)
            }
        }

        PresentationUiState.Sharing -> {
            PresentationStepScaffold(
                modifier = modifier,
                topBar = { BackTopAppBar(onBackClick = onBackClick) },
            ) {
                GenericLoading()
            }
        }

        is PresentationUiState.Error -> {
            PresentationStepScaffold(
                modifier = modifier,
                topBar = { BackTopAppBar(onBackClick = onBackClick) },
            ) {
                GenericErrorScreen(errorMessage = uiState.message)
            }
        }

        is PresentationUiState.ShareSuccess -> {
            PresentationSuccessScreen(
                verifierName = uiState.verifierName,
                onFinishClick = onFinishClick,
                onShowSharedClaimsClick = onSharedClaimsClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun PresentationStepScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            content = content,
        )
    }
}

@Composable
private fun BackTopAppBar(onBackClick: () -> Unit) {
    WalletTopAppBar(navigationIcon = { WalletBackButton(onClick = onBackClick) })
}

@Composable
private fun PresentationScreenPreview(uiState: PresentationUiState) {
    WalletPreview {
        PresentationScreen(
            uiState = uiState,
            onBackClick = { },
            onShareClick = { },
            onFinishClick = { },
            onOptionalClaimClick = { _, _ -> },
            onRecipientClick = { },
            onSharedClaimsClick = { },
            onSubmitPin = {},
            onBackToReview = {},
        )
    }
}

@PreviewsWalletScreen
@Composable
private fun PresentationPreview() {
    PresentationScreenPreview(uiState = PresentationUiState.EnterPin())
}

@PreviewsWalletScreen
@Composable
private fun PresentationCheckingPreview() {
    PresentationScreenPreview(uiState = PresentationUiState.Loading)
}

@PreviewsWalletScreen
@Composable
private fun PresentationReviewPreview() {
    PresentationScreenPreview(uiState = previewRequest)
}

@PreviewsWalletScreen
@Composable
private fun PresentationDonePreview() {
    PresentationScreenPreview(uiState = previewDone)
}
