// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import se.digg.wallet.core.designsystem.component.BottomActionBar
import se.digg.wallet.core.designsystem.component.BottomBarAction
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.LinkButton
import se.digg.wallet.core.designsystem.component.PinInput
import se.digg.wallet.core.designsystem.component.WalletBackButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.theme.ApprovalTheme
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.presentation.R

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
    AnimatedContent(
        targetState = uiState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(durationMillis = 220)) togetherWith fadeOut(snap(delayMillis = 220))
        },
        contentKey = { it::class },
        label = "presentationStep",
    ) { state ->
        PresentationTheme(uiState = state) {
            PresentationScreen(
                uiState = state,
                onBackClick = onBack,
                onShareClick = viewModel::onAccept,
                onFinishClick = onFinish,
                onSubmitPin = viewModel::sendData,
                onOptionalClaimClick = viewModel::onOptionalClaimCheckedChanged,
                onRecipientClick = {},
                onSharedSummaryClick = {},
            )
        }
    }
}

@Composable
private fun PresentationTheme(uiState: PresentationUiState, content: @Composable () -> Unit) {
    val isDesigned = when (uiState) {
        PresentationUiState.Loading,
        is PresentationUiState.PresentClaims,
        is PresentationUiState.ShareSuccess,
        -> true

        is PresentationUiState.EnterPin,
        PresentationUiState.Sharing,
        is PresentationUiState.Error,
        -> false
    }
    if (isDesigned) ApprovalTheme(content = content) else content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresentationScreen(
    uiState: PresentationUiState,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSubmitPin: (String) -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    onRecipientClick: () -> Unit,
    onSharedSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PresentationTopBar(uiState = uiState, onBackClick = onBackClick)
        },
        bottomBar = {
            PresentationBottomBar(
                uiState = uiState,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                onFinishClick = onFinishClick,
                onSharedSummaryClick = onSharedSummaryClick,
            )
        },
    ) { innerPadding ->
        PresentationContent(
            uiState = uiState,
            onSubmitPin = onSubmitPin,
            onOptionalClaimClick = onOptionalClaimClick,
            onRecipientClick = onRecipientClick,
            onSharedSummaryClick = onSharedSummaryClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PresentationTopBar(uiState: PresentationUiState, onBackClick: () -> Unit) {
    when (uiState) {
        PresentationUiState.Loading,
        is PresentationUiState.PresentClaims,
            -> {
            RequestTopAppBar(onCloseClick = onBackClick)
        }

        is PresentationUiState.EnterPin,
        PresentationUiState.Sharing,
        is PresentationUiState.Error,
            -> {
            WalletTopAppBar(navigationIcon = { WalletBackButton(onClick = onBackClick) })
        }

        is PresentationUiState.ShareSuccess -> {}
    }
}

@Composable
private fun PresentationBottomBar(
    uiState: PresentationUiState,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSharedSummaryClick: () -> Unit,
) {
    when (uiState) {
        PresentationUiState.Loading -> {
            BottomActionBar(
                secondaryAction = BottomBarAction(
                    text = stringResource(R.string.generic_cancel),
                    onClick = onBackClick,
                ),
            )
        }

        is PresentationUiState.PresentClaims -> {
            RequestBottomBar(
                sharedClaimCount = uiState.sharedCredentials.sumOf { it.claims.size },
                onSharedSummaryClick = onSharedSummaryClick,
                onCancelClick = onBackClick,
                onShareClick = onShareClick,
            )
        }

        is PresentationUiState.ShareSuccess -> {
            BottomActionBar(
                primaryAction = BottomBarAction(
                    text = stringResource(R.string.presentation_done_button),
                    onClick = onFinishClick,
                ),
            )
        }

        is PresentationUiState.EnterPin,
        PresentationUiState.Sharing,
        is PresentationUiState.Error,
            -> {
        }
    }
}

@Composable
private fun PresentationContent(
    uiState: PresentationUiState,
    onSubmitPin: (String) -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    onRecipientClick: () -> Unit,
    onSharedSummaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        when (uiState) {
            is PresentationUiState.Error -> {
                GenericErrorScreen(errorMessage = uiState.message)
            }

            PresentationUiState.Loading -> {
                CheckingRequestContent()
            }

            PresentationUiState.Sharing -> {
                GenericLoading()
            }

            is PresentationUiState.EnterPin -> {
                EnterPinContent(onSubmitPin = onSubmitPin)
            }

            is PresentationUiState.PresentClaims -> {
                RequestContent(
                    uiState = uiState,
                    onRecipientClick = onRecipientClick,
                    onOptionalClaimClick = onOptionalClaimClick,
                )
            }

            is PresentationUiState.ShareSuccess -> {
                ShareSuccessContent(
                    verifierName = uiState.verifierName,
                    onShowSharedClick = onSharedSummaryClick,
                )
            }
        }
    }
}

@Composable
private fun RequestTopAppBar(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    WalletTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.presentation_app_bar_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(R.drawable.close_x),
                    contentDescription = stringResource(R.string.generic_close),
                )
            }
        },
    )
}

@Composable
private fun RequestBottomBar(
    sharedClaimCount: Int,
    onSharedSummaryClick: () -> Unit,
    onCancelClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomActionBar(
        modifier = modifier,
        primaryAction = BottomBarAction(
            text = stringResource(R.string.generic_share),
            onClick = onShareClick,
        ),
        secondaryAction = BottomBarAction(
            text = stringResource(R.string.generic_cancel),
            onClick = onCancelClick,
        ),
        header = {
            LinkButton(
                text = pluralStringResource(
                    R.plurals.presentation_shared_count,
                    sharedClaimCount,
                    sharedClaimCount,
                ),
                onClick = onSharedSummaryClick,
                trailingIconRes = R.drawable.chevron_up,
                textStyle = MaterialTheme.typography.titleSmall,
            )
        },
    )
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
@ReadOnlyComposable
internal fun Map<String, String>.localized(): String? {
    val language = LocalConfiguration.current.locales[0].language
    return entries.firstOrNull { Locale.forLanguageTag(it.key).language == language }?.value
        ?: values.firstOrNull()
}

@Composable
private fun PresentationScreenPreview(uiState: PresentationUiState) {
    WalletPreview {
        PresentationTheme(uiState = uiState) {
            PresentationScreen(
                uiState = uiState,
                onBackClick = { },
                onShareClick = { },
                onFinishClick = { },
                onOptionalClaimClick = { _, _ -> },
                onRecipientClick = { },
                onSharedSummaryClick = { },
                onSubmitPin = {},
            )
        }
    }
}

@PreviewsWallet
@Composable
private fun PresentationPreview() {
    PresentationScreenPreview(uiState = PresentationUiState.EnterPin)
}

@PreviewsWallet
@Composable
private fun PresentationCheckingPreview() {
    PresentationScreenPreview(uiState = PresentationUiState.Loading)
}

@PreviewsWallet
@Composable
private fun PresentationRequestPreview() {
    PresentationScreenPreview(uiState = previewRequest)
}

@PreviewsWallet
@Composable
private fun PresentationDonePreview() {
    PresentationScreenPreview(uiState = previewDone)
}

@Preview(
    name = "Font scale 2.0",
    fontScale = 2f,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Composable
private fun PresentationRequestLargeFontPreview() {
    PresentationScreenPreview(uiState = previewRequest)
}

@Preview(
    name = "Font scale 2.0",
    fontScale = 2f,
    device = "spec:parent=pixel_9,orientation=portrait",
    showBackground = true,
)
@Composable
private fun PresentationDoneLargeFontPreview() {
    PresentationScreenPreview(uiState = previewDone)
}
