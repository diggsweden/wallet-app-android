// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component.pin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.pin.PinInput
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.PreviewsWalletScreen
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.presentation.R

@Composable
internal fun PresentationPinContent(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isWrongPin: Boolean = false,
) {
    val isPreview = LocalInspectionMode.current
    val appeared = remember { MutableTransitionState(isPreview).apply { targetState = true } }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showIcon = maxHeight >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppearFromBelow(visibleState = appeared, delayMillis = 0) {
                PinHeader(
                    showIcon = showIcon,
                    modifier = Modifier.padding(top = if (showIcon) 24.dp else 8.dp),
                )
            }
            AppearFromBelow(visibleState = appeared, delayMillis = 90) {
                PinInput(
                    buttonLabel = stringResource(R.string.generic_share),
                    onSubmit = onSubmit,
                    modifier = Modifier.padding(top = 28.dp),
                    isError = isWrongPin,
                )
            }
        }
    }
}

@Composable
private fun AppearFromBelow(
    visibleState: MutableTransitionState<Boolean>,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 300, delayMillis = delayMillis)) +
            slideInVertically(tween(durationMillis = 300, delayMillis = delayMillis)) { it / 8 },
    ) {
        content()
    }
}

@Composable
private fun PinHeader(showIcon: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = colors.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.lock),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(
            text = stringResource(R.string.presentation_pin_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.presentation_pin_description),
            modifier = Modifier.widthIn(max = 340.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewsWalletScreen
@Composable
private fun PresentationPinContentPreview() {
    WalletPreview {
        PresentationPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp))
    }
}

@PreviewsWallet
@Composable
private fun PresentationPinContentWrongPinPreview() {
    WalletPreview {
        PresentationPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp), isWrongPin = true)
    }
}
