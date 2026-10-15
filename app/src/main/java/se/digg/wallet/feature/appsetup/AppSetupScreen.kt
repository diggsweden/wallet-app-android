// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.InfoCard
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.Black100
import se.digg.wallet.core.designsystem.theme.Brown30
import se.digg.wallet.core.designsystem.theme.Brown70
import se.digg.wallet.core.designsystem.theme.DarkPrimary
import se.digg.wallet.core.designsystem.theme.Green10
import se.digg.wallet.core.designsystem.theme.Green100
import se.digg.wallet.core.designsystem.theme.Green50
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.theme.White
import se.digg.wallet.core.designsystem.theme.isWalletInDarkTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun AppSetupRoute(
    onNavigateToIntro: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToIntro by rememberUpdatedState(onNavigateToIntro)
    val currentOnNavigateToDashboard by rememberUpdatedState(onNavigateToDashboard)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AppSetupUiEffect.NavigateToIntro -> currentOnNavigateToIntro()
                AppSetupUiEffect.NavigateToDashboard -> currentOnNavigateToDashboard()
            }
        }
    }

    AppSetupScreen(
        uiState = uiState,
        onAcknowledgeReset = { viewModel.onResetAcknowledged() },
        onRetry = { viewModel.onRetry() },
        modifier = modifier,
    )
}

@Composable
fun AppSetupScreen(
    uiState: AppSetupUiState,
    onAcknowledgeReset: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AppSetupUiState.Loading -> AppSetupLoading(modifier = modifier)

        is AppSetupUiState.Error -> AppSetupError(onRetry = onRetry, modifier = modifier)

        AppSetupUiState.Reset -> AppSetupReset(
            onAcknowledge = onAcknowledgeReset,
            modifier = modifier,
        )
    }
}

@Composable
private fun AppSetupLoading(modifier: Modifier = Modifier) {
    GenericLoading(modifier = modifier)
}

@Composable
private fun AppSetupError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    GenericErrorScreen(modifier = modifier, onPrimaryAction = onRetry)
}

@Composable
private fun AppSetupReset(onAcknowledge: () -> Unit, modifier: Modifier = Modifier) {
    val isDarkMode = isWalletInDarkTheme()
    val descriptionColor = if (isDarkMode) Brown30 else Brown70
    val containerColor = if (isDarkMode) DarkPrimary else Green10
    val accentColor = if (isDarkMode) Green50 else Green100

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ResetContent(
            containerColor = containerColor,
            accentColor = accentColor,
            descriptionColor = descriptionColor,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = stringResource(R.string.app_setup_reset_button),
            onClick = onAcknowledge,
        )
    }
}

@Composable
private fun ResetContent(
    containerColor: Color,
    accentColor: Color,
    descriptionColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        ResetIllustration(containerColor = containerColor, accentColor = accentColor)
        Spacer(modifier = Modifier.height(24.dp))
        ResetHeader(descriptionColor = descriptionColor)
        Spacer(modifier = Modifier.height(32.dp))
        InfoCard(
            iconRes = R.drawable.smartphone_24px,
            title = stringResource(R.string.app_setup_reset_removed_title),
            description = stringResource(R.string.app_setup_reset_removed_description),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.app_setup_reset_register_again),
            style = WalletTextStyle.BodyMD,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResetHeader(descriptionColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_setup_reset_title),
            style = WalletTextStyle.H1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_setup_reset_description),
            style = WalletTextStyle.BodyMD,
            textAlign = TextAlign.Center,
            color = descriptionColor,
        )
    }
}

@Composable
private fun ResetIllustration(
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(120.dp)) {
        WalletIcon(
            containerColor = containerColor,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize(),
        )
        RefreshBadge(
            accentColor = accentColor,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun WalletIcon(containerColor: Color, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.account_balance_wallet_24px),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun RefreshBadge(accentColor: Color, modifier: Modifier = Modifier) {
    val iconColor = if (isWalletInDarkTheme()) Black100 else White
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = 800,
                delayMillis = 300,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .border(
                width = 4.dp,
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape,
            )
            .padding(4.dp)
            .background(color = accentColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.refresh_24px),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotation.value },
        )
    }
}

@Composable
@PreviewsWallet
private fun AppSetupResetPreview() {
    WalletPreview {
        AppSetupScreen(uiState = AppSetupUiState.Reset, onAcknowledgeReset = {}, onRetry = {})
    }
}
